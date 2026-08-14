package cn.edu.techgroup.outsourcing.modules.notification.mapper;

import cn.edu.techgroup.outsourcing.modules.notification.entity.NotificationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<NotificationEntity> {

    @Update("""
            UPDATE notification
            SET is_read = 1,
                read_at = #{readAt}
            WHERE id = #{notificationId}
              AND recipient_id = #{recipientId}
              AND is_read = 0
            """)
    int markRead(
            @Param("notificationId") Long notificationId,
            @Param("recipientId") Long recipientId,
            @Param("readAt") Instant readAt);

    @Update("""
            UPDATE notification
            SET is_read = 1,
                read_at = #{readAt}
            WHERE recipient_id = #{recipientId}
              AND is_read = 0
            """)
    int markAllRead(
            @Param("recipientId") Long recipientId,
            @Param("readAt") Instant readAt);
}
