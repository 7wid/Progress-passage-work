package cn.edu.techgroup.outsourcing.modules.assignment.service;

import cn.edu.techgroup.outsourcing.modules.assignment.dto.UpdateRequestMembersCommand;
import cn.edu.techgroup.outsourcing.modules.assignment.vo.MemberOptionVO;
import cn.edu.techgroup.outsourcing.modules.assignment.vo.RequestAssignmentVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.util.List;

public interface AssignmentService {

    List<MemberOptionVO> listMemberOptions(
            String keyword,
            LoginUser operator);

    RequestAssignmentVO get(
            Long requestId,
            LoginUser viewer);

    RequestAssignmentVO update(
            Long requestId,
            UpdateRequestMembersCommand command,
            LoginUser operator);
}