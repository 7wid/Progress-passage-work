package cn.edu.techgroup.outsourcing.modules.assignment.mapper;

import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RequestMemberMapper
        extends BaseMapper<RequestMemberEntity> {

    @Select("""
            SELECT id,
                   request_id,
                   user_id,
                   member_type,
                   joined_at
            FROM request_member
            WHERE request_id = #{requestId}
            ORDER BY
                CASE member_type
                    WHEN 'OWNER' THEN 0
                    ELSE 1
                END,
                joined_at,
                id
            """)
    List<RequestMemberEntity> selectByRequestId(
            @Param("requestId") Long requestId);

    @Select("""
            SELECT COUNT(*)
            FROM request_member
            WHERE request_id = #{requestId}
              AND user_id = #{userId}
            """)
    long countByRequestIdAndUserId(
            @Param("requestId") Long requestId,
            @Param("userId") Long userId);

    @Select("""
            SELECT request_id
            FROM request_member
            WHERE user_id = #{userId}
              AND member_type = #{memberType}
            ORDER BY request_id
            """)
    List<Long> selectRequestIdsByUserIdAndType(
            @Param("userId") Long userId,
            @Param("memberType") RequestMemberType memberType);
}
