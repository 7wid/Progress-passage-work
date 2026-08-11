package cn.edu.techgroup.outsourcing.modules.evaluation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.evaluation.dto.ConfirmRejectionCommand;
import cn.edu.techgroup.outsourcing.modules.evaluation.dto.CreateEvaluationCommand;
import cn.edu.techgroup.outsourcing.modules.evaluation.entity.EvaluationEntity;
import cn.edu.techgroup.outsourcing.modules.evaluation.enums.EvaluationConclusion;
import cn.edu.techgroup.outsourcing.modules.evaluation.mapper.EvaluationMapper;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    private static final Long REQUEST_ID = 100L;
    @BeforeAll
static void initializeMybatisMetadata() {
    TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(
                    new MybatisConfiguration(),
                    ""),
            UserEntity.class);
}
    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private RequestMapper requestMapper;

    @Mock
    private StatusHistoryMapper statusHistoryMapper;

    @Mock
    private UserMapper userMapper;

    private EvaluationServiceImpl evaluationService;





    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationServiceImpl(
                evaluationMapper,
                requestMapper,
                statusHistoryMapper,
                userMapper);
    }

    @Test
    void feasibleMovesRequestToPendingAssignment() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(RequestStatus.PENDING_REVIEW, 0));

        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_REVIEW",
                "PENDING_ASSIGNMENT",
                0))
                .thenReturn(1);

        when(evaluationMapper.selectMaxVersion(REQUEST_ID))
                .thenReturn(0);

        stubEvaluationInsert();

        when(statusHistoryMapper.insert(
                any(StatusHistoryEntity.class)))
                .thenReturn(1);

        EvaluationResultVO result = evaluationService.create(
                REQUEST_ID,
                command(0, EvaluationConclusion.FEASIBLE),
                loginUser(2L, UserRole.MEMBER));

        assertSame(
                RequestStatus.PENDING_ASSIGNMENT,
                result.requestStatus());

        assertEquals(1, result.requestVersion());
        assertEquals(1, result.evaluation().version());
    }

    @Test
    void needMoreInfoMovesRequestToNeedMoreInfo() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(RequestStatus.PENDING_REVIEW, 0));

        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_REVIEW",
                "NEED_MORE_INFO",
                0))
                .thenReturn(1);

        when(evaluationMapper.selectMaxVersion(REQUEST_ID))
                .thenReturn(0);

        stubEvaluationInsert();

        when(statusHistoryMapper.insert(
                any(StatusHistoryEntity.class)))
                .thenReturn(1);

        EvaluationResultVO result = evaluationService.create(
                REQUEST_ID,
                command(
                        0,
                        EvaluationConclusion.NEED_MORE_INFO),
                loginUser(2L, UserRole.MEMBER));

        assertSame(
                RequestStatus.NEED_MORE_INFO,
                result.requestStatus());
    }

    @Test
    void memberNotFeasibleWaitsForAdmin() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(RequestStatus.PENDING_REVIEW, 0));

        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_REVIEW",
                "PENDING_REVIEW",
                0))
                .thenReturn(1);

        when(evaluationMapper.selectMaxVersion(REQUEST_ID))
                .thenReturn(0);

        stubEvaluationInsert();

        EvaluationResultVO result = evaluationService.create(
                REQUEST_ID,
                command(
                        0,
                        EvaluationConclusion.NOT_FEASIBLE),
                loginUser(2L, UserRole.MEMBER));

        assertSame(
                RequestStatus.PENDING_REVIEW,
                result.requestStatus());

        assertTrue(result.adminConfirmationRequired());

        verify(
                statusHistoryMapper,
                never())
                .insert(any(StatusHistoryEntity.class));
    }

    @Test
    void staleVersionIsRejected() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(RequestStatus.PENDING_REVIEW, 2));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> evaluationService.create(
                        REQUEST_ID,
                        command(
                                1,
                                EvaluationConclusion.FEASIBLE),
                        loginUser(2L, UserRole.MEMBER)));

        assertSame(
                ErrorCode.REQUEST_STATUS_CONFLICT,
                exception.getErrorCode());

        verifyNoInteractions(
                evaluationMapper,
                statusHistoryMapper);
    }

    @Test
    void requesterCannotCreateEvaluation() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> evaluationService.create(
                        REQUEST_ID,
                        command(
                                0,
                                EvaluationConclusion.FEASIBLE),
                        loginUser(
                                1L,
                                UserRole.REQUESTER)));

        assertSame(
                ErrorCode.ACCESS_DENIED,
                exception.getErrorCode());

        verifyNoInteractions(
                requestMapper,
                evaluationMapper,
                statusHistoryMapper,
                userMapper);
    }

    @Test
    void requesterCannotSeeInternalNote() {
        RequestEntity request =
                request(RequestStatus.PENDING_REVIEW, 0);
        request.setCreatorId(1L);

        EvaluationEntity evaluation = new EvaluationEntity();
        evaluation.setId(10L);
        evaluation.setRequestId(REQUEST_ID);
        evaluation.setEvaluatorId(2L);
        evaluation.setConclusion(
                EvaluationConclusion.NEED_MORE_INFO);
        evaluation.setPublicComment(
                "这是需求方可以看到的完整评估说明");
        evaluation.setInternalNote("仅技术组可见");
        evaluation.setVersion(1);
        evaluation.setCreatedAt(
                Instant.parse("2026-08-10T08:00:00Z"));

        UserEntity evaluator = new UserEntity();
        evaluator.setId(2L);
        evaluator.setDisplayName("评估成员");

        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request);

        when(evaluationMapper.selectList(any()))
                .thenReturn(List.of(evaluation));

        when(userMapper.selectList(any()))
                .thenReturn(List.of(evaluator));

        List<EvaluationVO> result = evaluationService.list(
                REQUEST_ID,
                loginUser(1L, UserRole.REQUESTER));

        assertEquals(1, result.size());
        assertNull(result.get(0).internalNote());
    }

    @Test
    void adminCanConfirmLatestRejection() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(RequestStatus.PENDING_REVIEW, 1));

        EvaluationEntity evaluation = new EvaluationEntity();
        evaluation.setId(10L);
        evaluation.setRequestId(REQUEST_ID);
        evaluation.setEvaluatorId(2L);
        evaluation.setConclusion(
                EvaluationConclusion.NOT_FEASIBLE);
        evaluation.setPublicComment(
                "这是暂时无法承接该需求的完整原因");
        evaluation.setVersion(1);
        evaluation.setCreatedAt(
                Instant.parse("2026-08-10T08:00:00Z"));

        when(evaluationMapper.selectById(10L))
                .thenReturn(evaluation);

        when(evaluationMapper.selectMaxVersion(REQUEST_ID))
                .thenReturn(1);

        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_REVIEW",
                "REJECTED",
                1))
                .thenReturn(1);

        when(statusHistoryMapper.insert(
                any(StatusHistoryEntity.class)))
                .thenReturn(1);

        RejectionConfirmationVO result =
                evaluationService.confirmRejection(
                        REQUEST_ID,
                        10L,
                        new ConfirmRejectionCommand(1),
                        loginUser(3L, UserRole.ADMIN));

        assertSame(
                RequestStatus.REJECTED,
                result.requestStatus());

        assertEquals(2, result.requestVersion());
    }

    private void stubEvaluationInsert() {
        when(evaluationMapper.insert(
                any(EvaluationEntity.class)))
                .thenAnswer(invocation -> {
                    EvaluationEntity entity =
                            invocation.getArgument(0);
                    entity.setId(10L);
                    return 1;
                });
    }

    private CreateEvaluationCommand command(
            int version,
            EvaluationConclusion conclusion) {

        boolean feasible =
                conclusion == EvaluationConclusion.FEASIBLE;

        return new CreateEvaluationCommand(
                version,
                conclusion,
                "这是用于自动化测试的完整评估说明内容",
                feasible
                        ? "采用 Spring Boot 和 Vue 实现完整技术方案"
                        : null,
                feasible
                        ? new BigDecimal("16.00")
                        : null,
                feasible
                        ? Instant.parse(
                                "2030-09-01T08:00:00Z")
                        : null,
                "Java,Vue,MySQL",
                null,
                "内部测试备注");
    }

    private RequestEntity request(
            RequestStatus status,
            int version) {

        RequestEntity request = new RequestEntity();
        request.setId(REQUEST_ID);
        request.setCreatorId(1L);
        request.setStatus(status);
        request.setVersion(version);
        return request;
    }

    private LoginUser loginUser(
            Long id,
            UserRole role) {

        return new LoginUser(
                id,
                "test-user",
                "password-hash",
                "测试用户",
                role,
                true,
                true);
    }
}