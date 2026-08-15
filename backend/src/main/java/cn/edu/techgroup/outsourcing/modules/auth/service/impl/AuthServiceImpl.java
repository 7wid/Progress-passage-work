package cn.edu.techgroup.outsourcing.modules.auth.service.impl;

import cn.edu.techgroup.outsourcing.modules.auth.dto.LoginCommand;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditActions;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.auth.service.AuthService;
import cn.edu.techgroup.outsourcing.modules.auth.vo.CurrentUserVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuditRecorder auditRecorder;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            AuditRecorder auditRecorder) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public CurrentUserVO login(
            LoginCommand command,
            HttpServletRequest request,
            HttpServletResponse response) {

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            command.account(),
                            command.password()));
        } catch (AuthenticationException exception) {
            auditRecorder.recordBestEffort(
                    null,
                    AuditActions.AUTH_LOGIN_FAILED,
                    "AUTHENTICATION",
                    null,
                    null,
                    Map.of("outcome", "FAILURE"));
            throw exception;
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        auditRecorder.record(
                loginUser.id(),
                AuditActions.AUTH_LOGIN,
                "USER",
                loginUser.id().toString(),
                null,
                Map.of("outcome", "SUCCESS", "role", loginUser.role().name()));

        // 如果请求已经携带旧 Session，则更换 Session ID，
        // 防止 Session Fixation（会话固定）攻击。
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                request,
                response);

        return CurrentUserVO.from(loginUser);
    }
}
