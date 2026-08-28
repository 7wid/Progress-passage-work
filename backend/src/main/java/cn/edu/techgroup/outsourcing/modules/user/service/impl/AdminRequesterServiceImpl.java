package cn.edu.techgroup.outsourcing.modules.user.service.impl;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.user.dto.AdminRequesterListQuery;
import cn.edu.techgroup.outsourcing.modules.user.dto.CreateRequesterCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateRequesterStatusCommand;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.event.MemberAccessChangedPublisher;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.modules.user.service.AdminRequesterService;
import cn.edu.techgroup.outsourcing.modules.user.vo.AdminRequesterVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRequesterServiceImpl implements AdminRequesterService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;
    private final MemberAccessChangedPublisher accessChangedPublisher;
    private final Validator validator;

    public AdminRequesterServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
            AuditRecorder auditRecorder, MemberAccessChangedPublisher accessChangedPublisher,
            Validator validator) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
        this.accessChangedPublisher = accessChangedPublisher;
        this.validator = validator;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminRequesterVO> list(AdminRequesterListQuery query, LoginUser operator) {
        requireAdmin(operator);
        validate(query);
        var wrapper = Wrappers.<UserEntity>lambdaQuery()
                .select(UserEntity::getId, UserEntity::getAccount, UserEntity::getDisplayName,
                        UserEntity::getEmail, UserEntity::getPhone, UserEntity::getDepartment,
                        UserEntity::getStatus, UserEntity::getCreatedAt, UserEntity::getUpdatedAt)
                .eq(UserEntity::getRole, UserRole.REQUESTER);
        if (query.keyword() != null) {
            wrapper.and(filter -> filter.like(UserEntity::getAccount, query.keyword())
                    .or().like(UserEntity::getDisplayName, query.keyword()));
        }
        wrapper.eq(query.status() != null, UserEntity::getStatus, query.status())
                .orderByDesc(UserEntity::getCreatedAt).orderByDesc(UserEntity::getId);
        Page<UserEntity> result = userMapper.selectPage(new Page<>(query.page(), query.pageSize()), wrapper);
        return PageResponse.of(result.getRecords().stream().map(AdminRequesterVO::from).toList(),
                result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRequesterVO create(CreateRequesterCommand command, LoginUser operator) {
        requireAdmin(operator);
        validate(command);
        if (command.initialPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "初始密码 UTF-8 编码不能超过 72 字节");
        }
        UserEntity requester = new UserEntity();
        requester.setAccount(command.account());
        requester.setPasswordHash(passwordEncoder.encode(command.initialPassword()));
        requester.setDisplayName(command.displayName());
        requester.setEmail(command.email());
        requester.setPhone(command.phone());
        requester.setDepartment(command.department());
        requester.setRole(UserRole.REQUESTER);
        requester.setStatus(UserStatus.ACTIVE);
        requester.setFailedLoginCount(0);
        requester.setCreatedAt(now());
        requester.setUpdatedAt(requester.getCreatedAt());
        try {
            if (userMapper.insert(requester) != 1 || requester.getId() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "账号或邮箱已存在");
        }
        auditRecorder.record(operator.id(), "REQUESTER_CREATE", "USER",
                requester.getId().toString(), null, snapshotWithReason(requester, command.reason()));
        return AdminRequesterVO.from(requester);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRequesterVO updateStatus(Long requesterId, UpdateRequesterStatusCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validate(command);
        if (requesterId == null || requesterId <= 0) throw hiddenRequester();
        UserEntity requester = userMapper.selectRequesterForUpdate(requesterId);
        if (requester == null || requester.getRole() != UserRole.REQUESTER) throw hiddenRequester();
        if (requester.getUpdatedAt() == null
                || !requester.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS)
                        .equals(command.expectedUpdatedAt().truncatedTo(ChronoUnit.MILLIS))) {
            throw versionConflict();
        }
        if (requester.getStatus() == command.status()) return AdminRequesterVO.from(requester);

        Map<String, Object> before = snapshot(requester);
        requester.setStatus(command.status());
        if (command.status() == UserStatus.ACTIVE) {
            requester.setFailedLoginCount(0);
            requester.setLockedUntil(null);
        }
        Instant previous = requester.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS);
        Instant current = now();
        requester.setUpdatedAt(current.isAfter(previous) ? current : previous.plusMillis(1));
        if (userMapper.updateAdminStatus(requester) != 1) throw versionConflict();
        auditRecorder.record(operator.id(), "REQUESTER_STATUS", "USER", requesterId.toString(),
                before, snapshotWithReason(requester, command.reason()));
        // Existing account-based event invalidates JDBC sessions only after transaction commit.
        // ActiveSessionValidationFilter also rejects disabled accounts if session deletion fails.
        accessChangedPublisher.publish(requester.getAccount());
        return AdminRequesterVO.from(requester);
    }

    private void requireAdmin(LoginUser operator) {
        if (operator == null) throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        if (operator.role() != UserRole.ADMIN) throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private void validate(Object value) {
        if (value == null || !validator.validate(value).isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "请检查需求方账号参数");
        }
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private Map<String, Object> snapshot(UserEntity requester) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("account", requester.getAccount());
        result.put("displayName", requester.getDisplayName());
        result.put("role", requester.getRole());
        result.put("status", requester.getStatus());
        return result;
    }

    private Map<String, Object> snapshotWithReason(UserEntity requester, String reason) {
        Map<String, Object> result = snapshot(requester);
        result.put("reason", reason);
        return result;
    }

    private BusinessException hiddenRequester() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "需求方账号不存在");
    }

    private BusinessException versionConflict() {
        return new BusinessException(ErrorCode.DATA_VERSION_CONFLICT, "需求方账号已更新，请刷新后重试");
    }
}
