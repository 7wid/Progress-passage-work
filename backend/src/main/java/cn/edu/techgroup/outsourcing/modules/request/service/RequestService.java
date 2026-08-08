package cn.edu.techgroup.outsourcing.modules.request.service;

import cn.edu.techgroup.outsourcing.modules.request.dto.CreateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface RequestService {

    CreatedRequestVO createAndSubmit(
            CreateRequestCommand command,
            LoginUser operator);
}