package cn.edu.techgroup.outsourcing.modules.audit.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.modules.audit.entity.AuditLogEntity;
import cn.edu.techgroup.outsourcing.modules.audit.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditRecorderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesAndRedactsSensitiveSnapshots() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        when(mapper.insert(any(AuditLogEntity.class))).thenReturn(1);
        AuditRecorder recorder = new AuditRecorder(
                mapper,
                new AuditDataSanitizer(objectMapper));

        recorder.record(
                1L,
                "UPDATE",
                "USER",
                "2",
                Map.of("role", "MEMBER"),
                Map.of(
                        "role", "ADMIN",
                        "initialPassword", "NeverPersistThis",
                        "nested", Map.of("contactInfo", "13800000000")));

        ArgumentCaptor<AuditLogEntity> captor =
                ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(mapper).insert(captor.capture());
        String afterData = captor.getValue().getAfterData();
        assertTrue(afterData.contains("ADMIN"));
        assertTrue(afterData.contains("[REDACTED]"));
        assertFalse(afterData.contains("NeverPersistThis"));
        assertFalse(afterData.contains("13800000000"));
    }

    @Test
    void insertFailureAbortsManagementWrite() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        when(mapper.insert(any(AuditLogEntity.class))).thenReturn(0);
        AuditRecorder recorder = new AuditRecorder(
                mapper,
                new AuditDataSanitizer(objectMapper));

        assertThrows(
                BusinessException.class,
                () -> recorder.record(1L, "X", "Y", "1", null, Map.of()));
    }

    @Test
    void bestEffortAuditDoesNotReplacePrimaryFailure() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        when(mapper.insert(any(AuditLogEntity.class))).thenReturn(0);
        AuditRecorder recorder = new AuditRecorder(
                mapper,
                new AuditDataSanitizer(objectMapper));

        assertDoesNotThrow(() -> recorder.recordBestEffort(
                null,
                AuditActions.AUTH_LOGIN_FAILED,
                "AUTHENTICATION",
                null,
                null,
                Map.of("outcome", "FAILURE")));
    }
}
