package cn.edu.techgroup.outsourcing.modules.user.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class UserMapperLockSqlTest {

    @Test
    void userLockingQueriesUsePrimaryIndexAndAscendingIdOrder()
            throws Exception {
        assertOrderedPrimaryLock(
                UserMapper.class.getMethod(
                        "selectTargetAndActiveAdminsForUpdate",
                        Long.class));
        assertOrderedPrimaryLock(
                UserMapper.class.getMethod(
                        "selectAssignmentUsersByIdsForUpdate",
                        Collection.class));
    }

    private void assertOrderedPrimaryLock(Method method) {
        Select select = method.getAnnotation(Select.class);
        String sql = String.join(" ", select.value())
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
        assertTrue(sql.contains("FROM SYS_USER FORCE INDEX (PRIMARY)"));
        assertTrue(sql.contains("ORDER BY ID"));
        assertTrue(sql.contains("FOR UPDATE"));
    }
}
