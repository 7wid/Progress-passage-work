package cn.edu.techgroup.outsourcing.modules.request.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditActions;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;
import cn.edu.techgroup.outsourcing.modules.category.mapper.CategoryMapper;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEvents;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.dto.CancelRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.SaveDraftCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.SubmitRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.UpdateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestRevisionEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestRevisionMapper;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestNumberGenerator;
import cn.edu.techgroup.outsourcing.modules.request.service.RequesterRequestLifecycleService;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestMutationVO;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequesterRequestLifecycleServiceImpl
        implements RequesterRequestLifecycleService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<RequestStatus> EDITABLE_STATUSES = Set.of(
            RequestStatus.DRAFT,
            RequestStatus.NEED_MORE_INFO);
    private static final Set<RequestStatus> CANCELABLE_STATUSES = Set.of(
            RequestStatus.DRAFT,
            RequestStatus.PENDING_REVIEW,
            RequestStatus.NEED_MORE_INFO,
            RequestStatus.PENDING_ASSIGNMENT);

    private final RequestMapper requestMapper;
    private final RequestRevisionMapper requestRevisionMapper;
    private final CategoryMapper categoryMapper;
    private final StatusHistoryMapper statusHistoryMapper;
    private final RequestNumberGenerator requestNumberGenerator;
    private final UserMapper userMapper;
    private final NotificationEventPublisher notificationEventPublisher;
    private final AuditRecorder auditRecorder;
    private final ObjectMapper objectMapper;

    public RequesterRequestLifecycleServiceImpl(
            RequestMapper requestMapper,
            RequestRevisionMapper requestRevisionMapper,
            CategoryMapper categoryMapper,
            StatusHistoryMapper statusHistoryMapper,
            RequestNumberGenerator requestNumberGenerator,
            UserMapper userMapper,
            NotificationEventPublisher notificationEventPublisher,
            AuditRecorder auditRecorder,
            ObjectMapper objectMapper) {
        this.requestMapper = requestMapper;
        this.requestRevisionMapper = requestRevisionMapper;
        this.categoryMapper = categoryMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.requestNumberGenerator = requestNumberGenerator;
        this.userMapper = userMapper;
        this.notificationEventPublisher = notificationEventPublisher;
        this.auditRecorder = auditRecorder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatedRequestVO createDraft(
            SaveDraftCommand command,
            LoginUser operator) {
        requireRequester(operator);
        if (command == null) {
            throw invalidArgument("草稿内容不能为空");
        }
        validateOptionalContent(command.categoryId(), command.expectedDeadline());

        RequestEntity request = new RequestEntity();
        request.setCreatorId(operator.id());
        applyContent(request, command);
        request.setStatus(RequestStatus.DRAFT);
        request.setProgress(0);
        request.setVersion(0);

        if (requestMapper.insert(request) != 1 || request.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        insertStatusHistory(request, operator.id(), null, RequestStatus.DRAFT, "需求草稿已创建");
        saveRevision(request, operator.id(), "创建需求草稿");
        auditRecorder.record(
                operator.id(),
                AuditActions.REQUEST_DRAFT_SAVED,
                "REQUEST",
                request.getId().toString(),
                null,
                snapshot(request));
        return CreatedRequestVO.from(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RequestMutationVO update(
            Long requestId,
            UpdateRequestCommand command,
            LoginUser operator) {
        requireRequester(operator);
        if (command == null || command.expectedVersion() == null) {
            throw invalidArgument("需求内容或版本不能为空");
        }

        RequestEntity request = findOwnedForUpdate(requestId, operator);
        requireVersion(request, command.expectedVersion());
        if (!EDITABLE_STATUSES.contains(request.getStatus())) {
            throw statusConflict("当前需求状态不允许修改内容");
        }

        Map<String, Object> before = snapshot(request);
        applyContent(request, command);
        validateOptionalContent(request.getCategoryId(), request.getExpectedDeadline());
        if (request.getStatus() == RequestStatus.NEED_MORE_INFO) {
            validateComplete(request);
        }

        if (requestMapper.updateRequesterEditableFields(request, command.expectedVersion()) != 1) {
            throw statusConflict("需求已被其他操作更新，请刷新后重试");
        }
        request.setVersion(command.expectedVersion() + 1);

        String reason = request.getStatus() == RequestStatus.DRAFT
                ? "保存需求草稿"
                : "补充需求资料";
        saveRevision(request, operator.id(), reason);
        auditRecorder.record(
                operator.id(),
                AuditActions.REQUEST_CONTENT_UPDATED,
                "REQUEST",
                request.getId().toString(),
                before,
                snapshot(request));
        return toMutation(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RequestMutationVO submit(
            Long requestId,
            SubmitRequestCommand command,
            LoginUser operator) {
        requireRequester(operator);
        if (command == null
                || command.expectedVersion() == null
                || !Boolean.TRUE.equals(command.informationConfirmed())) {
            throw invalidArgument("提交前必须确认信息真实有效");
        }

        RequestEntity request = findOwnedForUpdate(requestId, operator);
        requireVersion(request, command.expectedVersion());
        if (!EDITABLE_STATUSES.contains(request.getStatus())) {
            throw statusConflict("当前需求状态不允许提交");
        }
        validateComplete(request);

        RequestStatus oldStatus = request.getStatus();
        Instant submittedAt = Instant.now();
        String requestNo = request.getRequestNo();
        if (requestNo == null) {
            requestNo = requestNumberGenerator.generate(request.getId(), submittedAt);
        }

        if (requestMapper.submitRequesterRequest(
                request.getId(),
                oldStatus.getValue(),
                command.expectedVersion(),
                requestNo,
                submittedAt) != 1) {
            throw statusConflict("需求已被其他操作更新，请刷新后重试");
        }

        request.setRequestNo(requestNo);
        request.setStatus(RequestStatus.PENDING_REVIEW);
        request.setSubmittedAt(request.getSubmittedAt() == null
                ? submittedAt
                : request.getSubmittedAt());
        request.setVersion(command.expectedVersion() + 1);
        String reason = oldStatus == RequestStatus.DRAFT
                ? "需求方提交需求"
                : "需求方补充资料并重新提交";
        insertStatusHistory(
                request,
                operator.id(),
                oldStatus,
                RequestStatus.PENDING_REVIEW,
                reason);
        auditRecorder.record(
                operator.id(),
                AuditActions.REQUEST_SUBMITTED,
                "REQUEST",
                request.getId().toString(),
                Map.of("status", oldStatus, "version", command.expectedVersion()),
                Map.of(
                        "requestNo", requestNo,
                        "status", RequestStatus.PENDING_REVIEW,
                        "version", request.getVersion()));
        notificationEventPublisher.publish(
                NotificationEvents.requestSubmitted(
                        request.getId(), requestNo, operator.id(), List.of()));
        return toMutation(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RequestMutationVO cancel(
            Long requestId,
            CancelRequestCommand command,
            LoginUser operator) {
        requireRequester(operator);
        if (command == null
                || command.expectedVersion() == null
                || command.reason() == null) {
            throw invalidArgument("取消参数不正确");
        }

        RequestEntity request = findOwnedForUpdate(requestId, operator);
        requireVersion(request, command.expectedVersion());
        if (!CANCELABLE_STATUSES.contains(request.getStatus())) {
            throw statusConflict("当前需求状态不允许取消");
        }

        RequestStatus oldStatus = request.getStatus();
        if (requestMapper.compareAndSetStatus(
                request.getId(),
                oldStatus.getValue(),
                RequestStatus.CANCELLED.getValue(),
                command.expectedVersion()) != 1) {
            throw statusConflict("需求已被其他操作更新，请刷新后重试");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setVersion(command.expectedVersion() + 1);
        insertStatusHistory(
                request,
                operator.id(),
                oldStatus,
                RequestStatus.CANCELLED,
                "需求方取消：" + command.reason());
        auditRecorder.record(
                operator.id(),
                AuditActions.REQUEST_CANCELLED_BY_REQUESTER,
                "REQUEST",
                request.getId().toString(),
                Map.of("status", oldStatus, "version", command.expectedVersion()),
                Map.of(
                        "status", RequestStatus.CANCELLED,
                        "version", request.getVersion(),
                        "reason", command.reason()));
        if (oldStatus != RequestStatus.DRAFT) {
            notificationEventPublisher.publish(
                    NotificationEvents.requestCancelled(
                            request.getId(),
                            request.getRequestNo(),
                            operator.id(),
                            userMapper.selectActiveTeamUserIds()));
        }
        return toMutation(request);
    }

    private void applyContent(RequestEntity request, SaveDraftCommand command) {
        request.setCategoryId(command.categoryId());
        request.setTitle(command.title());
        request.setBackground(command.background());
        request.setDescription(command.description());
        request.setExpectedResult(command.expectedResult());
        request.setExpectedDeadline(command.expectedDeadline());
        request.setUrgency(command.urgency());
        request.setBudgetAmount(command.budgetAmount());
        request.setBudgetDescription(command.budgetDescription());
        request.setTechnicalConstraints(command.technicalConstraints());
        request.setContactInfo(command.contactInfo());
    }

    private void applyContent(RequestEntity request, UpdateRequestCommand command) {
        request.setCategoryId(command.categoryId());
        request.setTitle(command.title());
        request.setBackground(command.background());
        request.setDescription(command.description());
        request.setExpectedResult(command.expectedResult());
        request.setExpectedDeadline(command.expectedDeadline());
        request.setUrgency(command.urgency());
        request.setBudgetAmount(command.budgetAmount());
        request.setBudgetDescription(command.budgetDescription());
        request.setTechnicalConstraints(command.technicalConstraints());
        request.setContactInfo(command.contactInfo());
    }

    private void validateOptionalContent(Long categoryId, LocalDate expectedDeadline) {
        if (categoryId != null) {
            CategoryEntity category = categoryMapper.selectById(categoryId);
            if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
                throw invalidArgument("需求分类不存在或已经停用");
            }
        }
        if (expectedDeadline != null
                && expectedDeadline.isBefore(LocalDate.now(BUSINESS_ZONE))) {
            throw invalidArgument("期望完成日期不能早于今天");
        }
    }

    private void validateComplete(RequestEntity request) {
        validateOptionalContent(request.getCategoryId(), request.getExpectedDeadline());
        if (request.getCategoryId() == null
                || !hasLength(request.getTitle(), 5, 80)
                || !hasLength(request.getBackground(), 20, 1000)
                || !hasLength(request.getDescription(), 50, 5000)
                || !hasLength(request.getExpectedResult(), 5, 3000)
                || request.getExpectedDeadline() == null
                || request.getUrgency() == null
                || !hasLength(request.getContactInfo(), 1, 255)) {
            throw invalidArgument("需求信息不完整，请补齐必填内容后再提交");
        }
    }

    private boolean hasLength(String value, int min, int max) {
        return value != null && value.length() >= min && value.length() <= max;
    }

    private RequestEntity findOwnedForUpdate(Long requestId, LoginUser operator) {
        if (requestId == null || requestId <= 0) {
            throw hiddenRequest();
        }
        RequestEntity request = requestMapper.selectByIdForUpdate(requestId);
        if (request == null || !Objects.equals(request.getCreatorId(), operator.id())) {
            throw hiddenRequest();
        }
        return request;
    }

    private void requireVersion(RequestEntity request, Integer expectedVersion) {
        if (!Objects.equals(request.getVersion(), expectedVersion)) {
            throw statusConflict("需求版本已变化，请刷新后重试");
        }
    }

    private void insertStatusHistory(
            RequestEntity request,
            Long operatorId,
            RequestStatus fromStatus,
            RequestStatus toStatus,
            String reason) {
        StatusHistoryEntity history = new StatusHistoryEntity();
        history.setRequestId(request.getId());
        history.setOperatorId(operatorId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setReason(reason);
        history.setCreatedAt(Instant.now());
        if (statusHistoryMapper.insert(history) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void saveRevision(RequestEntity request, Long operatorId, String reason) {
        RequestRevisionEntity revision = new RequestRevisionEntity();
        revision.setRequestId(request.getId());
        revision.setRevisionNo(requestRevisionMapper.selectMaxRevisionNo(request.getId()) + 1);
        try {
            revision.setContentSnapshot(objectMapper.writeValueAsString(snapshot(request)));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        revision.setChangeReason(reason);
        revision.setOperatorId(operatorId);
        revision.setCreatedAt(Instant.now());
        if (requestRevisionMapper.insert(revision) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private Map<String, Object> snapshot(RequestEntity request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("categoryId", request.getCategoryId());
        snapshot.put("title", request.getTitle());
        snapshot.put("background", request.getBackground());
        snapshot.put("description", request.getDescription());
        snapshot.put("expectedResult", request.getExpectedResult());
        snapshot.put("expectedDeadline", request.getExpectedDeadline());
        snapshot.put("urgency", request.getUrgency());
        snapshot.put("budgetAmount", request.getBudgetAmount());
        snapshot.put("budgetDescription", request.getBudgetDescription());
        snapshot.put("technicalConstraints", request.getTechnicalConstraints());
        snapshot.put("contactInfo", request.getContactInfo());
        snapshot.put("status", request.getStatus());
        snapshot.put("version", request.getVersion());
        return snapshot;
    }

    private RequestMutationVO toMutation(RequestEntity request) {
        return new RequestMutationVO(
                request.getId().toString(),
                request.getRequestNo(),
                request.getStatus(),
                request.getVersion());
    }

    private void requireRequester(LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (operator.role() != UserRole.REQUESTER && operator.role() != UserRole.ADMIN) {
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
