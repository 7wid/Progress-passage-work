package cn.edu.techgroup.outsourcing.modules.auth.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.auth.dto.LoginCommand;
import cn.edu.techgroup.outsourcing.modules.auth.service.AuthService;
import cn.edu.techgroup.outsourcing.modules.auth.vo.CurrentUserVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public ApiResponse<String> csrf(CsrfToken token) {
        return ApiResponse.success(token.getToken());
    }

    @PostMapping("/login")
    public ApiResponse<CurrentUserVO> login(
            @Valid @RequestBody LoginCommand command,
            HttpServletRequest request) {
        return ApiResponse.success(authService.login(command, request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(CurrentUserVO.from(loginUser));
    }
}
