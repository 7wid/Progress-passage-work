package cn.edu.techgroup.outsourcing.modules.user.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.user.dto.RegisterUserCommand;
import cn.edu.techgroup.outsourcing.modules.user.service.UserRegistrationService;
import cn.edu.techgroup.outsourcing.modules.user.vo.RegistrationStatusVO;
import cn.edu.techgroup.outsourcing.modules.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserRegistrationController {

    private final UserRegistrationService registrationService;

    public UserRegistrationController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/registration")
    public ApiResponse<RegistrationStatusVO> status() {
        return ApiResponse.success(registrationService.status());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileVO> register(
            @Valid @RequestBody RegisterUserCommand command) {
        return ApiResponse.success(registrationService.register(command));
    }
}
