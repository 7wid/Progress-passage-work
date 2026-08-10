package cn.edu.techgroup.outsourcing.modules.evaluation.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.evaluation.dto.ConfirmRejectionCommand;
import cn.edu.techgroup.outsourcing.modules.evaluation.dto.CreateEvaluationCommand;
import cn.edu.techgroup.outsourcing.modules.evaluation.entity.EvaluationEntity;
import cn.edu.techgroup.outsourcing.modules.evaluation.enums.EvaluationConclusion;
import cn.edu.techgroup.outsourcing.modules.evaluation.mapper.EvaluationMapper;
import cn.edu.techgroup.outsourcing.modules.evaluation.service.EvaluationService;
import cn.edu.techgroup.outsourcing.modules.evaluation.vo.EvaluationResultVO;
import cn.edu.techgroup.outsourcing.modules.evaluation.vo.EvaluationVO;
import cn.edu.techgroup.outsourcing.modules.evaluation.vo.RejectionConfirmationVO;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.Collection;
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
public class EvaluationServiceImpl implements EvaluationService {

    private static final String WORKLOAD_UNIT_HOURS = "HOURS";

    private final EvaluationMapper evaluationMapper;
    private final RequestMapper requestMapper;
    private final StatusHistoryMapper statusHistoryMapper;
    private final UserMapper userMapper;

    public EvaluationServiceImpl(
            EvaluationMapper evaluationMapper,
            RequestMapper requestMapper,
            StatusHistoryMapper statusHistoryMapper,
            UserMapper userMapper) {
        this.evaluationMapper = evaluationMapper;
        this.requestMapper = requestMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationVO> list(
            Long requestId,
            LoginUser viewer) {

        findVisibleRequest(requestId, viewer);

        List<EvaluationEntity> evaluations = evaluationMapper.selectList(
                Wrappers.<EvaluationEntity>lambdaQuery()
                        .eq(EvaluationEntity::getRequestId, requestId)
                        .orderByDesc(EvaluationEntity::getVersion)
                        .orderByDesc(EvaluationEntity::getId));

        Map<Long, String> evaluatorNames = loadUserNames(
                evaluations.stream()
                        .map(EvaluationEntity::getEvaluatorId)
                        .toList());

        boolean hideInternalNote = viewer.role() == UserRole.REQUESTER;

        return evaluations.stream()
                .map(evaluation -> toVO(
                        evaluation,
                        evaluatorNames.getOrDefault(
                                evaluation.getEvaluatorId(),
                                "未知用户"),
                        hideInternalNote))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationResultVO create(
            Long requestId,
            CreateEvaluationCommand command,
            LoginUser operator) {

        requireEvaluator(operator);

        RequestEntity request = findRequest(requestId);
        validateCreateCommand(request, command);

        RequestStatus oldStatus = request.getStatus();
        RequestStatus targetStatus = targetStatus(
                command.conclusion(),
                operator.role());

        int updatedRows = requestMapper.compareAndSetStatus(
                requestId,
                RequestStatus.PENDING_REVIEW.getValue(),
                targetStatus.getValue(),
                command.requestVersion());

        if (updatedRows != 1) {
            throw statusConflict();
        }

        int evaluationVersion =
                evaluationMapper.selectMaxVersion(requestId) + 1;

        Instant createdAt = Instant.now();
        boolean feasible =
                command.conclusion() == EvaluationConclusion.FEASIBLE;

        EvaluationEntity evaluation = new EvaluationEntity();
        evaluation.setRequestId(requestId);
        evaluation.setEvaluatorId(operator.id());
        evaluation.setConclusion(command.conclusion());
        evaluation.setPublicComment(command.publicComment());

        // 非“可承接”结论不保存上一次残留的方案、工作量和完成时间。
        evaluation.setSolutionSummary(
                feasible ? command.solutionSummary() : null);
        evaluation.setEstimatedWorkload(
                feasible ? command.estimatedWorkload() : null);
        evaluation.setEstimatedFinishAt(
                feasible ? command.estimatedFinishAt() : null);

        evaluation.setRequiredSkills(command.requiredSkills());
        evaluation.setRisks(command.risks());
        evaluation.setInternalNote(command.internalNote());
        evaluation.setVersion(evaluationVersion);
        evaluation.setCreatedAt(createdAt);

        try {
            int insertedRows = evaluationMapper.insert(evaluation);

            if (insertedRows != 1 || evaluation.getId() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        } catch (DuplicateKeyException exception) {
            throw statusConflict();
        }

        if (targetStatus != oldStatus) {
            insertStatusHistory(
                    requestId,
                    operator.id(),
                    oldStatus,
                    targetStatus,
                    historyReason(command.conclusion()));
        }

        boolean confirmationRequired =
                command.conclusion() == EvaluationConclusion.NOT_FEASIBLE
                        && operator.role() == UserRole.MEMBER;

        return new EvaluationResultVO(
                toVO(evaluation, operator.displayName(), false),
                targetStatus,
                command.requestVersion() + 1,
                confirmationRequired);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RejectionConfirmationVO confirmRejection(
            Long requestId,
            Long evaluationId,
            ConfirmRejectionCommand command,
            LoginUser operator) {

        requireAdmin(operator);

        RequestEntity request = findRequest(requestId);
        validatePendingReviewAndVersion(
                request,
                command.requestVersion());

        if (evaluationId == null || evaluationId <= 0) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "评估记录不存在");
        }

        EvaluationEntity evaluation =
                evaluationMapper.selectById(evaluationId);

        if (evaluation == null
                || !Objects.equals(
                        evaluation.getRequestId(),
                        requestId)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "评估记录不存在");
        }

        if (evaluation.getConclusion()
                != EvaluationConclusion.NOT_FEASIBLE) {
            throw new BusinessException(
                    ErrorCode.REQUEST_STATUS_CONFLICT,
                    "该评估结论不需要确认");
        }

        int latestVersion =
                evaluationMapper.selectMaxVersion(requestId);

        if (evaluation.getVersion() == null
                || evaluation.getVersion() != latestVersion) {
            throw new BusinessException(
                    ErrorCode.REQUEST_STATUS_CONFLICT,
                    "只能确认最新一版评估");
        }

        int updatedRows = requestMapper.compareAndSetStatus(
                requestId,
                RequestStatus.PENDING_REVIEW.getValue(),
                RequestStatus.REJECTED.getValue(),
                command.requestVersion());

        if (updatedRows != 1) {
            throw statusConflict();
        }

        insertStatusHistory(
                requestId,
                operator.id(),
                RequestStatus.PENDING_REVIEW,
                RequestStatus.REJECTED,
                "管理员确认暂不承接");

        return new RejectionConfirmationVO(
                requestId.toString(),
                RequestStatus.REJECTED,
                command.requestVersion() + 1);
    }

    private void validateCreateCommand(
            RequestEntity request,
            CreateEvaluationCommand command) {

        if (command == null
                || command.requestVersion() == null
                || command.conclusion() == null
                || !StringUtils.hasText(command.publicComment())
                || command.publicComment().length() < 10
                || command.publicComment().length() > 5000) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "评估说明应为 10～5000 个字符");
        }

        validatePendingReviewAndVersion(
                request,
                command.requestVersion());

        if (command.conclusion()
                != EvaluationConclusion.FEASIBLE) {
            return;
        }

        if (!StringUtils.hasText(command.solutionSummary())
                || command.solutionSummary().length() < 10
                || command.solutionSummary().length() > 5000
                || command.estimatedWorkload() == null
                || command.estimatedWorkload().signum() <= 0
                || command.estimatedFinishAt() == null
                || !command.estimatedFinishAt().isAfter(Instant.now())) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "可承接时必须填写有效的技术方案、"
                            + "预计工作量和预计完成时间");
        }
    }

    private void validatePendingReviewAndVersion(
            RequestEntity request,
            Integer expectedVersion) {

        if (request.getStatus() != RequestStatus.PENDING_REVIEW) {
            throw new BusinessException(
                    ErrorCode.REQUEST_STATUS_CONFLICT,
                    "当前需求不是待评估状态");
        }

        if (expectedVersion == null
                || !Objects.equals(
                        request.getVersion(),
                        expectedVersion)) {
            throw statusConflict();
        }
    }

    private RequestStatus targetStatus(
            EvaluationConclusion conclusion,
            UserRole role) {

        return switch (conclusion) {
            case FEASIBLE -> RequestStatus.PENDING_ASSIGNMENT;
            case NEED_MORE_INFO -> RequestStatus.NEED_MORE_INFO;
            case NOT_FEASIBLE -> role == UserRole.ADMIN
                    ? RequestStatus.REJECTED
                    : RequestStatus.PENDING_REVIEW;
        };
    }

    private String historyReason(
            EvaluationConclusion conclusion) {

        return switch (conclusion) {
            case FEASIBLE -> "评估结论：可承接";
            case NEED_MORE_INFO -> "评估结论：需补充资料";
            case NOT_FEASIBLE -> "管理员评估确认：暂不承接";
        };
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
        history.setReason(reason);
        history.setCreatedAt(Instant.now());

        if (statusHistoryMapper.insert(history) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
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

    private RequestEntity findVisibleRequest(
            Long requestId,
            LoginUser viewer) {

        if (viewer == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        RequestEntity request = findRequest(requestId);

        switch (viewer.role()) {
            case REQUESTER -> {
                if (!Objects.equals(
                        request.getCreatorId(),
                        viewer.id())) {
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
            default -> throw new BusinessException(
                    ErrorCode.ACCESS_DENIED);
        }

        return request;
    }

    private void requireEvaluator(LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        if (operator.role() != UserRole.MEMBER
                && operator.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
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

    private Map<Long, String> loadUserNames(
            Collection<Long> userIds) {

        Set<Long> ids = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        return userMapper.selectList(
                        Wrappers.<UserEntity>lambdaQuery()
                                .select(
                                        UserEntity::getId,
                                        UserEntity::getDisplayName)
                                .in(UserEntity::getId, ids))
                .stream()
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        UserEntity::getDisplayName));
    }

    private EvaluationVO toVO(
            EvaluationEntity evaluation,
            String evaluatorName,
            boolean hideInternalNote) {

        return new EvaluationVO(
                evaluation.getId().toString(),
                evaluation.getRequestId().toString(),
                evaluation.getEvaluatorId().toString(),
                evaluatorName,
                evaluation.getConclusion(),
                evaluation.getPublicComment(),
                evaluation.getSolutionSummary(),
                evaluation.getEstimatedWorkload(),
                evaluation.getEstimatedWorkload() == null
                        ? null
                        : WORKLOAD_UNIT_HOURS,
                evaluation.getEstimatedFinishAt(),
                evaluation.getRequiredSkills(),
                evaluation.getRisks(),
                hideInternalNote
                        ? null
                        : evaluation.getInternalNote(),
                evaluation.getVersion(),
                evaluation.getCreatedAt());
    }

    private BusinessException statusConflict() {
        return new BusinessException(
                ErrorCode.REQUEST_STATUS_CONFLICT,
                "需求已发生变化，请刷新后重试");
    }

    private BusinessException hiddenRequest() {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "需求不存在");
    }
}