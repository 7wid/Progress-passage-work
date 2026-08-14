package cn.edu.techgroup.outsourcing.modules.assignment.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.assignment.dto.UpdateRequestMembersCommand;
import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.assignment.service.AssignmentService;
import cn.edu.techgroup.outsourcing.modules.assignment.vo.MemberOptionVO;
import cn.edu.techgroup.outsourcing.modules.assignment.vo.RequestAssignmentVO;
import cn.edu.techgroup.outsourcing.modules.assignment.vo.RequestMemberVO;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEvents;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private static final int MAX_PARTICIPANTS = 20;
    private static final int MAX_KEYWORD_LENGTH = 64;
    private static final int MIN_REASON_LENGTH = 5;
    private static final int MAX_REASON_LENGTH = 500;

    private final RequestMapper requestMapper;
    private final RequestMemberMapper requestMemberMapper;
    private final UserMapper userMapper;
    private final StatusHistoryMapper statusHistoryMapper;
    private final NotificationEventPublisher notificationEventPublisher;
    private final AuditRecorder auditRecorder;

    public AssignmentServiceImpl(
            RequestMapper requestMapper,
            RequestMemberMapper requestMemberMapper,
            UserMapper userMapper,
            StatusHistoryMapper statusHistoryMapper,
            NotificationEventPublisher notificationEventPublisher,
            AuditRecorder auditRecorder) {
        this.requestMapper = requestMapper;
        this.requestMemberMapper = requestMemberMapper;
        this.userMapper = userMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.notificationEventPublisher = notificationEventPublisher;
        this.auditRecorder = auditRecorder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberOptionVO> listMemberOptions(
            String keyword,
            LoginUser operator) {
        requireAdmin(operator);

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw invalidArgument("搜索关键词不能超过 64 个字符");
        }

        return userMapper.selectAssignableUsers(normalizedKeyword)
                .stream()
                .map(user -> new MemberOptionVO(
                        user.getId().toString(),
                        user.getAccount(),
                        user.getDisplayName(),
                        user.getRole()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RequestAssignmentVO get(
            Long requestId,
            LoginUser viewer) {
        RequestEntity request = findVisibleRequest(requestId, viewer);
        List<RequestMemberEntity> members =
                requestMemberMapper.selectByRequestId(requestId);
        return buildAssignmentVO(request, members, loadUsers(members));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RequestAssignmentVO update(
            Long requestId,
            UpdateRequestMembersCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validateCommand(command);

        RequestEntity request = findRequestForUpdate(requestId);
        RequestStatus oldStatus = request.getStatus();
        if (oldStatus != RequestStatus.PENDING_ASSIGNMENT
                && oldStatus != RequestStatus.IN_PROGRESS) {
            throw new BusinessException(
                    ErrorCode.REQUEST_STATUS_CONFLICT,
                    "当前需求状态不允许调整任务成员");
        }
        if (!Objects.equals(request.getVersion(), command.requestVersion())) {
            throw statusConflict();
        }

        Map<Long, RequestMemberType> desiredTypes = desiredTypes(command);
        Map<Long, UserEntity> targetUsers = loadTargetUsers(desiredTypes.keySet());
        List<RequestMemberEntity> existingMembers =
                requestMemberMapper.selectByRequestId(requestId);

        if (oldStatus == RequestStatus.IN_PROGRESS
                && sameMemberConfiguration(desiredTypes, existingMembers)) {
            return buildAssignmentVO(request, existingMembers, targetUsers);
        }

        String oldOwnerName = resolveOldOwnerName(existingMembers);
        String newOwnerName = targetUsers.get(command.ownerId()).getDisplayName();

        int updatedRows = requestMapper.compareAndSetStatus(
                requestId,
                oldStatus.getValue(),
                RequestStatus.IN_PROGRESS.getValue(),
                command.requestVersion());
        if (updatedRows != 1) {
            throw statusConflict();
        }

        try {
            reconcileMembers(requestId, desiredTypes, existingMembers);
        } catch (DuplicateKeyException exception) {
            throw statusConflict();
        }

        insertStatusHistory(
                requestId,
                operator.id(),
                oldStatus,
                oldOwnerName,
                newOwnerName,
                command);

        request.setStatus(RequestStatus.IN_PROGRESS);
        request.setVersion(command.requestVersion() + 1);

        List<RequestMemberEntity> finalMembers =
                requestMemberMapper.selectByRequestId(requestId);
        auditRecorder.record(
                operator.id(),
                "ASSIGNMENT_UPDATE",
                "REQUEST",
                requestId.toString(),
                Map.of("members", existingMemberSnapshot(existingMembers)),
                Map.of(
                        "members", desiredMemberSnapshot(desiredTypes),
                        "reason", command.reason().trim()));
        List<Long> recipients = new ArrayList<>(desiredTypes.keySet());
        recipients.add(request.getCreatorId());
        notificationEventPublisher.publish(
                NotificationEvents.assignmentUpdated(
                        request.getId(),
                        request.getRequestNo(),
                        operator.id(),
                        recipients));
        return buildAssignmentVO(request, finalMembers, targetUsers);
    }

    private void validateCommand(UpdateRequestMembersCommand command) {
        if (command == null) {
            throw invalidArgument("请求参数不能为空");
        }
        if (command.requestVersion() == null || command.requestVersion() < 0) {
            throw invalidArgument("请求版本不正确");
        }
        if (command.ownerId() == null || command.ownerId() <= 0) {
            throw invalidArgument("负责人不能为空");
        }
        if (command.participantIds() == null) {
            throw invalidArgument("参与成员列表不能为空");
        }
        if (command.participantIds().size() > MAX_PARTICIPANTS) {
            throw invalidArgument("参与成员数量不能超过 20 人");
        }
        if (command.participantIds().stream()
                .anyMatch(id -> id == null || id <= 0)) {
            throw invalidArgument("参与成员编号不正确");
        }

        Set<Long> participantIds = new HashSet<>(command.participantIds());
        if (participantIds.size() != command.participantIds().size()) {
            throw invalidArgument("参与成员不能重复");
        }
        if (participantIds.contains(command.ownerId())) {
            throw invalidArgument("负责人不能同时作为参与成员");
        }

        String reason = command.reason();
        if (!StringUtils.hasText(reason)
                || reason.trim().length() < MIN_REASON_LENGTH
                || reason.trim().length() > MAX_REASON_LENGTH) {
            throw invalidArgument("调整原因应为 5～500 个字符");
        }
    }

    private Map<Long, RequestMemberType> desiredTypes(
            UpdateRequestMembersCommand command) {
        Map<Long, RequestMemberType> desiredTypes = new LinkedHashMap<>();
        desiredTypes.put(command.ownerId(), RequestMemberType.OWNER);
        command.participantIds().forEach(
                id -> desiredTypes.put(id, RequestMemberType.PARTICIPANT));
        return desiredTypes;
    }

    private Map<Long, UserEntity> loadTargetUsers(Set<Long> userIds) {
        List<UserEntity> users =
                userMapper.selectAssignmentUsersByIdsForUpdate(userIds);
        Map<Long, UserEntity> userMap = users.stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));

        for (Long userId : userIds) {
            UserEntity user = userMap.get(userId);
            if (user == null) {
                throw invalidArgument("指定的任务成员不存在");
            }
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw invalidArgument("只能选择已启用的任务成员");
            }
            if (user.getRole() != UserRole.MEMBER
                    && user.getRole() != UserRole.ADMIN) {
                throw invalidArgument("任务成员必须为技术组成员或管理员");
            }
        }
        return userMap;
    }

    private Map<Long, UserEntity> loadUsers(List<RequestMemberEntity> members) {
        Set<Long> userIds = members.stream()
                .map(RequestMemberEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userMapper.selectAssignmentUsersByIds(userIds)
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));
    }

    private boolean sameMemberConfiguration(
            Map<Long, RequestMemberType> desiredTypes,
            List<RequestMemberEntity> existingMembers) {
        if (desiredTypes.size() != existingMembers.size()) {
            return false;
        }
        return existingMembers.stream().allMatch(member ->
                desiredTypes.get(member.getUserId()) == member.getMemberType());
    }

    private void reconcileMembers(
            Long requestId,
            Map<Long, RequestMemberType> desiredTypes,
            List<RequestMemberEntity> existingMembers) {
        Set<Long> handledIds = new HashSet<>();

        for (RequestMemberEntity existing : existingMembers) {
            if (existing.getMemberType() != RequestMemberType.OWNER) {
                continue;
            }
            RequestMemberType desiredType = desiredTypes.get(existing.getUserId());
            if (desiredType == RequestMemberType.OWNER) {
                continue;
            }
            updateOrDeleteExisting(existing, desiredType);
            handledIds.add(existing.getId());
        }

        for (RequestMemberEntity existing : existingMembers) {
            if (handledIds.contains(existing.getId())) {
                continue;
            }
            RequestMemberType desiredType = desiredTypes.get(existing.getUserId());
            if (desiredType == null) {
                deleteMember(existing.getId());
            } else if (existing.getMemberType() != desiredType) {
                updateMemberType(existing.getId(), desiredType);
            }
        }

        Set<Long> existingUserIds = existingMembers.stream()
                .map(RequestMemberEntity::getUserId)
                .collect(Collectors.toSet());
        Instant joinedAt = Instant.now();
        desiredTypes.forEach((userId, memberType) -> {
            if (!existingUserIds.contains(userId)) {
                insertMember(requestId, userId, memberType, joinedAt);
            }
        });
    }

    private void updateOrDeleteExisting(
            RequestMemberEntity existing,
            RequestMemberType desiredType) {
        if (desiredType == null) {
            deleteMember(existing.getId());
        } else {
            updateMemberType(existing.getId(), desiredType);
        }
    }

    private void deleteMember(Long memberId) {
        if (requestMemberMapper.deleteById(memberId) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void updateMemberType(
            Long memberId,
            RequestMemberType memberType) {
        RequestMemberEntity update = new RequestMemberEntity();
        update.setId(memberId);
        update.setMemberType(memberType);
        if (requestMemberMapper.updateById(update) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void insertMember(
            Long requestId,
            Long userId,
            RequestMemberType memberType,
            Instant joinedAt) {
        RequestMemberEntity member = new RequestMemberEntity();
        member.setRequestId(requestId);
        member.setUserId(userId);
        member.setMemberType(memberType);
        member.setJoinedAt(joinedAt);
        if (requestMemberMapper.insert(member) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void insertStatusHistory(
            Long requestId,
            Long operatorId,
            RequestStatus oldStatus,
            String oldOwnerName,
            String newOwnerName,
            UpdateRequestMembersCommand command) {
        StatusHistoryEntity history = new StatusHistoryEntity();
        history.setRequestId(requestId);
        history.setOperatorId(operatorId);
        history.setFromStatus(oldStatus);
        history.setToStatus(RequestStatus.IN_PROGRESS);
        history.setReason(
                "任务成员调整：负责人由 "
                        + oldOwnerName
                        + " 调整为 "
                        + newOwnerName
                        + "；参与成员 "
                        + command.participantIds().size()
                        + " 人；原因："
                        + command.reason().trim());
        history.setCreatedAt(Instant.now());
        if (statusHistoryMapper.insert(history) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String resolveOldOwnerName(
            List<RequestMemberEntity> existingMembers) {
        Long ownerId = existingMembers.stream()
                .filter(member -> member.getMemberType() == RequestMemberType.OWNER)
                .map(RequestMemberEntity::getUserId)
                .findFirst()
                .orElse(null);
        if (ownerId == null) {
            return "无";
        }
        UserEntity owner = userMapper.selectById(ownerId);
        return owner == null ? "未知用户" : owner.getDisplayName();
    }

    private List<Map<String, Object>> existingMemberSnapshot(
            List<RequestMemberEntity> members) {
        return members.stream()
                .map(member -> Map.<String, Object>of(
                        "userId", member.getUserId(),
                        "memberType", member.getMemberType()))
                .toList();
    }

    private List<Map<String, Object>> desiredMemberSnapshot(
            Map<Long, RequestMemberType> desiredTypes) {
        return desiredTypes.entrySet()
                .stream()
                .map(entry -> Map.<String, Object>of(
                        "userId", entry.getKey(),
                        "memberType", entry.getValue()))
                .toList();
    }

    private RequestAssignmentVO buildAssignmentVO(
            RequestEntity request,
            List<RequestMemberEntity> members,
            Map<Long, UserEntity> users) {
        List<RequestMemberEntity> owners = members.stream()
                .filter(member -> member.getMemberType() == RequestMemberType.OWNER)
                .toList();
        if (owners.size() > 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        RequestMemberVO owner = owners.isEmpty()
                ? null
                : toRequestMemberVO(owners.getFirst(), users);
        List<RequestMemberVO> participants = members.stream()
                .filter(member -> member.getMemberType() == RequestMemberType.PARTICIPANT)
                .map(member -> toRequestMemberVO(member, users))
                .toList();

        return new RequestAssignmentVO(
                request.getId().toString(),
                request.getStatus(),
                request.getVersion(),
                owner,
                participants);
    }

    private RequestMemberVO toRequestMemberVO(
            RequestMemberEntity member,
            Map<Long, UserEntity> users) {
        UserEntity user = users.get(member.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return new RequestMemberVO(
                member.getId().toString(),
                member.getUserId().toString(),
                user.getDisplayName(),
                user.getRole(),
                member.getMemberType(),
                member.getJoinedAt());
    }

    private RequestEntity findRequest(Long requestId) {
        if (requestId == null || requestId <= 0) {
            throw hiddenRequest();
        }
        RequestEntity request = requestMapper.selectById(requestId);
        if (request == null) {
            throw hiddenRequest();
        }
        return request;
    }

    private RequestEntity findRequestForUpdate(Long requestId) {
        if (requestId == null || requestId <= 0) {
            throw hiddenRequest();
        }
        RequestEntity request = requestMapper.selectByIdForUpdate(requestId);
        if (request == null) {
            throw hiddenRequest();
        }
        return request;
    }

    private RequestEntity findVisibleRequest(
            Long requestId,
            LoginUser viewer) {
        if (viewer == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        RequestEntity request = findRequest(requestId);
        switch (viewer.role()) {
            case REQUESTER -> {
                if (!Objects.equals(request.getCreatorId(), viewer.id())) {
                    throw hiddenRequest();
                }
            }
            case MEMBER -> {
                if (request.getStatus() == RequestStatus.DRAFT) {
                    throw hiddenRequest();
                }
            }
            case ADMIN -> {
                // 管理员可以查看全部需求。
            }
            default -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return request;
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
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "需求不存在");
    }

    private BusinessException statusConflict() {
        return new BusinessException(
                ErrorCode.REQUEST_STATUS_CONFLICT,
                "需求或任务成员已发生变化，请刷新后重试");
    }

    private BusinessException invalidArgument(String message) {
        return new BusinessException(ErrorCode.INVALID_ARGUMENT, message);
    }
}
