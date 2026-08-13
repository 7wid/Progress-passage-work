package cn.edu.techgroup.outsourcing.modules.file.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.file.enums.AttachmentBusinessType;
import cn.edu.techgroup.outsourcing.modules.file.service.AttachmentService;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentSnapshotVO;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/requests/{requestId}/attachments")
@PreAuthorize("hasAnyRole('REQUESTER', 'MEMBER', 'ADMIN')")
public class AttachmentController {
    private final AttachmentService service;

    public AttachmentController(AttachmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttachmentVO> upload(
            @PathVariable Long requestId,
            @RequestParam AttachmentBusinessType businessType,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(service.upload(requestId, businessType, file, loginUser));
    }

    @GetMapping
    public ApiResponse<AttachmentSnapshotVO> list(
            @PathVariable Long requestId,
            @RequestParam AttachmentBusinessType businessType,
            @RequestParam(defaultValue = "false") boolean pendingOnly,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(service.list(requestId, businessType, pendingOnly, loginUser));
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long requestId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal LoginUser loginUser) {
        service.deletePending(requestId, attachmentId, loginUser);
    }
}
