package cn.edu.techgroup.outsourcing.modules.request.service;

import cn.edu.techgroup.outsourcing.modules.request.dto.AdminRequestActionCommand;
import cn.edu.techgroup.outsourcing.modules.request.vo.AdminRequestActionVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface AdminRequestService {

    AdminRequestActionVO cancel(
            Long requestId,
            AdminRequestActionCommand command,
            LoginUser operator);

    AdminRequestActionVO reopen(
            Long requestId,
            AdminRequestActionCommand command,
            LoginUser operator);
}
