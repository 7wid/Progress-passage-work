package cn.edu.techgroup.outsourcing.modules.delivery.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.delivery.dto.CreateAcceptanceCommand;
import cn.edu.techgroup.outsourcing.modules.delivery.dto.CreateDeliveryCommand;
import cn.edu.techgroup.outsourcing.modules.delivery.entity.AcceptanceEntity;
import cn.edu.techgroup.outsourcing.modules.delivery.entity.DeliveryEntity;
import cn.edu.techgroup.outsourcing.modules.delivery.enums.AcceptanceResult;
import cn.edu.techgroup.outsourcing.modules.delivery.mapper.AcceptanceMapper;
import cn.edu.techgroup.outsourcing.modules.delivery.mapper.DeliveryMapper;
import cn.edu.techgroup.outsourcing.modules.delivery.service.DeliveryAcceptanceService;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.AcceptanceVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.CreatedAcceptanceResultVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.CreatedDeliveryResultVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.DeliveryAcceptanceSnapshotVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.DeliveryVO;
import cn.edu.techgroup.outsourcing.modules.file.service.AttachmentService;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentVO;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEvents;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DeliveryAcceptanceServiceImpl
        implements DeliveryAcceptanceService {

    private static final int MIN_DESCRIPTION_LENGTH = 5;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;
    private static final int MIN_COMMENT_LENGTH = 5;
    private static final int MAX_COMMENT_LENGTH = 2000;
    private static final int MAX_URL_LENGTH = 1000;

    private final RequestMapper requestMapper;
    private final RequestMemberMapper requestMemberMapper;
    private final DeliveryMapper deliveryMapper;
    private final AcceptanceMapper acceptanceMapper;
    private final StatusHistoryMapper statusHistoryMapper;
    private final UserMapper userMapper;
    private final AttachmentService attachmentService;
    private final NotificationEventPublisher notificationEventPublisher;

    public DeliveryAcceptanceServiceImpl(
            RequestMapper requestMapper,
            RequestMemberMapper requestMemberMapper,
            DeliveryMapper deliveryMapper,
            AcceptanceMapper acceptanceMapper,
            StatusHistoryMapper statusHistoryMapper,
            UserMapper userMapper,
            AttachmentService attachmentService,
            NotificationEventPublisher notificationEventPublisher) {
        this.requestMapper = requestMapper;
        this.requestMemberMapper = requestMemberMapper;
        this.deliveryMapper = deliveryMapper;
        this.acceptanceMapper = acceptanceMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.userMapper = userMapper;
        this.attachmentService = attachmentService;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAcceptanceSnapshotVO get(
            Long requestId,
            LoginUser viewer) {
        RequestEntity request = findVisibleRequest(requestId, viewer);
        List<RequestMemberEntity> members = viewer.role() == UserRole.MEMBER
                ? requestMemberMapper.selectByRequestId(requestId)
                : List.of();
        List<DeliveryEntity> deliveries =
                deliveryMapper.selectByRequestId(requestId);
        List<AcceptanceEntity> acceptances =
                acceptanceMapper.selectByRequestId(requestId);
        Map<Long, String> userNames = loadUserNames(deliveries, acceptances);

        boolean canSubmitDelivery = request.getStatus() == RequestStatus.IN_PROGRESS
                && (viewer.role() == UserRole.ADMIN
                        || isOwner(members, viewer.id()));
        DeliveryEntity latestDelivery = deliveries.isEmpty()
                ? null
                : deliveries.getFirst();
        Set<Long> acceptedDeliveryIds = acceptances.stream()
                .map(AcceptanceEntity::getDeliveryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean hasUnacceptedLatestDelivery = latestDelivery != null
                && !acceptedDeliveryIds.contains(latestDelivery.getId());
        boolean canAccept = request.getStatus() == RequestStatus.PENDING_ACCEPTANCE
                && hasUnacceptedLatestDelivery
                && (viewer.role() == UserRole.ADMIN
                        || (viewer.role() == UserRole.REQUESTER
                                && Objects.equals(
                                        request.getCreatorId(),
                                        viewer.id())));

        return new DeliveryAcceptanceSnapshotVO(
                request.getId().toString(),
                request.getStatus(),
                request.getVersion(),
                canSubmitDelivery,
                canAccept,
                deliveries.stream()
                        .map(delivery -> toVO(delivery, userNames,
                                attachmentService.findBoundDeliveryAttachments(
                                        requestId, delivery.getId(), viewer)))
                        .toList(),
                acceptances.stream()
                        .map(acceptance -> toVO(acceptance, userNames))
                        .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatedDeliveryResultVO createDelivery(
            Long requestId,
            CreateDeliveryCommand command,
            LoginUser operator) {
        requireTeamMember(operator);
        validateDeliveryCommand(requestId, command);

        RequestEntity request = findRequest(requestId);
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw statusConflict("只有处理中的需求可以提交交付物");
        }
        requireExpectedVersion(request, command.requestVersion());
        if (operator.role() != UserRole.ADMIN) {
            List<RequestMemberEntity> members =
                    requestMemberMapper.selectByRequestId(requestId);
            if (!isOwner(members, operator.id())) {
                throw accessDenied("只有主负责人或管理员可以提交交付物");
            }
        }

        int updatedRows = requestMapper.compareAndSetStatus(
                requestId,
                RequestStatus.IN_PROGRESS.getValue(),
                RequestStatus.PENDING_ACCEPTANCE.getValue(),
                command.requestVersion());
        if (updatedRows != 1) {
            throw statusConflict("需求已发生变化，请刷新后重试");
        }

        Instant createdAt = Instant.now();
        DeliveryEntity delivery = new DeliveryEntity();
        delivery.setRequestId(requestId);
        delivery.setSubmitterId(operator.id());
        delivery.setDescription(command.description());
        delivery.setDeliveryUrl(command.deliveryUrl());
        delivery.setCreatedAt(createdAt);
        if (deliveryMapper.insert(delivery) != 1 || delivery.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        List<AttachmentVO> attachments =
                attachmentService.bindPendingDeliveryAttachments(
                        requestId, delivery.getId(), command.attachmentIds(), operator);

        insertStatusHistory(
                requestId,
                operator.id(),
                RequestStatus.IN_PROGRESS,
                RequestStatus.PENDING_ACCEPTANCE,
                "提交交付物，等待需求方验收");

        notificationEventPublisher.publish(
                NotificationEvents.deliverySubmitted(
                        request.getId(),
                        request.getRequestNo(),
                        operator.id(),
                        request.getCreatorId()));

        return new CreatedDeliveryResultVO(
                toVO(delivery, Map.of(operator.id(), operator.displayName()), attachments),
                RequestStatus.PENDING_ACCEPTANCE,
                command.requestVersion() + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatedAcceptanceResultVO createAcceptance(
            Long requestId,
            CreateAcceptanceCommand command,
            LoginUser operator) {
        requireRequesterOrAdmin(operator);
        validateAcceptanceCommand(requestId, command);

        RequestEntity request = findRequest(requestId);
        if (operator.role() != UserRole.ADMIN
                && !Objects.equals(request.getCreatorId(), operator.id())) {
            throw hiddenRequest();
        }
        if (request.getStatus() != RequestStatus.PENDING_ACCEPTANCE) {
            throw statusConflict("只有待验收的需求可以提交验收结果");
        }
        requireExpectedVersion(request, command.requestVersion());

        DeliveryEntity latestDelivery =
                deliveryMapper.selectLatestByRequestId(requestId);
        if (latestDelivery == null) {
            throw statusConflict("当前需求没有可验收的交付物");
        }
        if (acceptanceMapper.countByDeliveryId(latestDelivery.getId()) > 0) {
            throw statusConflict("最新交付物已经验收，请刷新后重试");
        }

        RequestStatus targetStatus = command.result() == AcceptanceResult.ACCEPTED
                ? RequestStatus.COMPLETED
                : RequestStatus.IN_PROGRESS;
        int updatedRows = requestMapper.compareAndSetStatus(
                requestId,
                RequestStatus.PENDING_ACCEPTANCE.getValue(),
                targetStatus.getValue(),
                command.requestVersion());
        if (updatedRows != 1) {
            throw statusConflict("需求已发生变化，请刷新后重试");
        }

        Instant createdAt = Instant.now();
        AcceptanceEntity acceptance = new AcceptanceEntity();
        acceptance.setRequestId(requestId);
        acceptance.setDeliveryId(latestDelivery.getId());
        acceptance.setOperatorId(operator.id());
        acceptance.setResult(command.result());
        acceptance.setComment(command.comment());
        acceptance.setCreatedAt(createdAt);
        if (acceptanceMapper.insert(acceptance) != 1
                || acceptance.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        String reason = command.result() == AcceptanceResult.ACCEPTED
                ? "需求方验收通过"
                : "验收退回：" + command.comment();
        insertStatusHistory(
                requestId,
                operator.id(),
                RequestStatus.PENDING_ACCEPTANCE,
                targetStatus,
                reason);

        List<Long> recipients = requestMemberMapper.selectByRequestId(requestId)
                .stream()
                .map(RequestMemberEntity::getUserId)
                .toList();
        notificationEventPublisher.publish(
                NotificationEvents.acceptanceCompleted(
                        request.getId(),
                        request.getRequestNo(),
                        operator.id(),
                        recipients,
                        command.result() == AcceptanceResult.ACCEPTED));

        return new CreatedAcceptanceResultVO(
                toVO(acceptance, Map.of(operator.id(), operator.displayName())),
                targetStatus,
                command.requestVersion() + 1);
    }

    private void validateDeliveryCommand(
            Long requestId,
            CreateDeliveryCommand command) {
        validateRequestIdAndVersion(
                requestId,
                command == null ? null : command.requestVersion());
        if (command == null) {
            throw invalidArgument("请求参数不能为空");
        }
        if (!StringUtils.hasText(command.description())
                || command.description().length() < MIN_DESCRIPTION_LENGTH
                || command.description().length() > MAX_DESCRIPTION_LENGTH) {
            throw invalidArgument("交付说明应为 5～5000 个字符");
        }
        validateDeliveryUrl(command.deliveryUrl());
        if (command.attachmentIds() == null || command.attachmentIds().size() > 5
                || command.attachmentIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw invalidArgument("交付附件标识不正确，每次最多选择 5 个附件");
        }
    }

    private void validateDeliveryUrl(String deliveryUrl) {
        if (deliveryUrl == null) {
            return;
        }
        if (deliveryUrl.length() > MAX_URL_LENGTH) {
            throw invalidArgument("交付地址不能超过 1000 个字符");
        }
        try {
            URI uri = new URI(deliveryUrl);
            String scheme = uri.getScheme() == null
                    ? ""
                    : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme))
                    || !StringUtils.hasText(uri.getHost())
                    || uri.getUserInfo() != null) {
                throw invalidArgument("交付地址必须是有效的 HTTP 或 HTTPS 地址");
            }
        } catch (URISyntaxException exception) {
            throw invalidArgument("交付地址必须是有效的 HTTP 或 HTTPS 地址");
        }
    }

    private void validateAcceptanceCommand(
            Long requestId,
            CreateAcceptanceCommand command) {
        validateRequestIdAndVersion(
                requestId,
                command == null ? null : command.requestVersion());
        if (command == null || command.result() == null) {
            throw invalidArgument("请选择验收结果");
        }
        String comment = command.comment();
        boolean commentRequired =
                command.result() == AcceptanceResult.REWORK_REQUIRED;
        if (commentRequired
                && (!StringUtils.hasText(comment)
                        || comment.length() < MIN_COMMENT_LENGTH)) {
            throw invalidArgument("当前验收结果必须填写至少 5 个字符的说明");
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw invalidArgument("验收说明不能超过 2000 个字符");
        }
    }

    private void validateRequestIdAndVersion(
            Long requestId,
            Integer requestVersion) {
        if (requestId == null || requestId <= 0) {
            throw hiddenRequest();
        }
        if (requestVersion == null || requestVersion < 0) {
            throw invalidArgument("需求版本不正确");
        }
    }

    private void requireExpectedVersion(
            RequestEntity request,
            Integer requestVersion) {
        if (!Objects.equals(request.getVersion(), requestVersion)) {
            throw statusConflict("需求已发生变化，请刷新后重试");
        }
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
                // Administrators may view all requests.
            }
            default -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return request;
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

    private void requireTeamMember(LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (operator.role() != UserRole.MEMBER
                && operator.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void requireRequesterOrAdmin(LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (operator.role() != UserRole.REQUESTER
                && operator.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private boolean isOwner(
            List<RequestMemberEntity> members,
            Long userId) {
        return members.stream()
                .anyMatch(member -> Objects.equals(member.getUserId(), userId)
                        && member.getMemberType() == RequestMemberType.OWNER);
    }

    private Map<Long, String> loadUserNames(
            List<DeliveryEntity> deliveries,
            List<AcceptanceEntity> acceptances) {
        Set<Long> userIds = new HashSet<>();
        deliveries.stream()
                .map(DeliveryEntity::getSubmitterId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        acceptances.stream()
                .map(AcceptanceEntity::getOperatorId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectAssignmentUsersByIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        UserEntity::getDisplayName));
    }

    private DeliveryVO toVO(
            DeliveryEntity delivery,
            Map<Long, String> userNames,
            List<AttachmentVO> attachments) {
        return new DeliveryVO(
                delivery.getId().toString(),
                delivery.getRequestId().toString(),
                delivery.getSubmitterId().toString(),
                userNames.getOrDefault(delivery.getSubmitterId(), "未知用户"),
                delivery.getDescription(),
                delivery.getDeliveryUrl(),
                attachments,
                delivery.getCreatedAt());
    }

    private AcceptanceVO toVO(
            AcceptanceEntity acceptance,
            Map<Long, String> userNames) {
        return new AcceptanceVO(
                acceptance.getId().toString(),
                acceptance.getRequestId().toString(),
                acceptance.getDeliveryId() == null
                        ? null
                        : acceptance.getDeliveryId().toString(),
                acceptance.getOperatorId().toString(),
                userNames.getOrDefault(acceptance.getOperatorId(), "未知用户"),
                acceptance.getResult(),
                acceptance.getComment(),
                acceptance.getCreatedAt());
    }

    private void insertStatusHistory(
            Long requestId,
            Long operatorId,
            RequestStatus fromStatus,
            RequestStatus toStatus,
            String reason) {
        StatusHistoryEntity history = new StatusHistoryEntity();
        history.setRequestId(requestId);
        history.setOperatorId(operatorId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setReason(truncate(reason, 1000));
        history.setCreatedAt(Instant.now());
        if (statusHistoryMapper.insert(history) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }

    private BusinessException hiddenRequest() {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "需求不存在");
    }

    private BusinessException invalidArgument(String message) {
        return new BusinessException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private BusinessException accessDenied(String message) {
        return new BusinessException(ErrorCode.ACCESS_DENIED, message);
    }

    private BusinessException statusConflict(String message) {
        return new BusinessException(
                ErrorCode.REQUEST_STATUS_CONFLICT,
                message);
    }
}
