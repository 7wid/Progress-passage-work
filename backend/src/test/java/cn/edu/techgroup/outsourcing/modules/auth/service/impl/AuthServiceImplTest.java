package cn.edu.techgroup.outsourcing.modules.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.edu.techgroup.outsourcing.modules.audit.service.AuditActions;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.auth.dto.LoginCommand;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

class AuthServiceImplTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successfulLoginIsAuditedBeforeSessionIsSaved() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        SecurityContextRepository repository = mock(SecurityContextRepository.class);
        AuditRecorder auditRecorder = mock(AuditRecorder.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = mock(Authentication.class);
        LoginUser principal = new LoginUser(
                7L,
                "admin",
                "hash",
                "系统管理员",
                UserRole.ADMIN,
                true,
                true);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(request.getSession(false)).thenReturn(null);
        AuthServiceImpl service = new AuthServiceImpl(
                authenticationManager,
                repository,
                auditRecorder);

        var result = service.login(
                new LoginCommand("admin", "Password123"),
                request,
                response);

        assertEquals("7", result.id());
        InOrder order = inOrder(auditRecorder, repository);
        order.verify(auditRecorder).record(
                eq(7L),
                eq(AuditActions.AUTH_LOGIN),
                eq("USER"),
                eq("7"),
                isNull(),
                any());
        order.verify(repository).saveContext(any(), eq(request), eq(response));
    }

    @Test
    void failedLoginAuditsOutcomeWithoutAccountOrPassword() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        SecurityContextRepository repository = mock(SecurityContextRepository.class);
        AuditRecorder auditRecorder = mock(AuditRecorder.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));
        AuthServiceImpl service = new AuthServiceImpl(
                authenticationManager,
                repository,
                auditRecorder);

        org.junit.jupiter.api.Assertions.assertThrows(
                BadCredentialsException.class,
                () -> service.login(
                        new LoginCommand("sensitive-account", "SensitivePassword123"),
                        request,
                        response));

        verify(auditRecorder).recordBestEffort(
                isNull(),
                eq(AuditActions.AUTH_LOGIN_FAILED),
                eq("AUTHENTICATION"),
                isNull(),
                isNull(),
                any());
        verifyNoInteractions(repository);
    }
}
