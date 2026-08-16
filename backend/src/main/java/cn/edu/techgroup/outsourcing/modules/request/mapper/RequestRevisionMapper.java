package cn.edu.techgroup.outsourcing.modules.request.mapper;

import cn.edu.techgroup.outsourcing.modules.request.entity.RequestRevisionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RequestRevisionMapper extends BaseMapper<RequestRevisionEntity> {

    @Select("""
            SELECT COALESCE(MAX(revision_no), 0)
            FROM request_revision
            WHERE request_id = #{requestId}
            """)
    int selectMaxRevisionNo(@Param("requestId") Long requestId);
}
