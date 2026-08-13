package cn.edu.techgroup.outsourcing.modules.file.mapper;

import cn.edu.techgroup.outsourcing.modules.file.entity.AttachmentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import java.time.Instant;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AttachmentMapper extends BaseMapper<AttachmentEntity> {
    @Select("""
            SELECT * FROM attachment
            WHERE request_id = #{requestId}
              AND business_type = #{businessType}
              AND business_id IS NOT NULL
            ORDER BY created_at, id
            """)
    List<AttachmentEntity> selectBoundByRequest(
            @Param("requestId") Long requestId,
            @Param("businessType") String businessType);

    @Select("""
            SELECT * FROM attachment
            WHERE request_id = #{requestId}
              AND business_type = 'DELIVERY'
              AND business_id IS NULL
              AND uploader_id = #{uploaderId}
            ORDER BY created_at, id
            """)
    List<AttachmentEntity> selectPendingDelivery(
            @Param("requestId") Long requestId,
            @Param("uploaderId") Long uploaderId);

    @Select("""
            SELECT COUNT(*) FROM attachment
            WHERE request_id = #{requestId}
              AND business_type = #{businessType}
              AND business_id IS NOT NULL
            """)
    long countBoundByRequest(
            @Param("requestId") Long requestId,
            @Param("businessType") String businessType);

    @Select("""
            SELECT COUNT(*) FROM attachment
            WHERE request_id = #{requestId}
              AND business_type = 'DELIVERY'
              AND business_id IS NULL
              AND uploader_id = #{uploaderId}
            """)
    long countPendingDelivery(
            @Param("requestId") Long requestId,
            @Param("uploaderId") Long uploaderId);

    @Select({
        "<script>", "SELECT * FROM attachment WHERE id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "FOR UPDATE", "</script>"
    })
    List<AttachmentEntity> selectByIdsForUpdate(@Param("ids") Collection<Long> ids);

    @Update({
        "<script>", "UPDATE attachment SET business_id = #{deliveryId}",
        "WHERE request_id = #{requestId} AND business_type = 'DELIVERY'",
        "AND business_id IS NULL AND uploader_id = #{uploaderId} AND id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    int bindPendingToDelivery(
            @Param("requestId") Long requestId,
            @Param("deliveryId") Long deliveryId,
            @Param("uploaderId") Long uploaderId,
            @Param("ids") Collection<Long> ids);

    @Delete("""
            DELETE FROM attachment
            WHERE id = #{id} AND request_id = #{requestId}
              AND business_type = 'DELIVERY' AND business_id IS NULL
              AND uploader_id = #{uploaderId}
            """)
    int deleteOwnPending(
            @Param("id") Long id,
            @Param("requestId") Long requestId,
            @Param("uploaderId") Long uploaderId);

    @Delete("""
            DELETE FROM attachment
            WHERE id = #{id} AND request_id = #{requestId}
              AND business_type = 'REQUEST'
              AND business_id = #{requestId}
            """)
    int deleteRequestAttachment(
            @Param("id") Long id,
            @Param("requestId") Long requestId);

    @Select("""
            SELECT * FROM attachment
            WHERE business_type = 'DELIVERY'
              AND business_id IS NULL
              AND created_at < #{cutoff}
            ORDER BY created_at, id
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<AttachmentEntity> selectExpiredPendingDeliveryForUpdate(
            @Param("cutoff") Instant cutoff,
            @Param("limit") int limit);

    @Delete("""
            DELETE FROM attachment
            WHERE id = #{id}
              AND business_type = 'DELIVERY'
              AND business_id IS NULL
              AND created_at < #{cutoff}
            """)
    int deleteExpiredPendingDelivery(
            @Param("id") Long id,
            @Param("cutoff") Instant cutoff);
}
