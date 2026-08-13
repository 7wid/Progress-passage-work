package cn.edu.techgroup.outsourcing.modules.file.controller;

import cn.edu.techgroup.outsourcing.modules.file.service.AttachmentService;
import cn.edu.techgroup.outsourcing.modules.file.vo.FileDownload;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
public class FileDownloadController {
    private final AttachmentService service;

    public FileDownloadController(AttachmentService service) {
        this.service = service;
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('REQUESTER', 'MEMBER', 'ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        FileDownload download = service.download(id, loginUser);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalName(), StandardCharsets.UTF_8)
                .build();
        MediaType type;
        try {
            type = MediaType.parseMediaType(download.contentType());
        } catch (IllegalArgumentException exception) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(type)
                .contentLength(download.sizeBytes())
                .body(download.resource());
    }
}
