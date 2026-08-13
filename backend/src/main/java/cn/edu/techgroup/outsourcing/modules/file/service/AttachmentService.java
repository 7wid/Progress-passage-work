package cn.edu.techgroup.outsourcing.modules.file.service;

import cn.edu.techgroup.outsourcing.modules.file.enums.AttachmentBusinessType;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentSnapshotVO;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentVO;
import cn.edu.techgroup.outsourcing.modules.file.vo.FileDownload;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.util.List;
import java.time.Instant;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {
    AttachmentVO upload(Long requestId, AttachmentBusinessType businessType,
            MultipartFile file, LoginUser operator);
    AttachmentSnapshotVO list(Long requestId, AttachmentBusinessType businessType,
            boolean pendingOnly, LoginUser viewer);
    void deletePending(Long requestId, Long attachmentId, LoginUser operator);
    FileDownload download(Long attachmentId, LoginUser viewer);
    List<AttachmentVO> findBoundDeliveryAttachments(Long requestId, Long deliveryId,
            LoginUser viewer);
    List<AttachmentVO> bindPendingDeliveryAttachments(Long requestId, Long deliveryId,
            List<Long> attachmentIds, LoginUser operator);
    int cleanupExpiredPending(Instant cutoff, int limit);
}
