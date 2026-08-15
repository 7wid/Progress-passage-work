package cn.edu.techgroup.outsourcing.modules.audit.service;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AuditDataSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final Set<String> EXACT_SENSITIVE_KEYS = Set.of(
            "email",
            "phone",
            "contactinfo",
            "authorization",
            "cookie",
            "setcookie");

    private final ObjectMapper objectMapper;

    public AuditDataSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toSafeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sanitize(objectMapper.valueToTree(value)));
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public JsonNode parseSafeJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return sanitize(objectMapper.readTree(value));
        } catch (JsonProcessingException exception) {
            return objectMapper.getNodeFactory().textNode("[INVALID_AUDIT_JSON]");
        }
    }

    private JsonNode sanitize(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            for (var field : object.properties()) {
                if (isSensitive(field.getKey())) {
                    object.put(field.getKey(), REDACTED);
                } else {
                    sanitize(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::sanitize);
        }
        return node;
    }

    private boolean isSensitive(String key) {
        String normalized = key.replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        return EXACT_SENSITIVE_KEYS.contains(normalized)
                || normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("credential");
    }
}
