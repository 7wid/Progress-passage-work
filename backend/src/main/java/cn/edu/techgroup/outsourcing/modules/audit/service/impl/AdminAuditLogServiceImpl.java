package cn.edu.techgroup.outsourcing.modules.audit.service.impl;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.dto.AdminAuditLogQuery;
import cn.edu.techgroup.outsourcing.modules.audit.entity.AuditLogEntity;
import cn.edu.techgroup.outsourcing.modules.audit.mapper.AuditLogMapper;
import cn.edu.techgroup.outsourcing.modules.audit.service.AdminAuditLogService;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditDataSanitizer;
import cn.edu.techgroup.outsourcing.modules.audit.vo.AuditLogVO;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long MAX_RANGE_DAYS = 366;

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;
    private final AuditDataSanitizer sanitizer;

    public AdminAuditLogServiceImpl(
            AuditLogMapper auditLogMapper,
            UserMapper userMapper,
            AuditDataSanitizer sanitizer) {
        this.auditLogMapper = auditLogMapper;
        this.userMapper = userMapper;
        this.sanitizer = sanitizer;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogVO> list(
            AdminAuditLogQuery query,
            LoginUser operator) {
        requireAdmin(operator);
        validateQuery(query);

        var wrapper = Wrappers.<AuditLogEntity>lambdaQuery()
                .eq(query.actorId() != null, AuditLogEntity::getActorId, query.actorId())
                .eq(query.action() != null, AuditLogEntity::getAction, query.action())
                .eq(query.targetType() != null, AuditLogEntity::getTargetType, query.targetType())
                .eq(query.targetId() != null, AuditLogEntity::getTargetId, query.targetId())
                .eq(query.requestId() != null, AuditLogEntity::getRequestId, query.requestId())
                .ge(query.from() != null,
                        AuditLogEntity::getCreatedAt,
                        query.from() == null
                                ? null
                                : query.from().atStartOfDay(BUSINESS_ZONE).toInstant())
                .lt(query.to() != null,
                        AuditLogEntity::getCreatedAt,
                        query.to() == null
                                ? null
                                : query.to().plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant())
                .orderByDesc(AuditLogEntity::getCreatedAt)
                .orderByDesc(AuditLogEntity::getId);

        Page<AuditLogEntity> result = auditLogMapper.selectPage(
                new Page<>(query.page(), query.pageSize()),
                wrapper);
        Map<Long, String> actorNames = loadActorNames(result);
        var items = result.getRecords().stream()
                .map(entity -> toVO(entity, actorNames))
                .toList();
        return PageResponse.of(
                items,
                result.getCurrent(),
                result.getSize(),
                result.getTotal());
    }

    private Map<Long, String> loadActorNames(Page<AuditLogEntity> result) {
        Set<Long> actorIds = result.getRecords().stream()
                .map(AuditLogEntity::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (actorIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(actorIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getDisplayName));
    }

    private AuditLogVO toVO(
            AuditLogEntity entity,
            Map<Long, String> actorNames) {
        String actorName = entity.getActorId() == null
                ? "系统"
                : actorNames.getOrDefault(entity.getActorId(), "未知用户");
        return new AuditLogVO(
                entity.getId().toString(),
                entity.getActorId() == null ? null : entity.getActorId().toString(),
                actorName,
                entity.getAction(),
                entity.getTargetType(),
                entity.getTargetId(),
                sanitizer.parseSafeJson(entity.getBeforeData()),
                sanitizer.parseSafeJson(entity.getAfterData()),
                entity.getRequestId(),
                maskIpAddress(entity.getIpAddress()),
                entity.getCreatedAt());
    }

    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        String value = ipAddress.trim();
        int lastDot = value.lastIndexOf('.');
        if (lastDot > 0) {
            return value.substring(0, lastDot + 1) + "*";
        }
        int lastColon = value.lastIndexOf(':');
        if (lastColon > 0) {
            return value.substring(0, lastColon + 1) + "*";
        }
        return "*";
    }

    private void validateQuery(AdminAuditLogQuery query) {
        if (query == null
                || query.page() == null
                || query.pageSize() == null
                || query.page() < 1
                || query.pageSize() < 1
                || query.pageSize() > 100) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "分页参数不正确");
        }
        if (query.from() != null
                && query.to() != null
                && (query.from().isAfter(query.to())
                        || ChronoUnit.DAYS.between(query.from(), query.to()) > MAX_RANGE_DAYS)) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "审计查询日期范围不正确或超过 366 天");
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
}
