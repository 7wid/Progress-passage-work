package cn.edu.techgroup.outsourcing.modules.progress.mapper;

import cn.edu.techgroup.outsourcing.modules.progress.entity.ProgressLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProgressLogMapper extends BaseMapper<ProgressLogEntity> {

    @Select("""
            <script>
            SELECT id,
                   request_id,
                   author_id,
                   progress,
                   content,
                   next_plan,
                   next_update_at,
                   visible_to_requester,
                   created_at
            FROM progress_log
            WHERE request_id = #{requestId}
            <if test="includeInternal == false">
              AND visible_to_requester = TRUE
            </if>
            ORDER BY created_at DESC, id DESC
            </script>
            """)
    List<ProgressLogEntity> selectByRequestId(
            @Param("requestId") Long requestId,
            @Param("includeInternal") boolean includeInternal);
}
