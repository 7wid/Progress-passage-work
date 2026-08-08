package cn.edu.techgroup.outsourcing.modules.auth.service;

import cn.edu.techgroup.outsourcing.modules.auth.dto.LoginCommand;
import cn.edu.techgroup.outsourcing.modules.auth.vo.CurrentUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    CurrentUserVO login(
            LoginCommand command,
            HttpServletRequest request,
            HttpServletResponse response);
}