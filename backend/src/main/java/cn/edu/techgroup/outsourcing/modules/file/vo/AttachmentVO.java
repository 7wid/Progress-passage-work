package cn.edu.techgroup.outsourcing.modules.file.vo;

import cn.edu.techgroup.outsourcing.modules.file.enums.AttachmentBusinessType;
import java.time.Instant;

public record AttachmentVO(
        String id,
        String requestId,
        AttachmentBusinessType businessType,
        String businessId,
        String originalName,
        String contentType,
        long sizeBytes,
        String uploaderId,
        String uploaderName,
        Instant createdAt,
        boolean canDelete) {}
