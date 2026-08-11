package cn.edu.techgroup.outsourcing.modules.assignment.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import java.util.List;

public record RequestAssignmentVO(
        String requestId,
        RequestStatus requestStatus,
        int requestVersion,
        RequestMemberVO owner,
        List<RequestMemberVO> participants) {
}