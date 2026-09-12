package cn.edu.techgroup.outsourcing.modules.auth.mapper;

import java.time.Instant;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PasswordRecoveryMapper {
    record Token(Long userId, String tokenHash, String email,
                 String passwordFingerprint, Instant expiresAt) {}

    @Insert("""
            INSERT INTO password_reset_token
                (user_id, token_hash, email, password_fingerprint, expires_at)
            VALUES (#{userId}, #{tokenHash}, #{email}, #{passwordFingerprint}, #{expiresAt})
            ON DUPLICATE KEY UPDATE token_hash = #{tokenHash}, email = #{email},
                password_fingerprint = #{passwordFingerprint}, expires_at = #{expiresAt}
            """)
    int save(Token token);

    @Select("""
            SELECT user_id, token_hash, email, password_fingerprint, expires_at
            FROM password_reset_token WHERE token_hash = #{hash}
            """)
    Token findToken(@Param("hash") String hash);

    @Select("""
            SELECT user_id, token_hash, email, password_fingerprint, expires_at
            FROM password_reset_token WHERE token_hash = #{hash} FOR UPDATE
            """)
    Token lockToken(@Param("hash") String hash);

    @Delete("DELETE FROM password_reset_token WHERE token_hash = #{hash}")
    int deleteToken(@Param("hash") String hash);

    @Insert("""
            INSERT IGNORE INTO auth_rate_limit (bucket_key, hits, expires_at)
            VALUES (#{key}, 0, #{expiresAt})
            """)
    int createBucket(@Param("key") String key, @Param("expiresAt") Instant expiresAt);

    @Update("""
            UPDATE auth_rate_limit SET hits = hits + 1
            WHERE bucket_key = #{key} AND hits < #{limit}
            """)
    int incrementBucket(@Param("key") String key, @Param("limit") int limit);

    @Update("""
            UPDATE auth_rate_limit SET hits = 1, expires_at = #{until}
            WHERE bucket_key = #{key} AND expires_at <= #{now}
            """)
    int claimCooldown(@Param("key") String key, @Param("now") Instant now,
            @Param("until") Instant until);

    @Delete("DELETE FROM password_reset_token WHERE expires_at < #{now} LIMIT 1000")
    int deleteExpiredTokens(@Param("now") Instant now);

    @Delete("DELETE FROM auth_rate_limit WHERE expires_at < #{now} LIMIT 1000")
    int deleteExpiredBuckets(@Param("now") Instant now);
}
