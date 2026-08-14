package cn.edu.techgroup.outsourcing.modules.user.mapper;

import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("""
            SELECT *
            FROM sys_user FORCE INDEX (PRIMARY)
            WHERE id = #{targetUserId}
               OR (role = 'ADMIN' AND status = 'ACTIVE')
            ORDER BY id
            FOR UPDATE
            """)
    List<UserEntity> selectTargetAndActiveAdminsForUpdate(
            @Param("targetUserId") Long targetUserId);

    @Select("""
            SELECT COUNT(*) FROM request_member rm
            JOIN tech_request tr ON tr.id = rm.request_id
            WHERE rm.user_id = #{userId} AND rm.member_type = 'OWNER'
              AND tr.status NOT IN ('COMPLETED', 'REJECTED', 'CANCELLED')
            """)
    long countActiveOwnerRequests(@Param("userId") Long userId);

    @Select("""
            SELECT tr.id
            FROM tech_request tr
            JOIN request_member rm ON rm.request_id = tr.id
            WHERE rm.user_id = #{userId}
              AND rm.member_type = 'OWNER'
              AND tr.status NOT IN ('COMPLETED', 'REJECTED', 'CANCELLED')
            ORDER BY tr.id
            FOR UPDATE OF tr
            """)
    List<Long> lockActiveOwnerRequestIds(@Param("userId") Long userId);

    @Select("""
            <script>SELECT rm.user_id, COUNT(*) AS active_count FROM request_member rm
            JOIN tech_request tr ON tr.id=rm.request_id
            WHERE rm.member_type='OWNER' AND tr.status NOT IN ('COMPLETED','REJECTED','CANCELLED')
              AND rm.user_id IN <foreach collection="userIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY rm.user_id</script>
            """)
    List<UserActiveOwnerCount> selectActiveOwnerCounts(@Param("userIds") Collection<Long> userIds);

    @Select("""
            SELECT id
            FROM sys_user
            WHERE status = 'ACTIVE'
              AND role IN ('MEMBER', 'ADMIN')
            ORDER BY id
            """)
    List<Long> selectActiveTeamUserIds();

    @Select("""
            SELECT id
            FROM sys_user
            WHERE status = 'ACTIVE'
              AND role = 'ADMIN'
            ORDER BY id
            """)
    List<Long> selectActiveAdminIds();

    @Select("""
            <script>
            SELECT id,
                   account,
                   display_name,
                   role,
                   status
            FROM sys_user
            WHERE status = 'ACTIVE'
              AND role IN ('MEMBER', 'ADMIN')
            <if test="keyword != null and keyword != ''">
              AND (
                  account LIKE CONCAT('%', #{keyword}, '%')
                  OR display_name LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            ORDER BY display_name, id
            LIMIT 100
            </script>
            """)
    List<UserEntity> selectAssignableUsers(
            @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT id,
                   account,
                   display_name,
                   role,
                   status
            FROM sys_user
            WHERE id IN
            <foreach collection="userIds"
                     item="userId"
                     open="("
                     separator=","
                     close=")">
                #{userId}
            </foreach>
            </script>
            """)
    List<UserEntity> selectAssignmentUsersByIds(
            @Param("userIds") Collection<Long> userIds);

    @Select("""
            <script>
            SELECT id,
                   account,
                   display_name,
                   role,
                   status
            FROM sys_user FORCE INDEX (PRIMARY)
            WHERE id IN
            <foreach collection="userIds"
                     item="userId"
                     open="("
                     separator=","
                     close=")">
                #{userId}
            </foreach>
            ORDER BY id
            FOR UPDATE
            </script>
            """)
    List<UserEntity> selectAssignmentUsersByIdsForUpdate(
            @Param("userIds") Collection<Long> userIds);

    @Update("""
            UPDATE sys_user
            SET display_name = #{displayName},
                email = #{email},
                phone = #{phone},
                department = #{department},
                role = #{role},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateAdminProfile(UserEntity user);

    @Update("""
            UPDATE sys_user
            SET status = #{status},
                failed_login_count = #{failedLoginCount},
                locked_until = #{lockedUntil},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateAdminStatus(UserEntity user);
}
