package cn.edu.techgroup.outsourcing.modules.progress.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEvents;
import cn.edu.techgroup.outsourcing.modules.progress.dto.CreateProgressCommand;
import cn.edu.techgroup.outsourcing.modules.progress.entity.ProgressLogEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.ProgressLogMapper;
import cn.edu.techgroup.outsourcing.modules.progress.service.ProgressService;
import cn.edu.techgroup.outsourcing.modules.progress.vo.CreatedProgressResultVO;
import cn.edu.techgroup.outsourcing.modules.progress.vo.ProgressLogVO;
import cn.edu.techgroup.outsourcing.modules.progress.vo.RequestProgressSnapshotVO;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProgressServiceImpl implements ProgressService {

    private static final int MIN_CONTENT_LENGTH = 5;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_NEXT_PLAN_LENGTH = 2000;
    private static final Duration FOLLOW_UP_INTERVAL = Duration.ofDays(7);

    private final RequestMapper requestMapper;
    private final RequestMemberMapper requestMemberMapper;
    private final ProgressLogMapper progressLogMapper;
    private final UserMapper userMapper;
    private final NotificationEventPublisher notificationEventPublisher;

    public ProgressServiceImpl(
            RequestMapper requestMapper,
            RequestMemberMapper requestMemberMapper,
            ProgressLogMapper progressLogMapper,
            UserMapper userMapper,
            NotificationEventPublisher notificationEventPublisher) {
        this.requestMapper = requestMapper;
        this.requestMemberMapper = requestMemberMapper;
        this.progressLogMapper = progressLogMapper;
        this.userMapper = userMapper;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public RequestProgressSnapshotVO get(
            Long requestId,
            LoginUser viewer) {
        RequestEntity request = findVisibleRequest(requestId, viewer);
        List<RequestMemberEntity> members = viewer.role() == UserRole.MEMBER
                ? requestMemberMapper.selectByRequestId(requestId)
                : List.of();
        boolean owner = isOwner(members, viewer.id());
        boolean includeInternal = viewer.role() != UserRole.REQUESTER;
        boolean canUpdate = request.getStatus() == RequestStatus.IN_PROGRESS
                && (viewer.role() == UserRole.ADMIN || owner);

        List<ProgressLogEntity> logs = progressLogMapper.selectByRequestId(
                requestId,
                includeInternal);
        Map<Long, String> authorNames = loadAuthorNames(logs);
        List<ProgressLogVO> logViews = logs.stream()
                .map(log -> toVO(
                        log,
                        authorNames.getOrDefault(log.getAuthorId(), "未知用户")))
                .toList();
        ProgressLogEntity latestLog = logs.isEmpty() ? null : logs.getFirst();
        Instant lastProgressAt = latestLog == null ? null : latestLog.getCreatedAt();
        Instant nextUpdateAt = latestLog == null ? null : latestLog.getNextUpdateAt();
        boolean needsFollowUp = needsFollowUp(
                request,
                lastProgressAt,
                nextUpdateAt,
                Instant.now());

        return new RequestProgressSnapshotVO(
                request.getId().toString(),
                request.getStatus(),
                request.getVersion(),
                request.getProgress(),
                lastProgressAt,
                nextUpdateAt,
                needsFollowUp,
                canUpdate,
                logViews);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatedProgressResultVO create(
            Long requestId,
            CreateProgressCommand command,
            LoginUser operator) {
        requireTeamMember(operator);
        validateCommand(requestId, command);

        RequestEntity request = findRequest(requestId);
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw statusConflict("只有处理中的需求可以更新进度");
        }
        if (!Objects.equals(request.getVersion(), command.requestVersion())) {
            throw statusConflict("需求已被其他成员更新，请刷新后重试");
        }
        if (operator.role() != UserRole.ADMIN) {
            List<RequestMemberEntity> members =
                    requestMemberMapper.selectByRequestId(requestId);
            if (!isOwner(members, operator.id())) {
                throw new BusinessException(
                        ErrorCode.ACCESS_DENIED,
                        "只有主负责人或管理员可以更新进度");
            }
        }

        int updatedRows = requestMapper.compareAndSetProgress(
                requestId,
                RequestStatus.IN_PROGRESS.getValue(),
                command.requestVersion(),
                command.progress());
        if (updatedRows != 1) {
            throw statusConflict("需求已被其他成员更新，请刷新后重试");
        }

        Instant createdAt = Instant.now();
        ProgressLogEntity log = new ProgressLogEntity();
        log.setRequestId(requestId);
        log.setAuthorId(operator.id());
        log.setProgress(command.progress());
        log.setContent(command.content());
        log.setNextPlan(command.nextPlan());
        log.setNextUpdateAt(command.nextUpdateAt());
        log.setVisibleToRequester(command.visibleToRequester());
        log.setCreatedAt(createdAt);
        if (progressLogMapper.insert(log) != 1 || log.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        if (Boolean.TRUE.equals(command.visibleToRequester())) {
            notificationEventPublisher.publish(
                    NotificationEvents.progressUpdated(
                            request.getId(),
                            request.getRequestNo(),
                            operator.id(),
                            request.getCreatorId()));
        }

        ProgressLogVO logView = toVO(log, operator.displayName());
        return new CreatedProgressResultVO(
                logView,
                command.progress(),
                command.requestVersion() + 1);
    }

    private void validateCommand(
            Long requestId,
            CreateProgressCommand command) {
        if (requestId == null || requestId <= 0) {
            throw hiddenRequest();
        }
        if (command == null) {
            throw invalidArgument("请求参数不能为空");
        }
        if (command.requestVersion() == null || command.requestVersion() < 0) {
            throw invalidArgument("请求版本不正确");
        }
        if (command.progress() == null
                || command.progress() < 0
                || command.progress() > 100) {
            throw invalidArgument("进度必须为 0～100 的整数");
        }
        if (!StringUtils.hasText(command.content())
                || command.content().length() < MIN_CONTENT_LENGTH
                || command.content().length() > MAX_CONTENT_LENGTH) {
            throw invalidArgument("进度说明应为 5～2000 个字符");
        }
        if (command.nextPlan() != null
                && command.nextPlan().length() > MAX_NEXT_PLAN_LENGTH) {
            throw invalidArgument("下一步计划不能超过 2000 个字符");
        }
        if (command.nextUpdateAt() != null
                && !command.nextUpdateAt().isAfter(Instant.now())) {
            throw invalidArgument("预计下次更新时间必须晚于当前时间");
        }
        if (command.visibleToRequester() == null) {
            throw invalidArgument("请选择进度记录的可见范围");
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
                // 管理员可以查看全部需求。
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

    private Map<Long, String> loadAuthorNames(List<ProgressLogEntity> logs) {
        Set<Long> authorIds = logs.stream()
                .map(ProgressLogEntity::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (authorIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectAssignmentUsersByIds(authorIds)
                .stream()
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        UserEntity::getDisplayName));
    }

    private boolean isOwner(
            List<RequestMemberEntity> members,
            Long userId) {
        return members.stream()
                .anyMatch(member -> Objects.equals(member.getUserId(), userId)
                        && member.getMemberType() == RequestMemberType.OWNER);
    }

    private boolean needsFollowUp(
            RequestEntity request,
            Instant lastProgressAt,
            Instant nextUpdateAt,
            Instant now) {
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            return false;
        }
        if (nextUpdateAt != null && !nextUpdateAt.isAfter(now)) {
            return true;
        }
        Instant activityAt = lastProgressAt == null
                ? request.getUpdatedAt()
                : lastProgressAt;
        return activityAt != null
                && !activityAt.plus(FOLLOW_UP_INTERVAL).isAfter(now);
    }

    private ProgressLogVO toVO(
            ProgressLogEntity log,
            String authorName) {
        return new ProgressLogVO(
                log.getId().toString(),
                log.getRequestId().toString(),
                log.getAuthorId().toString(),
                authorName,
                log.getProgress(),
                log.getContent(),
                log.getNextPlan(),
                log.getNextUpdateAt(),
                Boolean.TRUE.equals(log.getVisibleToRequester()),
                log.getCreatedAt());
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

    private BusinessException hiddenRequest() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "需求不存在");
    }

    private BusinessException invalidArgument(String message) {
        return new BusinessException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private BusinessException statusConflict(String message) {
        return new BusinessException(ErrorCode.REQUEST_STATUS_CONFLICT, message);
    }
}
