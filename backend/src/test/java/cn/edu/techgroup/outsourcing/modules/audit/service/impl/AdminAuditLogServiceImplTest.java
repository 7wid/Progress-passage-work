package cn.edu.techgroup.outsourcing.modules.audit.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.dto.AdminAuditLogQuery;
import cn.edu.techgroup.outsourcing.modules.audit.entity.AuditLogEntity;
import cn.edu.techgroup.outsourcing.modules.audit.mapper.AuditLogMapper;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditDataSanitizer;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                AuditLogEntity.class);
    }

    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private UserMapper userMapper;

    private AdminAuditLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminAuditLogServiceImpl(
                auditLogMapper,
                userMapper,
                new AuditDataSanitizer(new ObjectMapper()));
    }

    @Test
    void adminListsStableSafeAuditViews() {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(99L);
        entity.setActorId(7L);
        entity.setAction("MEMBER_UPDATE");
        entity.setTargetType("USER");
        entity.setTargetId("8");
        entity.setBeforeData("{\"role\":\"MEMBER\"}");
        entity.setAfterData("{\"passwordHash\":\"secret\",\"role\":\"ADMIN\"}");
        entity.setRequestId("req-audit-1");
        entity.setIpAddress("192.168.1.25");
        entity.setCreatedAt(Instant.parse("2026-08-15T08:00:00Z"));
        Page<AuditLogEntity> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(entity));
        when(auditLogMapper.selectPage(
                org.mockito.ArgumentMatchers.<Page<AuditLogEntity>>any(),
                org.mockito.ArgumentMatchers.<Wrapper<AuditLogEntity>>any()))
                .thenReturn(page);
        UserEntity actor = new UserEntity();
        actor.setId(7L);
        actor.setDisplayName("系统管理员");
        when(userMapper.selectByIds(any())).thenReturn(List.of(actor));

        var result = service.list(query(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 15)),
                loginUser(UserRole.ADMIN));

        assertEquals(1, result.total());
        var item = result.items().getFirst();
        assertEquals("系统管理员", item.actorName());
        assertEquals("192.168.1.*", item.ipAddress());
        assertEquals("[REDACTED]", item.afterData().get("passwordHash").asText());
        assertEquals("ADMIN", item.afterData().get("role").asText());
    }

    @Test
    void nonAdminCannotQueryAuditLogs() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.list(query(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 15)),
                        loginUser(UserRole.MEMBER)));

        assertSame(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verifyNoInteractions(auditLogMapper, userMapper);
    }

    @Test
    void rejectsRangesLongerThanOneYear() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.list(query(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2026, 8, 15)),
                        loginUser(UserRole.ADMIN)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(auditLogMapper, userMapper);
    }

    private AdminAuditLogQuery query(LocalDate from, LocalDate to) {
        return new AdminAuditLogQuery(
                1,
                20,
                null,
                null,
                null,
                null,
                null,
                from,
                to);
    }

    private LoginUser loginUser(UserRole role) {
        return new LoginUser(
                7L,
                "admin",
                "hash",
                "系统管理员",
                role,
                true,
                true);
    }
}
