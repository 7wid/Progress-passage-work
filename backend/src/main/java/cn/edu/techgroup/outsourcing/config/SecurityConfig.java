package cn.edu.techgroup.outsourcing.config;

import static org.springframework.security.config.Customizer.withDefaults;

import cn.edu.techgroup.outsourcing.security.ActiveSessionValidationFilter;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditActions;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            ActiveSessionValidationFilter activeSessionValidationFilter,
            AuditRecorder auditRecorder)
            throws Exception {
        CookieCsrfTokenRepository csrfRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        http.cors(withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(
                                new SpaCsrfTokenRequestHandler()))
                .securityContext(security -> security
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/csrf")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/registration")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/users/register")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, exception) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN)))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("SESSION")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if (authentication != null
                                    && authentication.getPrincipal() instanceof LoginUser user) {
                                auditRecorder.recordBestEffort(
                                        user.id(),
                                        AuditActions.AUTH_LOGOUT,
                                        "USER",
                                        user.id().toString(),
                                        null,
                                        Map.of("outcome", "SUCCESS"));
                            }
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        }))
                .addFilterBefore(
                        activeSessionValidationFilter,
                        AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            AppProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.webOrigins());
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Content-Type", "X-XSRF-TOKEN", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
