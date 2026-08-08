package cn.edu.techgroup.outsourcing.modules.request.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class RequestNumberGenerator {

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Shanghai");

    public String generate(Long requestId, Instant submittedAt) {
        String date = submittedAt
                .atZone(BUSINESS_ZONE)
                .toLocalDate()
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        return "REQ-%s-%04d".formatted(date, requestId);
    }
}