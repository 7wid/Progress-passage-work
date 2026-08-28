package cn.edu.techgroup.outsourcing.modules.user.service;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.user.dto.AdminRequesterListQuery;
import cn.edu.techgroup.outsourcing.modules.user.dto.CreateRequesterCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateRequesterStatusCommand;
import cn.edu.techgroup.outsourcing.modules.user.vo.AdminRequesterVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface AdminRequesterService {
    PageResponse<AdminRequesterVO> list(AdminRequesterListQuery query, LoginUser operator);

    AdminRequesterVO create(CreateRequesterCommand command, LoginUser operator);

    AdminRequesterVO updateStatus(Long requesterId, UpdateRequesterStatusCommand command,
            LoginUser operator);
}
