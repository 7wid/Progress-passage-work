package cn.edu.techgroup.outsourcing.modules.user.service;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.user.dto.AdminMemberListQuery;
import cn.edu.techgroup.outsourcing.modules.user.dto.CreateMemberCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMemberCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMemberStatusCommand;
import cn.edu.techgroup.outsourcing.modules.user.vo.AdminMemberVO;
import cn.edu.techgroup.outsourcing.modules.user.vo.SkillTagVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.util.List;

public interface AdminMemberService {

    PageResponse<AdminMemberVO> list(
            AdminMemberListQuery query,
            LoginUser operator);

    AdminMemberVO get(Long memberId, LoginUser operator);

    AdminMemberVO create(
            CreateMemberCommand command,
            LoginUser operator);

    AdminMemberVO update(
            Long memberId,
            UpdateMemberCommand command,
            LoginUser operator);

    AdminMemberVO updateStatus(
            Long memberId,
            UpdateMemberStatusCommand command,
            LoginUser operator);

    List<SkillTagVO> skillTags(LoginUser operator);
}
