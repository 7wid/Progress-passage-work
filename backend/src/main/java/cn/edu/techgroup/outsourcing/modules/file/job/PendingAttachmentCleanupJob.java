package cn.edu.techgroup.outsourcing.modules.file.job;

import cn.edu.techgroup.outsourcing.modules.file.service.AttachmentService;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingAttachmentCleanupJob {
    private static final Logger log =
            LoggerFactory.getLogger(PendingAttachmentCleanupJob.class);
    private static final int BATCH_SIZE = 100;

    private final AttachmentService attachmentService;
    private final Duration retention;

    public PendingAttachmentCleanupJob(
            AttachmentService attachmentService,
            @Value("${app.pending-attachment-retention:24h}") Duration retention) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("Pending attachment retention must be positive");
        }
        this.attachmentService = attachmentService;
        this.retention = retention;
    }

    @Scheduled(initialDelayString = "PT60S", fixedDelayString = "PT1H")
    public void cleanup() {
        int deleted = attachmentService.cleanupExpiredPending(
                Instant.now().minus(retention), BATCH_SIZE);
        if (deleted > 0) {
            log.info("Cleaned {} expired pending delivery attachments", deleted);
        }
    }
}
