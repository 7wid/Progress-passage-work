package cn.edu.techgroup.outsourcing.modules.request.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("request_revision")
public class RequestRevisionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requestId;
    private Integer revisionNo;
    private String contentSnapshot;
    private String changeReason;
    private Long operatorId;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Integer getRevisionNo() { return revisionNo; }
    public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }
    public String getContentSnapshot() { return contentSnapshot; }
    public void setContentSnapshot(String contentSnapshot) { this.contentSnapshot = contentSnapshot; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
