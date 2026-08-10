package cn.edu.techgroup.outsourcing.modules.evaluation.entity;

import cn.edu.techgroup.outsourcing.modules.evaluation.enums.EvaluationConclusion;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.Instant;

@TableName("evaluation")
public class EvaluationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requestId;
    private Long evaluatorId;
    private EvaluationConclusion conclusion;
    private String publicComment;
    private String solutionSummary;
    private BigDecimal estimatedWorkload;
    private Instant estimatedFinishAt;
    private String requiredSkills;
    private String risks;
    private String internalNote;
    private Integer version;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getEvaluatorId() {
        return evaluatorId;
    }

    public void setEvaluatorId(Long evaluatorId) {
        this.evaluatorId = evaluatorId;
    }

    public EvaluationConclusion getConclusion() {
        return conclusion;
    }

    public void setConclusion(EvaluationConclusion conclusion) {
        this.conclusion = conclusion;
    }

    public String getPublicComment() {
        return publicComment;
    }

    public void setPublicComment(String publicComment) {
        this.publicComment = publicComment;
    }

    public String getSolutionSummary() {
        return solutionSummary;
    }

    public void setSolutionSummary(String solutionSummary) {
        this.solutionSummary = solutionSummary;
    }

    public BigDecimal getEstimatedWorkload() {
        return estimatedWorkload;
    }

    public void setEstimatedWorkload(BigDecimal estimatedWorkload) {
        this.estimatedWorkload = estimatedWorkload;
    }

    public Instant getEstimatedFinishAt() {
        return estimatedFinishAt;
    }

    public void setEstimatedFinishAt(Instant estimatedFinishAt) {
        this.estimatedFinishAt = estimatedFinishAt;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getRisks() {
        return risks;
    }

    public void setRisks(String risks) {
        this.risks = risks;
    }

    public String getInternalNote() {
        return internalNote;
    }

    public void setInternalNote(String internalNote) {
        this.internalNote = internalNote;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}