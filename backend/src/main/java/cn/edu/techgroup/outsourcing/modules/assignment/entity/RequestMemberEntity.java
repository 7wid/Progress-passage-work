package cn.edu.techgroup.outsourcing.modules.assignment.entity;

import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("request_member")
public class RequestMemberEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long requestId;

    private Long userId;

    private RequestMemberType memberType;

    private Instant joinedAt;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public RequestMemberType getMemberType() {
        return memberType;
    }

    public void setMemberType(RequestMemberType memberType) {
        this.memberType = memberType;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}