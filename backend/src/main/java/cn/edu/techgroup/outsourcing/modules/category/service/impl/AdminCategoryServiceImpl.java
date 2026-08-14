package cn.edu.techgroup.outsourcing.modules.category.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.category.dto.CreateAdminCategoryCommand;
import cn.edu.techgroup.outsourcing.modules.category.dto.UpdateAdminCategoryCommand;
import cn.edu.techgroup.outsourcing.modules.category.dto.UpdateAdminCategoryStatusCommand;
import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;
import cn.edu.techgroup.outsourcing.modules.category.mapper.CategoryMapper;
import cn.edu.techgroup.outsourcing.modules.category.service.AdminCategoryService;
import cn.edu.techgroup.outsourcing.modules.category.vo.AdminCategoryVO;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private static final int MIN_REASON_LENGTH = 5;
    private static final int MAX_REASON_LENGTH = 500;

    private final CategoryMapper categoryMapper;
    private final AuditRecorder auditRecorder;

    public AdminCategoryServiceImpl(
            CategoryMapper categoryMapper,
            AuditRecorder auditRecorder) {
        this.categoryMapper = categoryMapper;
        this.auditRecorder = auditRecorder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminCategoryVO> list(LoginUser operator) {
        requireAdmin(operator);
        return categoryMapper.selectList(
                        Wrappers.<CategoryEntity>lambdaQuery()
                                .orderByAsc(CategoryEntity::getSortOrder)
                                .orderByAsc(CategoryEntity::getId))
                .stream()
                .map(AdminCategoryVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCategoryVO create(
            CreateAdminCategoryCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validateCreateCommand(command);
        String reason = normalizeReason(command.reason());

        CategoryEntity category = new CategoryEntity();
        category.setName(command.name().trim());
        category.setSortOrder(command.sortOrder());
        category.setEnabled(true);
        category.setCreatedAt(now());
        category.setUpdatedAt(category.getCreatedAt());
        try {
            if (categoryMapper.insert(category) != 1 || category.getId() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        } catch (DuplicateKeyException exception) {
            throw duplicateCategory();
        }

        auditRecorder.record(
                operator.id(),
                "CATEGORY_CREATE",
                "CATEGORY",
                category.getId().toString(),
                null,
                snapshotWithReason(category, reason));
        return AdminCategoryVO.from(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCategoryVO update(
            Long categoryId,
            UpdateAdminCategoryCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validateUpdateCommand(command);
        String reason = normalizeReason(command.reason());
        CategoryEntity category = findForUpdate(categoryId);
        requireExpectedUpdatedAt(category, command.expectedUpdatedAt());
        Map<String, Object> before = snapshot(category);

        category.setName(command.name().trim());
        category.setSortOrder(command.sortOrder());
        category.setUpdatedAt(nextUpdatedAt(category.getUpdatedAt()));
        try {
            if (categoryMapper.updateById(category) != 1) {
                throw updateConflict();
            }
        } catch (DuplicateKeyException exception) {
            throw duplicateCategory();
        }

        auditRecorder.record(
                operator.id(),
                "CATEGORY_UPDATE",
                "CATEGORY",
                categoryId.toString(),
                before,
                snapshotWithReason(category, reason));
        return AdminCategoryVO.from(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCategoryVO updateStatus(
            Long categoryId,
            UpdateAdminCategoryStatusCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validateStatusCommand(command);
        String reason = normalizeReason(command.reason());
        CategoryEntity category = findForUpdate(categoryId);
        requireExpectedUpdatedAt(category, command.expectedUpdatedAt());
        if (Objects.equals(category.getEnabled(), command.enabled())) {
            return AdminCategoryVO.from(category);
        }

        Map<String, Object> before = snapshot(category);
        category.setEnabled(command.enabled());
        category.setUpdatedAt(nextUpdatedAt(category.getUpdatedAt()));
        if (categoryMapper.updateById(category) != 1) {
            throw updateConflict();
        }
        auditRecorder.record(
                operator.id(),
                "CATEGORY_STATUS",
                "CATEGORY",
                categoryId.toString(),
                before,
                snapshotWithReason(category, reason));
        return AdminCategoryVO.from(category);
    }

    private CategoryEntity findForUpdate(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw hiddenCategory();
        }
        CategoryEntity category = categoryMapper.selectByIdForUpdate(categoryId);
        if (category == null) {
            throw hiddenCategory();
        }
        return category;
    }

    private void requireExpectedUpdatedAt(
            CategoryEntity category,
            Instant expectedUpdatedAt) {
        if (expectedUpdatedAt == null
                || category.getUpdatedAt() == null
                || !category.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS)
                        .equals(expectedUpdatedAt.truncatedTo(ChronoUnit.MILLIS))) {
            throw dataVersionConflict();
        }
    }

    private Instant nextUpdatedAt(Instant oldUpdatedAt) {
        Instant current = now();
        if (oldUpdatedAt == null) {
            return current;
        }
        Instant old = oldUpdatedAt.truncatedTo(ChronoUnit.MILLIS);
        return current.isAfter(old) ? current : old.plusMillis(1);
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private Map<String, Object> snapshot(CategoryEntity category) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", category.getName());
        snapshot.put("sortOrder", category.getSortOrder());
        snapshot.put("enabled", category.getEnabled());
        return snapshot;
    }

    private Map<String, Object> snapshotWithReason(
            CategoryEntity category,
            String reason) {
        Map<String, Object> snapshot = snapshot(category);
        snapshot.put("reason", reason);
        return snapshot;
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            throw invalidArgument("操作原因不能为空");
        }
        String normalized = reason.trim();
        if (normalized.length() < MIN_REASON_LENGTH
                || normalized.length() > MAX_REASON_LENGTH) {
            throw invalidArgument("操作原因应为 5～500 个字符");
        }
        return normalized;
    }

    private void validateCreateCommand(CreateAdminCategoryCommand command) {
        if (command == null
                || command.name() == null
                || command.sortOrder() == null) {
            throw invalidArgument("分类参数不正确");
        }
    }

    private void validateUpdateCommand(UpdateAdminCategoryCommand command) {
        if (command == null
                || command.name() == null
                || command.sortOrder() == null) {
            throw invalidArgument("分类参数不正确");
        }
    }

    private void validateStatusCommand(
            UpdateAdminCategoryStatusCommand command) {
        if (command == null || command.enabled() == null) {
            throw invalidArgument("分类状态参数不正确");
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

    private BusinessException invalidArgument(String message) {
        return new BusinessException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private BusinessException hiddenCategory() {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "分类不存在");
    }

    private BusinessException duplicateCategory() {
        return new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "分类名称已存在");
    }

    private BusinessException updateConflict() {
        return new BusinessException(
                ErrorCode.REQUEST_STATUS_CONFLICT,
                "分类已被其他管理员更新，请刷新后重试");
    }

    private BusinessException dataVersionConflict() {
        return new BusinessException(
                ErrorCode.DATA_VERSION_CONFLICT,
                "分类已被其他管理员更新，请刷新后重试");
    }
}
