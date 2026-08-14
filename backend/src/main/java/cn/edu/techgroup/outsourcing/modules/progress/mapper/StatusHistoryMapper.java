package cn.edu.techgroup.outsourcing.modules.progress.mapper;

import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StatusHistoryMapper
        extends BaseMapper<StatusHistoryEntity> {
    @Select("""
            SELECT * FROM status_history
            WHERE request_id=#{requestId} AND to_status='CANCELLED'
            ORDER BY created_at DESC,id DESC LIMIT 1
            """)
    StatusHistoryEntity selectLatestCancellation(@Param("requestId") Long requestId);
}
