package cn.edu.techgroup.outsourcing.modules.request.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;
import cn.edu.techgroup.outsourcing.modules.category.mapper.CategoryMapper;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.dto.CreateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestNumberGenerator;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestService;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestServiceImpl implements RequestService {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Shanghai");

    private final RequestMapper requestMapper;
    private final CategoryMapper categoryMapper;
    private final StatusHistoryMapper statusHistoryMapper;
    private final RequestNumberGenerator requestNumberGenerator;

    public RequestServiceImpl(
            RequestMapper requestMapper,
            CategoryMapper categoryMapper,
            StatusHistoryMapper statusHistoryMapper,
            RequestNumberGenerator requestNumberGenerator) {
        this.requestMapper = requestMapper;
        this.categoryMapper = categoryMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.requestNumberGenerator = requestNumberGenerator;
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

        return CreatedRequestVO.from(entity);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}