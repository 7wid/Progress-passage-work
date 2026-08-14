package cn.edu.techgroup.outsourcing.modules.user.mapper;

import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

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
}
