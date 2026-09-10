package cn.edu.techgroup.outsourcing.modules.auth.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.auth.dto.ForgotPasswordCommand;
import cn.edu.techgroup.outsourcing.modules.auth.dto.ResetPasswordCommand;
import cn.edu.techgroup.outsourcing.modules.auth.service.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-recovery")
public class PasswordRecoveryController {
    private final PasswordRecoveryService service;

    public PasswordRecoveryController(PasswordRecoveryService service) { this.service = service; }

    public record Status(boolean enabled, String channel) {}

    @GetMapping
    public ApiResponse<Status> status() {
        return ApiResponse.success(new Status(service.enabled(), "EMAIL"));
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<String> request(@Valid @RequestBody ForgotPasswordCommand command,
            HttpServletRequest request) {
        service.request(command.email(), request.getRemoteAddr());
        return ApiResponse.success("如果该邮箱已绑定可用账号，您将收到重置邮件，请查看收件箱或垃圾邮件。");
    }

    @PostMapping("/reset")
    public ApiResponse<String> reset(@Valid @RequestBody ResetPasswordCommand command,
            HttpServletRequest request) {
        service.checkResetLimit(request.getRemoteAddr());
        service.reset(command);
        return ApiResponse.success("密码已重置，请使用新密码重新登录。");
    }
}
