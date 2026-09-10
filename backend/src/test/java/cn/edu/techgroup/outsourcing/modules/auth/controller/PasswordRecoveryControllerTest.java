package cn.edu.techgroup.outsourcing.modules.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.edu.techgroup.outsourcing.config.AppProperties;
import cn.edu.techgroup.outsourcing.config.SecurityConfig;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.auth.service.PasswordRecoveryService;
import cn.edu.techgroup.outsourcing.security.ActiveSessionValidationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PasswordRecoveryController.class)
@org.springframework.test.context.ContextConfiguration(classes = PasswordRecoveryControllerTest.Config.class)
class PasswordRecoveryControllerTest {
    @org.springframework.context.annotation.Configuration
    @Import({SecurityConfig.class, PasswordRecoveryController.class,
            cn.edu.techgroup.outsourcing.common.error.GlobalExceptionHandler.class})
    static class Config {}
    @Autowired MockMvc mvc;
    @MockitoBean PasswordRecoveryService service;
    @MockitoBean AuditRecorder audit;
    @MockitoBean AppProperties properties;
    @MockitoBean ActiveSessionValidationFilter sessionFilter;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class).doFilter(
                    invocation.getArgument(0, ServletRequest.class), invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(sessionFilter).doFilter(any(), any(), any());
    }

    @Test
    void statusIsPublicAndUncached() throws Exception {
        when(service.enabled()).thenReturn(true);
        mvc.perform(get("/api/v1/auth/password-recovery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void anonymousRequestStillRequiresCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/password-recovery/requests")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"user@example.org\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousEmailRequestWithCsrfIsAcceptedAndNormalized() throws Exception {
        mvc.perform(post("/api/v1/auth/password-recovery/requests").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\" User@Example.org \"}"))
                .andExpect(status().isAccepted());
        verify(service).request(eq("user@example.org"), anyString());
    }

    @Test
    void malformedEmailIsRejectedWithoutDelivery() throws Exception {
        mvc.perform(post("/api/v1/auth/password-recovery/requests").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void resetUsesBodyTokenAndConsumesQuotaBeforeReset() throws Exception {
        mvc.perform(post("/api/v1/auth/password-recovery/reset").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + "a".repeat(43) + "\",\"newPassword\":\"Password123\"}"))
                .andExpect(status().isOk());
        var order = inOrder(service);
        order.verify(service).checkResetLimit(anyString());
        order.verify(service).reset(any());
    }
}
