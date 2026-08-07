package cn.edu.techgroup.outsourcing.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        List<String> webOrigins,
        @NotBlank String uploadDir,
        @Min(1) long maxFileSizeBytes,
        @Min(1) @Max(20) int maxFileCount) {}
