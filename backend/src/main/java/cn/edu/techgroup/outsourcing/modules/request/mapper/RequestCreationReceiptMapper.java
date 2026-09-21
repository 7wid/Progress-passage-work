package cn.edu.techgroup.outsourcing.modules.request.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RequestCreationReceiptMapper {
    record Receipt(Long creatorId, String idempotencyKey, String operation,
                   String payloadHash, String resultJson) {}

    // The unique key serializes retries across instances without locking unrelated creations.
    @Insert("""
            INSERT INTO request_creation_receipt
                (creator_id, idempotency_key, operation, payload_hash)
            VALUES (#{creatorId}, #{idempotencyKey}, #{operation}, #{payloadHash})
            ON DUPLICATE KEY UPDATE idempotency_key = idempotency_key
            """)
    int reserve(Receipt receipt);

    @Select("""
            SELECT creator_id, idempotency_key, operation, payload_hash, result_json
            FROM request_creation_receipt
            WHERE creator_id = #{creatorId} AND idempotency_key = #{key}
            FOR UPDATE
            """)
    Receipt lock(@Param("creatorId") Long creatorId, @Param("key") String key);

    @Update("""
            UPDATE request_creation_receipt SET result_json = #{resultJson}
            WHERE creator_id = #{creatorId} AND idempotency_key = #{idempotencyKey}
              AND result_json IS NULL
            """)
    int complete(Receipt receipt);
}
