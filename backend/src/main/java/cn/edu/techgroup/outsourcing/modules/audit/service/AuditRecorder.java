package cn.edu.techgroup.outsourcing.modules.audit.service;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.entity.AuditLogEntity;
import cn.edu.techgroup.outsourcing.modules.audit.mapper.AuditLogMapper;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditRecorder.class);

    private final AuditLogMapper auditLogMapper;
    private final AuditDataSanitizer sanitizer;

    public AuditRecorder(
            AuditLogMapper auditLogMapper,
            AuditDataSanitizer sanitizer) {
        this.auditLogMapper = auditLogMapper;
        this.sanitizer = sanitizer;
    }

    public void record(
            Long actorId,
            String action,
            String targetType,
            String targetId,
            Object beforeData,
            Object afterData) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setActorId(actorId);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setBeforeData(sanitizer.toSafeJson(beforeData));
        auditLog.setAfterData(sanitizer.toSafeJson(afterData));
        auditLog.setRequestId(MDC.get("requestId"));
        auditLog.setIpAddress(resolveRemoteAddress());
        auditLog.setCreatedAt(Instant.now());
        if (auditLogMapper.insert(auditLog) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public void recordBestEffort(
            Long actorId,
            String action,
            String targetType,
            String targetId,
            Object beforeData,
            Object afterData) {
        try {
            record(actorId, action, targetType, targetId, beforeData, afterData);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to persist audit event: action={}, targetType={}",
                    action,
                    targetType,
                    exception);
        }
    }

    private String resolveRemoteAddress() {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String address = request.getRemoteAddr();
        if (address == null || address.isBlank()) {
            return null;
        }
        String trimmed = address.trim();
        return trimmed.length() <= 64 ? trimmed : trimmed.substring(0, 64);
    }
}
