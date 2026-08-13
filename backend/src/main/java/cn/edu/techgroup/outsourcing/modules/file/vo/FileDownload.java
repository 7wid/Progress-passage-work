package cn.edu.techgroup.outsourcing.modules.file.vo;

import org.springframework.core.io.Resource;

public record FileDownload(
        String originalName,
        String contentType,
        long sizeBytes,
        Resource resource) {}
