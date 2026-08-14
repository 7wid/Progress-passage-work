package cn.edu.techgroup.outsourcing.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ActiveSessionValidationFilterTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private FilterChain filterChain;

    private ActiveSessionValidationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ActiveSessionValidationFilter(
                userMapper,
                new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousRequestContinuesWithoutDatabaseLookup() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userMapper);
    }

    @Test
    void activeAccountWithUnchangedRoleContinues() throws Exception {
        LoginUser loginUser = loginUser(UserRole.ADMIN);
        authenticate(loginUser);
        when(userMapper.selectById(9L)).thenReturn(
                user(UserRole.ADMIN, UserStatus.ACTIVE));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void disabledAccountInvalidatesSessionAndReturnsUniform401()
            throws Exception {
        authenticate(loginUser(UserRole.ADMIN));
        when(userMapper.selectById(9L)).thenReturn(
                user(UserRole.ADMIN, UserStatus.DISABLED));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        session.setAttribute("marker", "value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("UNAUTHENTICATED",
                new ObjectMapper().readTree(response.getContentAsString())
                        .path("error")
                        .path("code")
                        .asText());
        assertThrows(
                IllegalStateException.class,
                () -> session.getAttribute("marker"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void roleChangeInvalidatesCachedAuthentication() throws Exception {
        authenticate(loginUser(UserRole.ADMIN));
        when(userMapper.selectById(9L)).thenReturn(
                user(UserRole.MEMBER, UserStatus.ACTIVE));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void displayNameChangeInvalidatesCachedAuthentication() throws Exception {
        authenticate(loginUser(UserRole.ADMIN));
        UserEntity renamedUser = user(UserRole.ADMIN, UserStatus.ACTIVE);
        renamedUser.setDisplayName("新的管理员名称");
        when(userMapper.selectById(9L)).thenReturn(renamedUser);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void accountLookupFailureFailsClosed() throws Exception {
        authenticate(loginUser(UserRole.ADMIN));
        when(userMapper.selectById(9L))
                .thenThrow(new IllegalStateException("database unavailable"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    private void authenticate(LoginUser loginUser) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        loginUser,
                        loginUser.password(),
                        loginUser.getAuthorities()));
    }

    private LoginUser loginUser(UserRole role) {
        return new LoginUser(
                9L,
                "admin",
                "hash",
                "管理员",
                role,
                true,
                true);
    }

    private UserEntity user(UserRole role, UserStatus status) {
        UserEntity user = new UserEntity();
        user.setId(9L);
        user.setAccount("admin");
        user.setDisplayName("管理员");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
