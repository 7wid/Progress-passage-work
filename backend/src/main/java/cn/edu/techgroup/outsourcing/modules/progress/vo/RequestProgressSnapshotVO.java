package cn.edu.techgroup.outsourcing.modules.progress.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import java.time.Instant;
import java.util.List;

public record RequestProgressSnapshotVO(
        String requestId,
        RequestStatus requestStatus,
        int requestVersion,
        int currentProgress,
        Instant lastProgressAt,
        Instant nextUpdateAt,
        boolean needsFollowUp,
        boolean canUpdateProgress,
        List<ProgressLogVO> logs) {
}
