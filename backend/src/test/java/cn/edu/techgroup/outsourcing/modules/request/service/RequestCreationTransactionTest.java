package cn.edu.techgroup.outsourcing.modules.request.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.request.dto.CreateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.SaveDraftCommand;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestUrgency;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestCreationReceiptMapper;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringJUnitConfig(RequestCreationTransactionTest.Config.class)
class RequestCreationTransactionTest {
    @Autowired RequestCreationService service;
    @Autowired RequestService requests;
    @Autowired RequesterRequestLifecycleService lifecycle;
    @Autowired DataSource source;
    JdbcTemplate jdbc;
    AtomicLong sequence;
    AtomicLong notifications;
    String key;

    @BeforeEach
    void setup() {
        reset(requests, lifecycle);
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY)");
        jdbc.update("INSERT INTO sys_user VALUES (1), (2)");
        jdbc.execute("""
                CREATE TABLE request_creation_receipt (
                    creator_id BIGINT NOT NULL REFERENCES sys_user(id),
                    idempotency_key CHAR(36) NOT NULL, operation VARCHAR(16) NOT NULL,
                    payload_hash CHAR(64) NOT NULL, result_json VARCHAR(512),
                    PRIMARY KEY (creator_id, idempotency_key))
                """);
        jdbc.execute("CREATE TABLE business_write (id BIGINT PRIMARY KEY, creator_id BIGINT)");
        sequence = new AtomicLong();
        notifications = new AtomicLong();
        key = UUID.randomUUID().toString();
        when(requests.createAndSubmit(any(), any())).thenAnswer(call -> create(call.getArgument(1), RequestStatus.PENDING_REVIEW));
        when(lifecycle.createDraft(any(), any())).thenAnswer(call -> create(call.getArgument(1), RequestStatus.DRAFT));
    }

    private CreatedRequestVO create(LoginUser user, RequestStatus status) {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        long id = sequence.incrementAndGet();
        jdbc.update("INSERT INTO business_write VALUES (?, ?)", id, user.id());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { notifications.incrementAndGet(); }
        });
        return new CreatedRequestVO(Long.toString(id), status == RequestStatus.DRAFT ? null : "REQ-" + id, status);
    }

    @Test
    void concurrentRetriesReturnOneReceiptAndPublishOnce() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<CreatedRequestVO> call = () -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return service.create(command(), key, user(1));
            };
            var first = pool.submit(call);
            var second = pool.submit(call);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }
        verify(requests, times(1)).createAndSubmit(any(), any());
        assertEquals(1, count("business_write"));
        assertEquals(1, count("request_creation_receipt"));
        assertEquals(1, notifications.get());
    }

    @Test
    void draftReplayUsesPersistedOriginalReceiptAndNormalizesKeyCase() {
        var result = service.createDraft(draft("标题"), key, user(1));
        assertEquals(result, service.createDraft(draft("标题"), key.toUpperCase(java.util.Locale.ROOT), user(1)));
        verify(lifecycle, times(1)).createDraft(any(), any());
        assertEquals(RequestStatus.DRAFT, result.status());
        assertNull(result.requestNo());
        assertEquals(1, notifications.get());
    }

    @Test
    void differentContentAndOperationConflictWithoutAnotherBusinessWrite() {
        service.createDraft(draft("原内容"), key, user(1));
        var changed = assertThrows(BusinessException.class,
                () -> service.createDraft(draft("新内容"), key, user(1)));
        assertEquals(ErrorCode.IDEMPOTENCY_KEY_CONFLICT, changed.getErrorCode());
        assertEquals(ErrorCode.IDEMPOTENCY_KEY_CONFLICT, assertThrows(BusinessException.class,
                () -> service.create(command(), key, user(1))).getErrorCode());
        assertEquals(1, count("business_write"));
        verifyNoInteractions(requests);
    }

    @Test
    void sameKeyBelongsToAuthenticatedCreatorAndNewKeysAllowNewRequests() {
        var first = service.create(command(), key, user(1));
        var other = service.create(command(), key, user(2));
        var next = service.create(command(), UUID.randomUUID().toString(), user(1));
        assertNotEquals(first.id(), other.id());
        assertNotEquals(first.id(), next.id());
        assertEquals(3, count("request_creation_receipt"));
    }

    @Test
    void failureAfterBusinessWriteRollsBackReceiptAndAllowsRetry() {
        doAnswer(call -> {
            create(call.getArgument(1), RequestStatus.DRAFT);
            throw new IllegalStateException("audit failed");
        }).when(lifecycle).createDraft(any(), any());
        assertThrows(IllegalStateException.class, () -> service.createDraft(draft("草稿"), key, user(1)));
        assertEquals(0, count("business_write"));
        assertEquals(0, count("request_creation_receipt"));
        assertEquals(0, notifications.get());
        doAnswer(call -> create(call.getArgument(1), RequestStatus.DRAFT))
                .when(lifecycle).createDraft(any(), any());
        service.createDraft(draft("草稿"), key, user(1));
        assertEquals(1, count("business_write"));
        assertEquals(1, notifications.get());
    }

    @Test
    void receiptCompletionFailureRollsBackBusinessWrite() {
        jdbc.execute("ALTER TABLE request_creation_receipt ADD CONSTRAINT force_receipt_failure CHECK (result_json IS NULL)");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> service.create(command(), key, user(1)));
        assertEquals(0, count("business_write"));
        assertEquals(0, count("request_creation_receipt"));
        assertEquals(0, notifications.get());
    }

    @Test
    void invalidKeysAndMembersCannotReserveReceipts() {
        for (String invalid : List.of("", " ", "not-a-uuid", "a".repeat(200), key + " ")) {
            assertEquals(ErrorCode.INVALID_ARGUMENT, assertThrows(BusinessException.class,
                    () -> service.create(command(), invalid, user(1))).getErrorCode());
        }
        LoginUser member = new LoginUser(1L, "member", "", "成员", UserRole.MEMBER, true, true);
        assertEquals(ErrorCode.ACCESS_DENIED, assertThrows(BusinessException.class,
                () -> service.create(command(), key, member)).getErrorCode());
        assertEquals(0, count("request_creation_receipt"));
        verifyNoInteractions(requests, lifecycle);
    }

    @Test
    void unkeyedLegacyClientsRemainCompatible() {
        service.create(command(), null, user(1));
        service.create(command(), null, user(1));
        assertEquals(2, count("business_write"));
        assertEquals(0, count("request_creation_receipt"));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private static LoginUser user(long id) {
        return new LoginUser(id, "user" + id, "", "需求方", UserRole.REQUESTER, true, true);
    }

    private static SaveDraftCommand draft(String title) {
        return new SaveDraftCommand(null, title, null, null, null, null, null, null, null, null, null);
    }

    private static CreateRequestCommand command() {
        return new CreateRequestCommand(1L, "需求测试标题", "需求背景".repeat(6), "具体需求".repeat(15),
                "可以验收的成果", LocalDate.of(2099, 12, 1), RequestUrgency.NORMAL,
                null, null, null, "test@example.org", true);
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            JdbcDataSource source = new JdbcDataSource();
            source.setURL("jdbc:h2:mem:request-creation;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
            return source;
        }
        @Bean DataSourceTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
        @Bean RequestCreationReceiptMapper receipts(DataSource source) throws Exception {
            var factory = new SqlSessionFactoryBean();
            factory.setDataSource(source);
            var config = new org.apache.ibatis.session.Configuration();
            config.setMapUnderscoreToCamelCase(true);
            config.addMapper(RequestCreationReceiptMapper.class);
            factory.setConfiguration(config);
            return new SqlSessionTemplate(factory.getObject()).getMapper(RequestCreationReceiptMapper.class);
        }
        @Bean RequestService requests() { return mock(RequestService.class); }
        @Bean RequesterRequestLifecycleService lifecycle() { return mock(RequesterRequestLifecycleService.class); }
        @Bean RequestCreationService service(RequestCreationReceiptMapper receipts, RequestService requests,
                RequesterRequestLifecycleService lifecycle) {
            return new RequestCreationService(receipts, requests, lifecycle, new ObjectMapper().findAndRegisterModules());
        }
    }
}
