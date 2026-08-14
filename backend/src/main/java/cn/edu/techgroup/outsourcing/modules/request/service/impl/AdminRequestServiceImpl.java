package cn.edu.techgroup.outsourcing.modules.request.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEvents;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.dto.AdminRequestActionCommand;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.request.service.AdminRequestService;
import cn.edu.techgroup.outsourcing.modules.request.vo.AdminRequestActionVO;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRequestServiceImpl implements AdminRequestService {

    private static final Set<RequestStatus> CANCELABLE = Set.of(
            RequestStatus.PENDING_REVIEW,
            RequestStatus.NEED_MORE_INFO,
            RequestStatus.PENDING_ASSIGNMENT,
            RequestStatus.IN_PROGRESS,
            RequestStatus.PENDING_ACCEPTANCE);
    private static final Set<RequestStatus> RESTORABLE = CANCELABLE;

    private final RequestMapper requestMapper;
    private final RequestMemberMapper requestMemberMapper;
    private final StatusHistoryMapper statusHistoryMapper;
    private final UserMapper userMapper;
    private final AuditRecorder auditRecorder;
    private final NotificationEventPublisher notificationEventPublisher;

    public AdminRequestServiceImpl(
            RequestMapper requestMapper,
            RequestMemberMapper requestMemberMapper,
            StatusHistoryMapper statusHistoryMapper,
            UserMapper userMapper,
            AuditRecorder auditRecorder,
            NotificationEventPublisher notificationEventPublisher) {
        this.requestMapper = requestMapper;
        this.requestMemberMapper = requestMemberMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.userMapper = userMapper;
        this.auditRecorder = auditRecorder;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRequestActionVO cancel(
            Long requestId,
            AdminRequestActionCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validateCommand(command);
        RequestEntity request = findForUpdate(requestId);
        requireExpectedVersion(request, command.expectedVersion());
        if (!CANCELABLE.contains(request.getStatus())) {
            throw statusConflict();
        }
        return transition(
                request,
                RequestStatus.CANCELLED,
                command.reason().trim(),
                operator,
                false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRequestActionVO reopen(
            Long requestId,
            AdminRequestActionCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validateCommand(command);
        RequestEntity request = findForUpdate(requestId);
        requireExpectedVersion(request, command.expectedVersion());
        if (request.getStatus() != RequestStatus.REJECTED
                && request.getStatus() != RequestStatus.CANCELLED
                && request.getStatus() != RequestStatus.COMPLETED) {
            throw statusConflict();
        }

        List<RequestMemberEntity> currentMembers =
                requestMemberMapper.selectByRequestId(requestId);
        boolean hasActiveOwner = hasActiveTechnicalOwner(currentMembers);
        RequestStatus targetStatus = reopenTarget(
                request,
                hasActiveOwner);
        return transition(
                request,
                targetStatus,
                command.reason().trim(),
                operator,
                true);
    }

    private RequestStatus reopenTarget(
            RequestEntity request,
            boolean hasActiveOwner) {
        if (request.getStatus() == RequestStatus.REJECTED) {
            return RequestStatus.PENDING_REVIEW;
        }
        if (request.getStatus() == RequestStatus.COMPLETED) {
            return hasActiveOwner
                    ? RequestStatus.IN_PROGRESS
                    : RequestStatus.PENDING_ASSIGNMENT;
        }

        StatusHistoryEntity latestCancellation =
                statusHistoryMapper.selectLatestCancellation(request.getId());
        if (latestCancellation != null
                && RESTORABLE.contains(latestCancellation.getFromStatus())) {
            RequestStatus previousStatus = latestCancellation.getFromStatus();
            if (previousStatus != RequestStatus.IN_PROGRESS || hasActiveOwner) {
                return previousStatus;
            }
            return RequestStatus.PENDING_ASSIGNMENT;
        }
        return hasActiveOwner
                ? RequestStatus.IN_PROGRESS
                : RequestStatus.PENDING_REVIEW;
    }

    private boolean hasActiveTechnicalOwner(
            List<RequestMemberEntity> currentMembers) {
        Long ownerId = currentMembers.stream()
                .filter(member -> member.getMemberType() == RequestMemberType.OWNER)
                .map(RequestMemberEntity::getUserId)
                .findFirst()
                .orElse(null);
        if (ownerId == null) {
            return false;
        }
        List<UserEntity> owners =
                userMapper.selectAssignmentUsersByIdsForUpdate(List.of(ownerId));
        if (owners.size() != 1) {
            return false;
        }
        UserEntity owner = owners.getFirst();
        return owner.getStatus() == UserStatus.ACTIVE
                && (owner.getRole() == UserRole.MEMBER
                        || owner.getRole() == UserRole.ADMIN);
    }

    private AdminRequestActionVO transition(
            RequestEntity request,
            RequestStatus targetStatus,
            String reason,
            LoginUser operator,
            boolean reopened) {
        RequestStatus oldStatus = request.getStatus();
        int updatedRows = requestMapper.compareAndSetStatus(
                request.getId(),
                oldStatus.getValue(),
                targetStatus.getValue(),
                request.getVersion());
        if (updatedRows != 1) {
            throw statusConflict();
        }

        StatusHistoryEntity history = new StatusHistoryEntity();
        history.setRequestId(request.getId());
        history.setOperatorId(operator.id());
        history.setFromStatus(oldStatus);
        history.setToStatus(targetStatus);
        history.setReason(
                (reopened ? "管理员重新开启：" : "管理员取消：")
                        + reason);
        history.setCreatedAt(Instant.now());
        if (statusHistoryMapper.insert(history) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        int newVersion = request.getVersion() + 1;
        auditRecorder.record(
                operator.id(),
                reopened ? "REQUEST_REOPEN" : "REQUEST_CANCEL",
                "REQUEST",
                request.getId().toString(),
                Map.of(
                        "status", oldStatus,
                        "version", request.getVersion()),
                Map.of(
                        "status", targetStatus,
                        "version", newVersion,
                        "reason", reason));

        List<Long> recipientIds = new ArrayList<>();
        recipientIds.add(request.getCreatorId());
        recipientIds.addAll(requestMemberMapper.selectByRequestId(request.getId())
                .stream()
                .map(RequestMemberEntity::getUserId)
                .toList());
        notificationEventPublisher.publish(
                NotificationEvents.adminRequestUpdated(
                        request.getId(),
                        request.getRequestNo(),
                        operator.id(),
                        recipientIds,
                        reopened));
        return new AdminRequestActionVO(
                request.getId().toString(),
                targetStatus,
                newVersion);
    }

    private RequestEntity findForUpdate(Long requestId) {
        if (requestId == null || requestId <= 0) {
            throw hiddenRequest();
        }
        RequestEntity request = requestMapper.selectByIdForUpdate(requestId);
        if (request == null) {
            throw hiddenRequest();
        }
        return request;
    }

    private void requireExpectedVersion(
            RequestEntity request,
            Integer expectedVersion) {
        if (!Objects.equals(request.getVersion(), expectedVersion)) {
            throw statusConflict();
        }
    }

    private void validateCommand(AdminRequestActionCommand command) {
        if (command == null
                || command.expectedVersion() == null
                || command.expectedVersion() < 0
                || command.reason() == null) {
            throw invalidArgument();
        }
        int reasonLength = command.reason().trim().length();
        if (reasonLength < 5 || reasonLength > 500) {
            throw invalidArgument();
        }
    }

    private void requireAdmin(LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (operator.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private BusinessException hiddenRequest() {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "需求不存在");
    }

    private BusinessException statusConflict() {
        return new BusinessException(
                ErrorCode.REQUEST_STATUS_CONFLICT,
                "需求状态或版本已变化，请刷新后重试");
    }

    private BusinessException invalidArgument() {
        return new BusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "操作参数不正确");
    }
}
