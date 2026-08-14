package cn.edu.techgroup.outsourcing.modules.audit.service;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.entity.AuditLogEntity;
import cn.edu.techgroup.outsourcing.modules.audit.mapper.AuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class AuditRecorder {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditRecorder(
            AuditLogMapper auditLogMapper,
            ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
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
        auditLog.setBeforeData(toJson(beforeData));
        auditLog.setAfterData(toJson(afterData));
        auditLog.setRequestId(MDC.get("requestId"));
        auditLog.setCreatedAt(Instant.now());
        if (auditLogMapper.insert(auditLog) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
