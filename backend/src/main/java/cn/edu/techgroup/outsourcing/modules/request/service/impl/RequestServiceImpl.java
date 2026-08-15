package cn.edu.techgroup.outsourcing.modules.request.service.impl;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditActions;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;
import cn.edu.techgroup.outsourcing.modules.category.mapper.CategoryMapper;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEvents;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.dto.CreateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.RequestListQuery;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestNumberGenerator;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestService;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestDetailVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestSummaryVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.StatusHistoryVO;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RequestServiceImpl implements RequestService {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Shanghai");
    private final UserMapper userMapper;
    private final RequestMapper requestMapper;
    private final CategoryMapper categoryMapper;
    private final StatusHistoryMapper statusHistoryMapper;
    private final RequestMemberMapper requestMemberMapper;
    private final RequestNumberGenerator requestNumberGenerator;
    private final NotificationEventPublisher notificationEventPublisher;
    private final AuditRecorder auditRecorder;

    public RequestServiceImpl(
            RequestMapper requestMapper,
            CategoryMapper categoryMapper,
            StatusHistoryMapper statusHistoryMapper,
            RequestNumberGenerator requestNumberGenerator,
            RequestMemberMapper requestMemberMapper,
            UserMapper userMapper,
            NotificationEventPublisher notificationEventPublisher,
            AuditRecorder auditRecorder) {
        this.requestMapper = requestMapper;
        this.categoryMapper = categoryMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.requestNumberGenerator = requestNumberGenerator;
        this.userMapper = userMapper;
        this.requestMemberMapper = requestMemberMapper;
        this.notificationEventPublisher = notificationEventPublisher;
        this.auditRecorder = auditRecorder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatedRequestVO createAndSubmit(
            CreateRequestCommand command,
            LoginUser operator) {

        CategoryEntity category =
                categoryMapper.selectById(command.categoryId());

        if (category == null
                || !Boolean.TRUE.equals(category.getEnabled())) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "需求分类不存在或已经停用");
        }

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (command.expectedDeadline().isBefore(today)) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "期望完成日期不能早于今天");
        }

        if (!Boolean.TRUE.equals(command.informationConfirmed())) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "提交前必须确认信息真实有效");
        }

        Instant submittedAt = Instant.now();

        RequestEntity entity = new RequestEntity();
        entity.setCreatorId(operator.id());
        entity.setCategoryId(command.categoryId());
        entity.setTitle(command.title().trim());
        entity.setBackground(command.background().trim());
        entity.setDescription(command.description().trim());
        entity.setExpectedResult(command.expectedResult().trim());
        entity.setExpectedDeadline(command.expectedDeadline());
        entity.setUrgency(command.urgency());
        entity.setBudgetAmount(command.budgetAmount());
        entity.setBudgetDescription(
                trimToNull(command.budgetDescription()));
        entity.setTechnicalConstraints(
                trimToNull(command.technicalConstraints()));
        entity.setContactInfo(command.contactInfo().trim());
        entity.setStatus(RequestStatus.PENDING_REVIEW);
        entity.setProgress(0);
        entity.setVersion(0);
        entity.setSubmittedAt(submittedAt);

        int insertedRows = requestMapper.insert(entity);
        if (insertedRows != 1 || entity.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        String requestNo = requestNumberGenerator.generate(
                entity.getId(),
                submittedAt);

        int updatedRows = requestMapper.update(
                null,
                Wrappers.<RequestEntity>lambdaUpdate()
                        .eq(RequestEntity::getId, entity.getId())
                        .set(RequestEntity::getRequestNo, requestNo));

        if (updatedRows != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        entity.setRequestNo(requestNo);

        StatusHistoryEntity history = new StatusHistoryEntity();
        history.setRequestId(entity.getId());
        history.setOperatorId(operator.id());
        history.setFromStatus(null);
        history.setToStatus(RequestStatus.PENDING_REVIEW);
        history.setReason("需求创建并提交");

        if (statusHistoryMapper.insert(history) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        auditRecorder.record(
                operator.id(),
                AuditActions.REQUEST_SUBMITTED,
                "REQUEST",
                entity.getId().toString(),
                null,
                Map.of(
                        "requestNo", requestNo,
                        "status", RequestStatus.PENDING_REVIEW.name()));

        notificationEventPublisher.publish(
                NotificationEvents.requestSubmitted(
                        entity.getId(),
                        entity.getRequestNo(),
                        operator.id(),
                        List.of()));

        return CreatedRequestVO.from(entity);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RequestSummaryVO> list(
            RequestListQuery query,
            LoginUser viewer) {

        validateQuery(query);

        LambdaQueryWrapper<RequestEntity> wrapper = visibleRequests(viewer);
        wrapper.select(
                RequestEntity::getId,
                RequestEntity::getRequestNo,
                RequestEntity::getCreatorId,
                RequestEntity::getCategoryId,
                RequestEntity::getTitle,
                RequestEntity::getExpectedDeadline,
                RequestEntity::getUrgency,
                RequestEntity::getStatus,
                RequestEntity::getProgress,
                RequestEntity::getSubmittedAt,
                RequestEntity::getCreatedAt);

        applyFilters(wrapper, query);
        applySort(wrapper, query);

        Page<RequestEntity> result = requestMapper.selectPage(
                new Page<>(query.page(), query.pageSize()),
                wrapper);

        List<RequestEntity> records = result.getRecords();
        Map<Long, String> categoryNames = loadCategoryNames(records);
        Map<Long, String> creatorNames = loadUserNames(
                records.stream()
                        .map(RequestEntity::getCreatorId)
                        .toList());

        List<RequestSummaryVO> items = records.stream()
                .map(entity -> new RequestSummaryVO(
                        entity.getId().toString(),
                        entity.getRequestNo(),
                        entity.getTitle(),
                        entity.getCategoryId().toString(),
                        categoryNames.getOrDefault(entity.getCategoryId(), "未知分类"),
                        creatorNames.getOrDefault(entity.getCreatorId(), "未知用户"),
                        entity.getUrgency(),
                        entity.getStatus(),
                        entity.getProgress(),
                        entity.getExpectedDeadline(),
                        entity.getSubmittedAt(),
                        entity.getCreatedAt()))
                .toList();

        return PageResponse.of(
                items,
                result.getCurrent(),
                result.getSize(),
                result.getTotal());
    }

    private void applyFilters(
            LambdaQueryWrapper<RequestEntity> wrapper,
            RequestListQuery query) {
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(condition -> condition
                    .like(RequestEntity::getRequestNo, query.keyword())
                    .or()
                    .like(RequestEntity::getTitle, query.keyword()));
        }

        if (query.status() != null) {
            wrapper.eq(RequestEntity::getStatus, query.status());
        }

        if (query.categoryId() != null) {
            wrapper.eq(RequestEntity::getCategoryId, query.categoryId());
        }

        if (query.submittedFrom() != null) {
            wrapper.ge(
                    RequestEntity::getSubmittedAt,
                    query.submittedFrom().atStartOfDay(BUSINESS_ZONE).toInstant());
        }

        if (query.submittedTo() != null) {
            wrapper.lt(
                    RequestEntity::getSubmittedAt,
                    query.submittedTo().plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant());
        }
    }

    private void applySort(
            LambdaQueryWrapper<RequestEntity> wrapper,
            RequestListQuery query) {
        switch (query.sort()) {
            case NEWEST -> wrapper
                    .orderByDesc(RequestEntity::getSubmittedAt)
                    .orderByDesc(RequestEntity::getId);
            case OLDEST -> wrapper
                    .orderByAsc(RequestEntity::getSubmittedAt)
                    .orderByAsc(RequestEntity::getId);
            case DEADLINE_ASC -> wrapper
                    .orderByAsc(RequestEntity::getExpectedDeadline)
                    .orderByDesc(RequestEntity::getId);
            case DEADLINE_DESC -> wrapper
                    .orderByDesc(RequestEntity::getExpectedDeadline)
                    .orderByDesc(RequestEntity::getId);
        }
    }

    private LambdaQueryWrapper<RequestEntity> visibleRequests(LoginUser viewer) {
        if (viewer == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        LambdaQueryWrapper<RequestEntity> wrapper = Wrappers.lambdaQuery();
        switch (viewer.role()) {
            case REQUESTER -> wrapper.eq(RequestEntity::getCreatorId, viewer.id());
            case MEMBER -> wrapper.ne(RequestEntity::getStatus, RequestStatus.DRAFT);
            case ADMIN -> {
                // 管理员不添加数据范围条件。
            }
            default -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return wrapper;
    }

    private Map<Long, String> loadCategoryNames(List<RequestEntity> records) {
        Set<Long> ids = records.stream()
                .map(RequestEntity::getCategoryId)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        return categoryMapper.selectList(
                        Wrappers.<CategoryEntity>lambdaQuery()
                                .select(CategoryEntity::getId, CategoryEntity::getName)
                                .in(CategoryEntity::getId, ids))
                .stream()
                .collect(Collectors.toMap(
                        CategoryEntity::getId,
                        CategoryEntity::getName));
    }

    private Map<Long, String> loadUserNames(Collection<Long> userIds) {
        Set<Long> ids = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        return userMapper.selectList(
                        Wrappers.<UserEntity>lambdaQuery()
                                .select(UserEntity::getId, UserEntity::getDisplayName)
                                .in(UserEntity::getId, ids))
                .stream()
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        UserEntity::getDisplayName));
    }

    private void validateQuery(RequestListQuery query) {
        if (query.page() < 1
                || query.pageSize() < 1
                || query.pageSize() > 100) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "分页参数不正确");
        }

        if (query.submittedFrom() != null
                && query.submittedTo() != null
                && query.submittedFrom().isAfter(query.submittedTo())) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "开始日期不能晚于结束日期");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RequestDetailVO getDetail(
            Long requestId,
            LoginUser viewer) {

        if (requestId == null || requestId <= 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        RequestEntity entity = requestMapper.selectOne(
                visibleRequests(viewer)
                        .eq(RequestEntity::getId, requestId));

        if (entity == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "需求不存在");
        }

        CategoryEntity category = categoryMapper.selectById(entity.getCategoryId());
        UserEntity creator = userMapper.selectById(entity.getCreatorId());

        List<StatusHistoryEntity> histories = statusHistoryMapper.selectList(
                Wrappers.<StatusHistoryEntity>lambdaQuery()
                        .eq(StatusHistoryEntity::getRequestId, entity.getId())
                        .orderByAsc(StatusHistoryEntity::getCreatedAt)
                        .orderByAsc(StatusHistoryEntity::getId));

        Map<Long, String> operatorNames = loadUserNames(
                histories.stream()
                        .map(StatusHistoryEntity::getOperatorId)
                        .toList());

        List<StatusHistoryVO> historyVOs = histories.stream()
                .map(history -> new StatusHistoryVO(
                        history.getId().toString(),
                        history.getFromStatus(),
                        history.getToStatus(),
                        history.getReason(),
                        operatorNames.getOrDefault(history.getOperatorId(), "未知用户"),
                        history.getCreatedAt()))
                .toList();

        boolean assignedMember =
                viewer.role() == UserRole.MEMBER
                        && requestMemberMapper
                                .countByRequestIdAndUserId(
                                        entity.getId(),
                                        viewer.id()) > 0;

        boolean canViewContact =
                viewer.role() == UserRole.ADMIN
                        || entity.getCreatorId().equals(viewer.id())
                        || assignedMember;

        return new RequestDetailVO(
                entity.getId().toString(),
                entity.getRequestNo(),
                entity.getTitle(),
                entity.getCategoryId().toString(),
                category == null ? "未知分类" : category.getName(),
                entity.getCreatorId().toString(),
                creator == null ? "未知用户" : creator.getDisplayName(),
                entity.getBackground(),
                entity.getDescription(),
                entity.getExpectedResult(),
                entity.getExpectedDeadline(),
                entity.getUrgency(),
                entity.getBudgetAmount(),
                entity.getBudgetDescription(),
                entity.getTechnicalConstraints(),
                canViewContact ? entity.getContactInfo() : null,
                entity.getStatus(),
                entity.getProgress(),
                entity.getVersion(),
                entity.getSubmittedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                historyVOs);
    }
}
