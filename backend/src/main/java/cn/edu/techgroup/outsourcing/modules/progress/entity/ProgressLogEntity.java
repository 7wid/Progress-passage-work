package cn.edu.techgroup.outsourcing.modules.progress.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("progress_log")
public class ProgressLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requestId;
    private Long authorId;
    private Integer progress;
    private String content;
    private String nextPlan;
    private Instant nextUpdateAt;
    private Boolean visibleToRequester;
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

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNextPlan() {
        return nextPlan;
    }

    public void setNextPlan(String nextPlan) {
        this.nextPlan = nextPlan;
    }

    public Instant getNextUpdateAt() {
        return nextUpdateAt;
    }

    public void setNextUpdateAt(Instant nextUpdateAt) {
        this.nextUpdateAt = nextUpdateAt;
    }

    public Boolean getVisibleToRequester() {
        return visibleToRequester;
    }

    public void setVisibleToRequester(Boolean visibleToRequester) {
        this.visibleToRequester = visibleToRequester;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
