package cn.edu.techgroup.outsourcing.modules.evaluation.vo;

import cn.edu.techgroup.outsourcing.modules.evaluation.enums.EvaluationConclusion;
import java.math.BigDecimal;
import java.time.Instant;

public record EvaluationVO(
        String id,
        String requestId,
        String evaluatorId,
        String evaluatorName,
        EvaluationConclusion conclusion,
        String publicComment,
        String solutionSummary,
        BigDecimal estimatedWorkload,
        String workloadUnit,
        Instant estimatedFinishAt,
        String requiredSkills,
        String risks,
        String internalNote,
        int version,
        Instant createdAt) {
}