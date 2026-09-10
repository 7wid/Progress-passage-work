package cn.edu.techgroup.outsourcing.modules.auth.service;

import cn.edu.techgroup.outsourcing.modules.auth.mapper.PasswordRecoveryMapper;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecoveryRateLimiter {
    private final PasswordRecoveryMapper mapper;

    public RecoveryRateLimiter(PasswordRecoveryMapper mapper) {
        this.mapper = mapper;
    }

    public boolean allow(String scope, String subject, int limit, int seconds) {
        long window = Instant.now().getEpochSecond() / seconds;
        String key = RecoveryTokens.hash(scope + ":" + subject + ":" + window);
        mapper.createBucket(key, Instant.ofEpochSecond((window + 1) * seconds));
        return mapper.incrementBucket(key, limit) == 1;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void cleanup() {
        Instant now = Instant.now();
        for (int batch = 0; batch < 10 && mapper.deleteExpiredTokens(now) == 1000; batch++) {
            // Bound lock duration and work per scheduled run.
        }
        for (int batch = 0; batch < 10 && mapper.deleteExpiredBuckets(now) == 1000; batch++) {
            // Global request quotas keep hourly growth below this cleanup budget.
        }
    }

    public boolean allowEmailCooldown(String email) {
        Instant now = Instant.now();
        String key = RecoveryTokens.hash("send-cooldown:" + email);
        // DATETIME(3) may round timestamps up; a new bucket must always start claimable.
        mapper.createBucket(key, Instant.EPOCH);
        return mapper.claimCooldown(key, now, now.plusSeconds(60)) == 1;
    }
}
