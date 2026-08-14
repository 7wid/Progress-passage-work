package cn.edu.techgroup.outsourcing.security;

import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.common.error.ErrorResponse;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies the database-backed account state on every authenticated request.
 *
 * <p>Role and enabled flags are serialized into the JDBC session. The normal
 * AFTER_COMMIT session invalidation keeps those sessions fresh, while this
 * filter is the fail-closed fallback when deleting a session fails.
 */
@Component
public class ActiveSessionValidationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ActiveSessionValidationFilter.class);

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public ActiveSessionValidationFilter(
            UserMapper userMapper,
            ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean current;
        try {
            UserEntity user = userMapper.selectById(loginUser.id());
            current = user != null
                    && user.getStatus() == UserStatus.ACTIVE
                    && user.getRole() == loginUser.role()
                    && Objects.equals(user.getAccount(), loginUser.account())
                    && Objects.equals(
                            user.getDisplayName(),
                            loginUser.displayName());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to verify active session account id={}",
                    loginUser.id(),
                    exception);
            current = false;
        }

        if (!current) {
            invalidate(request);
            SecurityContextHolder.clearContext();
            writeUnauthenticated(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // The session was already invalidated by another request.
        }
    }

    private void writeUnauthenticated(HttpServletResponse response)
            throws IOException {
        ErrorCode code = ErrorCode.UNAUTHENTICATED;
        response.setStatus(code.getStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                ErrorResponse.of(
                        code.name(),
                        "登录状态已失效，请重新登录"));
    }
}
