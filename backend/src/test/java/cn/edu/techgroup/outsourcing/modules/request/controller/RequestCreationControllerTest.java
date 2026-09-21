package cn.edu.techgroup.outsourcing.modules.request.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.common.error.GlobalExceptionHandler;
import cn.edu.techgroup.outsourcing.config.AppProperties;
import cn.edu.techgroup.outsourcing.config.SecurityConfig;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestCreationService;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestService;
import cn.edu.techgroup.outsourcing.modules.request.service.RequesterRequestLifecycleService;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.security.ActiveSessionValidationFilter;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RequestController.class)
@ContextConfiguration(classes = RequestCreationControllerTest.Config.class)
class RequestCreationControllerTest {
    @org.springframework.context.annotation.Configuration
    @Import({SecurityConfig.class, RequestController.class, GlobalExceptionHandler.class})
    static class Config {}

    private static final String KEY = "ad3163ee-0cf2-4340-901e-1215c629f149";
    @Autowired MockMvc mvc;
    @MockitoBean RequestCreationService creation;
    @MockitoBean RequestService requests;
    @MockitoBean RequesterRequestLifecycleService lifecycle;
    @MockitoBean AuditRecorder audit;
    @MockitoBean AppProperties properties;
    @MockitoBean ActiveSessionValidationFilter sessionFilter;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(call -> {
            call.getArgument(2, FilterChain.class).doFilter(
                    call.getArgument(0, ServletRequest.class), call.getArgument(1, ServletResponse.class));
            return null;
        }).when(sessionFilter).doFilter(any(), any(), any());
    }

    private LoginUser requester() {
        return new LoginUser(20L, "requester", "", "需求方", UserRole.REQUESTER, true, true);
    }

    @Test
    void draftForwardsKeyAndAuthenticatedOwnerAndKeepsResponseShape() throws Exception {
        when(creation.createDraft(any(), eq(KEY), eq(requester())))
                .thenReturn(new CreatedRequestVO("9007199254740993", null, RequestStatus.DRAFT));
        mvc.perform(post("/api/v1/requests/drafts").with(user(requester())).with(csrf())
                        .header("Idempotency-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"草稿\",\"creatorId\":999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("9007199254740993"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
        verify(creation).createDraft(any(), eq(KEY), eq(requester()));
    }

    @Test
    void formalCreationForwardsKeyAndExactLongCategoryId() throws Exception {
        when(creation.create(any(), eq(KEY), eq(requester())))
                .thenReturn(new CreatedRequestVO("42", "REQ-42", RequestStatus.PENDING_REVIEW));
        String payload = """
                {"categoryId":"9007199254740993","title":"正式需求标题",
                 "background":"%s","description":"%s","expectedResult":"验收成果说明",
                 "expectedDeadline":"2099-12-01","urgency":"NORMAL",
                 "contactInfo":"test@example.org","informationConfirmed":true}
                """.formatted("背景".repeat(10), "描述".repeat(25));
        mvc.perform(post("/api/v1/requests").with(user(requester())).with(csrf())
                        .header("Idempotency-Key", KEY).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.requestNo").value("REQ-42"));
        verify(creation).create(argThat(command -> command.categoryId().equals(9007199254740993L)),
                eq(KEY), eq(requester()));
    }

    @Test
    void csrfAuthenticationAndRoleAreStillRequired() throws Exception {
        mvc.perform(post("/api/v1/requests/drafts").with(user(requester()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/requests/drafts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/requests/drafts").with(user("member").roles("MEMBER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(creation);
    }

    @Test
    void invalidContentIsRejectedBeforeReservingKey() throws Exception {
        mvc.perform(post("/api/v1/requests").with(user(requester())).with(csrf())
                        .header("Idempotency-Key", KEY).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
        verifyNoInteractions(creation);
    }

    @Test
    void legacyHeaderIsOptionalAndConflictIsActionable() throws Exception {
        when(creation.createDraft(any(), isNull(), any()))
                .thenReturn(new CreatedRequestVO("42", null, RequestStatus.DRAFT));
        mvc.perform(post("/api/v1/requests/drafts").with(user(requester())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        when(creation.createDraft(any(), eq(KEY), any()))
                .thenThrow(new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT));
        mvc.perform(post("/api/v1/requests/drafts").with(user(requester())).with(csrf())
                        .header("Idempotency-Key", KEY).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void configuredCorsAllowsIdempotencyHeader() {
        when(properties.webOrigins()).thenReturn(java.util.List.of("https://request.example.org"));
        var source = new SecurityConfig().corsConfigurationSource(properties);
        var request = new org.springframework.mock.web.MockHttpServletRequest("OPTIONS", "/api/v1/requests");
        var cors = source.getCorsConfiguration(request);
        org.junit.jupiter.api.Assertions.assertNotNull(cors);
        org.junit.jupiter.api.Assertions.assertNotNull(cors.checkHeaders(java.util.List.of("Idempotency-Key", "X-XSRF-TOKEN")));
        org.junit.jupiter.api.Assertions.assertNull(cors.checkOrigin("https://untrusted.example.org"));
    }
}
