package cn.edu.techgroup.outsourcing.modules.delivery.mapper;

import cn.edu.techgroup.outsourcing.modules.delivery.entity.DeliveryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeliveryMapper extends BaseMapper<DeliveryEntity> {

    @Select("""
            SELECT id, request_id, submitter_id, description, delivery_url, created_at
            FROM delivery
            WHERE request_id = #{requestId}
            ORDER BY created_at DESC, id DESC
            """)
    List<DeliveryEntity> selectByRequestId(@Param("requestId") Long requestId);

    @Select("""
            SELECT id, request_id, submitter_id, description, delivery_url, created_at
            FROM delivery
            WHERE request_id = #{requestId}
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    DeliveryEntity selectLatestByRequestId(@Param("requestId") Long requestId);
}
