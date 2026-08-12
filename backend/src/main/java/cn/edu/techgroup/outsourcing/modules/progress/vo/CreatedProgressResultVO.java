package cn.edu.techgroup.outsourcing.modules.progress.vo;

public record CreatedProgressResultVO(
        ProgressLogVO log,
        int currentProgress,
        int requestVersion) {
}
