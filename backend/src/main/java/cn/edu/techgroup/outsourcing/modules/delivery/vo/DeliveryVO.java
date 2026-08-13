package cn.edu.techgroup.outsourcing.modules.delivery.vo;

import java.time.Instant;
import java.util.List;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentVO;

public record DeliveryVO(
        String id,
        String requestId,
        String submitterId,
        String submitterName,
        String description,
        String deliveryUrl,
        List<AttachmentVO> attachments,
        Instant createdAt) {
}
