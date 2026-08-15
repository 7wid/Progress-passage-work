package cn.edu.techgroup.outsourcing.modules.audit.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record AuditLogVO(
        String id,
        String actorId,
        String actorName,
        String action,
        String targetType,
        String targetId,
        JsonNode beforeData,
        JsonNode afterData,
        String requestId,
        String ipAddress,
        Instant createdAt) {}
