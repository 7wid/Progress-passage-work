package cn.edu.techgroup.outsourcing.modules.evaluation.mapper;

import cn.edu.techgroup.outsourcing.modules.evaluation.entity.EvaluationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EvaluationMapper extends BaseMapper<EvaluationEntity> {

    @Select("""
            SELECT *
            FROM evaluation
            WHERE request_id = #{requestId}
              AND conclusion = 'FEASIBLE'
            ORDER BY version DESC, id DESC
            LIMIT 1
            """)
    EvaluationEntity selectLatestFeasibleByRequestId(
            @Param("requestId") Long requestId);

    @Select("""
            SELECT COALESCE(MAX(version), 0)
            FROM evaluation
            WHERE request_id = #{requestId}
            """)
    int selectMaxVersion(@Param("requestId") Long requestId);
}
