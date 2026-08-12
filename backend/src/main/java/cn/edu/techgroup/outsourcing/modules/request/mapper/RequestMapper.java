package cn.edu.techgroup.outsourcing.modules.request.mapper;

import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RequestMapper extends BaseMapper<RequestEntity> {

    @Select("""
            SELECT *
            FROM tech_request
            WHERE id = #{requestId}
            FOR UPDATE
            """)
    RequestEntity selectByIdForUpdate(
            @Param("requestId") Long requestId);

    @Update("""
            UPDATE tech_request
            SET status = #{toStatus},
                version = version + 1
            WHERE id = #{requestId}
              AND status = #{fromStatus}
              AND version = #{expectedVersion}
            """)
    int compareAndSetStatus(
            @Param("requestId") Long requestId,
            @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus,
            @Param("expectedVersion") Integer expectedVersion);

    @Update("""
            UPDATE tech_request
            SET progress = #{progress},
                version = version + 1
            WHERE id = #{requestId}
              AND status = #{expectedStatus}
              AND version = #{expectedVersion}
            """)
    int compareAndSetProgress(
            @Param("requestId") Long requestId,
            @Param("expectedStatus") String expectedStatus,
            @Param("expectedVersion") Integer expectedVersion,
            @Param("progress") Integer progress);
}
