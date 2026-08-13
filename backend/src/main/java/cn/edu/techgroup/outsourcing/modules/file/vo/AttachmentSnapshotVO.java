package cn.edu.techgroup.outsourcing.modules.file.vo;

import cn.edu.techgroup.outsourcing.modules.file.enums.AttachmentBusinessType;
import java.util.List;

public record AttachmentSnapshotVO(
        String requestId,
        AttachmentBusinessType businessType,
        boolean canUpload,
        List<AttachmentVO> attachments) {}
