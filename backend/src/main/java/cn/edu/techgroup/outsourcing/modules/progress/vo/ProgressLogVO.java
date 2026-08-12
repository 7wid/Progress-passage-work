package cn.edu.techgroup.outsourcing.modules.progress.vo;

import java.time.Instant;

public record ProgressLogVO(
        String id,
        String requestId,
        String authorId,
        String authorName,
        int progress,
        String content,
        String nextPlan,
        Instant nextUpdateAt,
        boolean visibleToRequester,
        Instant createdAt) {
}
