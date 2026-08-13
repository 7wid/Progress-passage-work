package cn.edu.techgroup.outsourcing.modules.delivery.mapper;

import cn.edu.techgroup.outsourcing.modules.delivery.entity.AcceptanceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AcceptanceMapper extends BaseMapper<AcceptanceEntity> {

    @Select("""
            SELECT id, request_id, delivery_id, operator_id, result, comment, created_at
            FROM acceptance
            WHERE request_id = #{requestId}
            ORDER BY created_at DESC, id DESC
            """)
    List<AcceptanceEntity> selectByRequestId(@Param("requestId") Long requestId);

    @Select("""
            SELECT COUNT(*)
            FROM acceptance
            WHERE delivery_id = #{deliveryId}
            """)
    long countByDeliveryId(@Param("deliveryId") Long deliveryId);
}
