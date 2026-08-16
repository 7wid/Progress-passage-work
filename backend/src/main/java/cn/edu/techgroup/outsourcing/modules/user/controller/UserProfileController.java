package cn.edu.techgroup.outsourcing.modules.user.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.user.dto.ChangePasswordCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMyProfileCommand;
import cn.edu.techgroup.outsourcing.modules.user.service.UserProfileService;
import cn.edu.techgroup.outsourcing.modules.user.vo.PasswordChangeResultVO;
import cn.edu.techgroup.outsourcing.modules.user.vo.UserProfileVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final SecurityContextRepository securityContextRepository;

    public UserProfileController(
            UserProfileService userProfileService,
            SecurityContextRepository securityContextRepository) {
        this.userProfileService = userProfileService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping
    public ApiResponse<UserProfileVO> get(
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(userProfileService.get(loginUser));
    }

    @PatchMapping
    public ApiResponse<UserProfileVO> update(
            @Valid @RequestBody UpdateMyProfileCommand command,
            @AuthenticationPrincipal LoginUser loginUser,
            HttpServletRequest request,
            HttpServletResponse response) {
        UserProfileVO profile = userProfileService.update(command, loginUser);
        refreshPrincipal(loginUser, profile.displayName(), request, response);
        return ApiResponse.success(profile);
    }

    @PutMapping("/password")
    public ApiResponse<PasswordChangeResultVO> changePassword(
            @Valid @RequestBody ChangePasswordCommand command,
            @AuthenticationPrincipal LoginUser loginUser,
            HttpServletRequest request) {
        String currentSessionId = request.getSession(false) == null
                ? null
                : request.getSession(false).getId();
        return ApiResponse.success(userProfileService.changePassword(
                command,
                currentSessionId,
                loginUser));
    }

    private void refreshPrincipal(
            LoginUser current,
            String displayName,
            HttpServletRequest request,
            HttpServletResponse response) {
        LoginUser refreshed = new LoginUser(
                current.id(),
                current.account(),
                current.password(),
                displayName,
                current.role(),
                current.enabled(),
                current.accountNonLocked());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                refreshed,
                null,
                refreshed.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
