package cn.edu.techgroup.outsourcing.modules.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.config.PasswordRecoveryProperties;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.auth.dto.ResetPasswordCommand;
import cn.edu.techgroup.outsourcing.modules.auth.mapper.PasswordRecoveryMapper;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringJUnitConfig(PasswordRecoveryTransactionTest.Config.class)
class PasswordRecoveryTransactionTest {
    @Autowired PasswordRecoveryService service;
    @Autowired PasswordRecoveryMapper mapper;
    @Autowired RecoveryRateLimiter limiter;
    @Autowired AuditRecorder audit;
    @Autowired DataSource dataSource;
    JdbcTemplate jdbc;
    String token;
    String oldHash;

    @BeforeEach
    void setup() {
        reset(audit);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE sys_user (id BIGINT PRIMARY KEY, account VARCHAR(64),
                    email VARCHAR(160), password_hash VARCHAR(255), status VARCHAR(32),
                    failed_login_count INT, locked_until TIMESTAMP(3),
                    password_reset_at TIMESTAMP(3), updated_at TIMESTAMP(3))
                """);
        jdbc.execute("""
                CREATE TABLE password_reset_token (user_id BIGINT PRIMARY KEY REFERENCES sys_user(id),
                    token_hash CHAR(64) UNIQUE, email VARCHAR(160), password_fingerprint CHAR(64),
                    expires_at TIMESTAMP(3))
                """);
        jdbc.execute("CREATE TABLE auth_rate_limit (bucket_key CHAR(64) PRIMARY KEY, hits INT, expires_at TIMESTAMP(3))");
        oldHash = new BCryptPasswordEncoder(4).encode("OldPassword1");
        jdbc.update("INSERT INTO sys_user (id, account, email, password_hash, status, failed_login_count) VALUES (1, 'user', 'user@example.org', ?, 'ACTIVE', 5)", oldHash);
        token = RecoveryTokens.generate();
        saveToken(token);
    }

    @Test
    void concurrentResetsHaveExactlyOneWinnerAndOldPasswordStopsWorking() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt = () -> {
                try {
                    service.reset(new ResetPasswordCommand(token, "NewPassword1"));
                    return true;
                } catch (BusinessException exception) {
                    return false;
                }
            };
            var results = pool.invokeAll(List.of(attempt, attempt));
            int successes = 0;
            for (var result : results) if (result.get()) successes++;
            assertEquals(1, successes);
        }
        String hash = jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE id = 1", String.class);
        assertTrue(new BCryptPasswordEncoder(4).matches("NewPassword1", hash));
        assertFalse(new BCryptPasswordEncoder(4).matches("OldPassword1", hash));
        assertEquals(0, jdbc.queryForObject("SELECT failed_login_count FROM sys_user", Integer.class));
        assertNotNull(jdbc.queryForObject("SELECT password_reset_at FROM sys_user", java.sql.Timestamp.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_token", Integer.class));
    }

    @Test
    void auditFailureRollsBackPasswordAndTokenConsumption() {
        doThrow(new IllegalStateException("audit unavailable")).when(audit)
                .record(any(), anyString(), anyString(), anyString(), any(), any());
        assertThrows(IllegalStateException.class, () -> service.reset(new ResetPasswordCommand(token, "NewPassword1")));
        assertEquals(oldHash, jdbc.queryForObject("SELECT password_hash FROM sys_user", String.class));
        assertNotNull(mapper.findToken(RecoveryTokens.hash(token)));
        assertNull(jdbc.queryForObject("SELECT password_reset_at FROM sys_user", java.sql.Timestamp.class));
    }

    @Test
    void resendingReplacesPreviousLinkAndExpiredLinkCannotReset() {
        String replacement = RecoveryTokens.generate();
        saveToken(replacement);
        assertNull(mapper.findToken(RecoveryTokens.hash(token)));
        assertThrows(BusinessException.class, () -> service.reset(new ResetPasswordCommand(token, "NewPassword1")));
        jdbc.update("UPDATE password_reset_token SET expires_at = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
        assertThrows(BusinessException.class, () -> service.reset(new ResetPasswordCommand(replacement, "NewPassword1")));
        assertEquals(oldHash, jdbc.queryForObject("SELECT password_hash FROM sys_user", String.class));
    }

    @Test
    void concurrentRateLimitCannotExceedQuotaAndCooldownIsAtomic() throws Exception {
        try (var pool = Executors.newFixedThreadPool(8)) {
            List<Callable<Boolean>> calls = new ArrayList<>();
            for (int i = 0; i < 20; i++) calls.add(() -> limiter.allow("test", "ip", 5, 3600));
            int allowed = 0;
            for (var result : pool.invokeAll(calls)) if (result.get()) allowed++;
            assertEquals(5, allowed);
            calls.clear();
            for (int i = 0; i < 10; i++) calls.add(() -> limiter.allowEmailCooldown("user@example.org"));
            int cooldownWinners = 0;
            for (var result : pool.invokeAll(calls)) if (result.get()) cooldownWinners++;
            assertEquals(1, cooldownWinners);
        }
    }

    @Test
    void firstEmailRequestAlwaysClaimsCooldownDespiteTimestampPrecision() {
        for (int i = 0; i < 100; i++) {
            String email = "user" + i + "@example.org";
            assertTrue(limiter.allowEmailCooldown(email));
            assertFalse(limiter.allowEmailCooldown(email));
        }
    }

    private void saveToken(String value) {
        mapper.save(new PasswordRecoveryMapper.Token(1L, RecoveryTokens.hash(value), "user@example.org",
                RecoveryTokens.hash(oldHash), Instant.now().plusSeconds(900)));
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            JdbcDataSource source = new JdbcDataSource();
            source.setURL("jdbc:h2:mem:recovery;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
            return source;
        }
        @Bean DataSourceTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(source);
            var configuration = new org.apache.ibatis.session.Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.addMapper(PasswordRecoveryMapper.class);
            configuration.addMapper(UserMapper.class);
            factory.setConfiguration(configuration);
            return factory.getObject();
        }
        @Bean SqlSessionTemplate template(SqlSessionFactory factory) { return new SqlSessionTemplate(factory); }
        @Bean PasswordRecoveryMapper mapper(SqlSessionTemplate template) { return template.getMapper(PasswordRecoveryMapper.class); }
        @Bean UserMapper users(SqlSessionTemplate template) { return template.getMapper(UserMapper.class); }
        @Bean RecoveryRateLimiter limiter(PasswordRecoveryMapper mapper) { return new RecoveryRateLimiter(mapper); }
        @Bean AuditRecorder audit() { return mock(AuditRecorder.class); }
        @Bean PasswordRecoveryService service(PasswordRecoveryMapper mapper, UserMapper users,
                RecoveryRateLimiter limiter, AuditRecorder audit, ApplicationEventPublisher events) {
            return new PasswordRecoveryService(new PasswordRecoveryProperties(true, "security@example.org",
                    "https://example.org/reset-password", Duration.ofMinutes(15)), limiter,
                    Runnable::run, mock(RecoveryMailDelivery.class), mapper, users,
                    new BCryptPasswordEncoder(4), audit, events);
        }
    }
}
