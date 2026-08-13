package cn.edu.techgroup.outsourcing.modules.file.job;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.modules.file.service.AttachmentService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingAttachmentCleanupJobTest {
    @Mock
    private AttachmentService attachmentService;

    @Test
    void invokesBoundedCleanupBatch() {
        when(attachmentService.cleanupExpiredPending(any(Instant.class), any(Integer.class)))
                .thenReturn(3);
        PendingAttachmentCleanupJob job = new PendingAttachmentCleanupJob(
                attachmentService, Duration.ofHours(24));

        job.cleanup();

        verify(attachmentService).cleanupExpiredPending(any(Instant.class),
                org.mockito.ArgumentMatchers.eq(100));
    }

    @Test
    void rejectsNonPositiveRetention() {
        assertThrows(IllegalArgumentException.class,
                () -> new PendingAttachmentCleanupJob(
                        attachmentService, Duration.ZERO));
    }
}
