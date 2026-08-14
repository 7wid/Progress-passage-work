package cn.edu.techgroup.outsourcing.modules.user.mapper;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserSkillMapper {
    @Select("""
            <script>
            SELECT us.user_id, st.id AS skill_id, st.name AS skill_name
            FROM user_skill us JOIN skill_tag st ON st.id = us.skill_id
            WHERE us.user_id IN
            <foreach collection="userIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY st.name, st.id
            </script>
            """)
    List<UserSkillRow> selectByUserIds(@Param("userIds") Collection<Long> userIds);

    @Delete("DELETE FROM user_skill WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO user_skill(user_id, skill_id) VALUES(#{userId}, #{skillId})")
    int insert(@Param("userId") Long userId, @Param("skillId") Long skillId);
}
