package cn.edu.techgroup.outsourcing.modules.file.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.config.AppProperties;
import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.file.entity.AttachmentEntity;
import cn.edu.techgroup.outsourcing.modules.file.enums.AttachmentBusinessType;
import cn.edu.techgroup.outsourcing.modules.file.mapper.AttachmentMapper;
import cn.edu.techgroup.outsourcing.modules.file.storage.FileStorage;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentSnapshotVO;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentVO;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {
    private static final Long REQUEST_ID = 100L;

    @Mock AttachmentMapper attachmentMapper;
    @Mock RequestMapper requestMapper;
    @Mock RequestMemberMapper requestMemberMapper;
    @Mock FileStorage storage;
    @Mock UserMapper userMapper;
    private AttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AttachmentServiceImpl(
                attachmentMapper, requestMapper, requestMemberMapper, storage,
                new AppProperties(List.of("http://localhost:5173"),
                        "./data/uploads", 20 * 1024 * 1024L, 5), userMapper);
    }

    @Test
    void requesterUploadsRequestAttachmentAndItIsImmediatelyBound() throws Exception {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_REVIEW));
        when(requestMapper.selectByIdForUpdate(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_REVIEW));
        when(attachmentMapper.countBoundByRequest(REQUEST_ID, "REQUEST"))
                .thenReturn(0L);
        when(storage.store(any())).thenReturn("2026/08/random-key");
        doAnswer(invocation -> {
            AttachmentEntity entity = invocation.getArgument(0);
            entity.setId(5L);
            return 1;
        }).when(attachmentMapper).insert(any(AttachmentEntity.class));

        AttachmentVO result = service.upload(
                REQUEST_ID, AttachmentBusinessType.REQUEST,
                textFile("spec.md", "# valid specification"),
                user(1L, UserRole.REQUESTER));

        assertEquals("100", result.businessId());
        assertTrue(result.canDelete());
        verify(storage).store(any());
        verify(requestMapper).selectByIdForUpdate(REQUEST_ID);
    }

    @Test
    void rejectsWrongRequesterAndWrongRequestStatusBeforeStoring() {
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(
                request(1L, RequestStatus.PENDING_REVIEW),
                request(1L, RequestStatus.IN_PROGRESS));

        BusinessException wrongUser = assertThrows(BusinessException.class,
                () -> service.upload(REQUEST_ID, AttachmentBusinessType.REQUEST,
                        textFile("a.txt", "valid text"), user(2L, UserRole.REQUESTER)));
        BusinessException wrongStatus = assertThrows(BusinessException.class,
                () -> service.upload(REQUEST_ID, AttachmentBusinessType.REQUEST,
                        textFile("a.txt", "valid text"), user(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.RESOURCE_NOT_FOUND, wrongUser.getErrorCode());
        assertSame(ErrorCode.ACCESS_DENIED, wrongStatus.getErrorCode());
        verifyNoInteractions(storage);
    }

    @Test
    void onlyOwnerCanUploadPendingDeliveryAttachment() throws Exception {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS));
        when(requestMapper.selectByIdForUpdate(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(member(9L, RequestMemberType.OWNER)));
        when(attachmentMapper.countPendingDelivery(REQUEST_ID, 9L)).thenReturn(0L);
        when(storage.store(any())).thenReturn("2026/08/random-key");
        doAnswer(invocation -> {
            AttachmentEntity entity = invocation.getArgument(0);
            entity.setId(8L);
            return 1;
        }).when(attachmentMapper).insert(any(AttachmentEntity.class));

        AttachmentVO result = service.upload(REQUEST_ID, AttachmentBusinessType.DELIVERY,
                textFile("readme.txt", "valid delivery"), user(9L, UserRole.MEMBER));

        assertEquals(null, result.businessId());
        assertTrue(result.canDelete());
    }

    @Test
    void rejectsDisallowedExtensionAndSpoofedPng() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_REVIEW));
        when(attachmentMapper.countBoundByRequest(REQUEST_ID, "REQUEST"))
                .thenReturn(0L);

        BusinessException executable = assertThrows(BusinessException.class,
                () -> service.upload(REQUEST_ID, AttachmentBusinessType.REQUEST,
                        textFile("attack.exe", "MZ unsafe"), user(1L, UserRole.REQUESTER)));
        BusinessException spoofed = assertThrows(BusinessException.class,
                () -> service.upload(REQUEST_ID, AttachmentBusinessType.REQUEST,
                        textFile("fake.png", "not an image"), user(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.INVALID_ARGUMENT, executable.getErrorCode());
        assertSame(ErrorCode.INVALID_ARGUMENT, spoofed.getErrorCode());
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsUploadWhenFileCountReachedLimit() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_REVIEW));
        when(attachmentMapper.countBoundByRequest(REQUEST_ID, "REQUEST"))
                .thenReturn(5L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.upload(REQUEST_ID, AttachmentBusinessType.REQUEST,
                        textFile("a.txt", "valid"), user(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(storage);
    }

    @Test
    void pendingListOnlyReturnsCurrentUploadersFiles() {
        AttachmentEntity entity = attachment(7L, null, 9L);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(member(9L, RequestMemberType.OWNER)));
        when(attachmentMapper.selectPendingDelivery(REQUEST_ID, 9L))
                .thenReturn(List.of(entity));
        when(attachmentMapper.countPendingDelivery(REQUEST_ID, 9L)).thenReturn(1L);

        AttachmentSnapshotVO result = service.list(
                REQUEST_ID, AttachmentBusinessType.DELIVERY, true,
                user(9L, UserRole.MEMBER));

        assertEquals(1, result.attachments().size());
        assertTrue(result.attachments().getFirst().canDelete());
        assertTrue(result.canUpload());
    }

    @Test
    void bindingRequiresEveryAttachmentToBeOwnPendingDeliveryFile() {
        AttachmentEntity own = attachment(7L, null, 9L);
        AttachmentEntity other = attachment(8L, null, 10L);
        when(attachmentMapper.selectByIdsForUpdate(List.of(7L, 8L)))
                .thenReturn(List.of(own, other));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.bindPendingDeliveryAttachments(
                        REQUEST_ID, 20L, List.of(7L, 8L), user(9L, UserRole.MEMBER)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verify(attachmentMapper, never()).bindPendingToDelivery(any(), any(), any(), any());
    }

    @Test
    void bindsAllValidatedPendingFilesToDelivery() {
        AttachmentEntity first = attachment(7L, null, 9L);
        AttachmentEntity second = attachment(8L, null, 9L);
        when(attachmentMapper.selectByIdsForUpdate(List.of(7L, 8L)))
                .thenReturn(List.of(first, second));
        when(attachmentMapper.bindPendingToDelivery(
                REQUEST_ID, 20L, 9L, List.of(7L, 8L))).thenReturn(2);

        List<AttachmentVO> result = service.bindPendingDeliveryAttachments(
                REQUEST_ID, 20L, List.of(7L, 8L), user(9L, UserRole.MEMBER));

        assertEquals(List.of("20", "20"),
                result.stream().map(AttachmentVO::businessId).toList());
    }

    @Test
    void downloadHidesPendingFileFromOtherUserAndLoadsForUploader() throws Exception {
        AttachmentEntity entity = attachment(7L, null, 9L);
        when(attachmentMapper.selectById(7L)).thenReturn(entity, entity);
        when(storage.load("2026/08/key"))
                .thenReturn(new ByteArrayResource("content".getBytes(StandardCharsets.UTF_8)));

        BusinessException hidden = assertThrows(BusinessException.class,
                () -> service.download(7L, user(10L, UserRole.ADMIN)));
        var download = service.download(7L, user(9L, UserRole.MEMBER));

        assertSame(ErrorCode.RESOURCE_NOT_FOUND, hidden.getErrorCode());
        assertEquals("file.txt", download.originalName());
    }

    @Test
    void deleteOnlyOwnPendingDeliveryFile() throws Exception {
        AttachmentEntity entity = attachment(7L, null, 9L);
        when(attachmentMapper.selectById(7L)).thenReturn(entity);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS));
        when(attachmentMapper.deleteOwnPending(7L, REQUEST_ID, 9L)).thenReturn(1);

        service.deletePending(REQUEST_ID, 7L, user(9L, UserRole.ADMIN));

        verify(attachmentMapper).deleteOwnPending(7L, REQUEST_ID, 9L);
    }

    @Test
    void requesterDeletesRequestAttachmentDuringEditableStatus() {
        AttachmentEntity entity = attachment(7L, REQUEST_ID, 9L);
        entity.setBusinessType(AttachmentBusinessType.REQUEST);
        when(attachmentMapper.selectById(7L)).thenReturn(entity);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.NEED_MORE_INFO));
        when(attachmentMapper.deleteRequestAttachment(7L, REQUEST_ID)).thenReturn(1);

        service.deletePending(REQUEST_ID, 7L, user(1L, UserRole.REQUESTER));

        verify(attachmentMapper).deleteRequestAttachment(7L, REQUEST_ID);
    }

    @Test
    void hidesRequestAttachmentDeleteFromUnrelatedRequester() {
        AttachmentEntity entity = attachment(7L, REQUEST_ID, 9L);
        entity.setBusinessType(AttachmentBusinessType.REQUEST);
        when(attachmentMapper.selectById(7L)).thenReturn(entity);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_REVIEW));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.deletePending(
                        REQUEST_ID, 7L, user(2L, UserRole.REQUESTER)));

        assertSame(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verify(attachmentMapper, never()).deleteRequestAttachment(any(), any());
    }

    @Test
    void rejectsZipTraversalAndOoxmlMacro() throws Exception {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_REVIEW));
        when(attachmentMapper.countBoundByRequest(REQUEST_ID, "REQUEST"))
                .thenReturn(0L);

        BusinessException traversal = assertThrows(BusinessException.class,
                () -> service.upload(REQUEST_ID, AttachmentBusinessType.REQUEST,
                        zipFile("bad.zip", "../escape.txt", new byte[] {1}),
                        user(1L, UserRole.REQUESTER)));
        MockMultipartFile macroDocument = zipFile(
                "macro.docx",
                List.of("[Content_Types].xml", "word/document.xml", "word/vbaProject.bin"),
                new byte[] {1});
        BusinessException macro = assertThrows(BusinessException.class,
                () -> service.upload(REQUEST_ID, AttachmentBusinessType.REQUEST,
                        macroDocument, user(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.INVALID_ARGUMENT, traversal.getErrorCode());
        assertSame(ErrorCode.INVALID_ARGUMENT, macro.getErrorCode());
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsZipEntryThatExpandsBeyondFiftyMegabytes() throws Exception {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_REVIEW));
        when(attachmentMapper.countBoundByRequest(REQUEST_ID, "REQUEST"))
                .thenReturn(0L);
        byte[] oversized = new byte[50 * 1024 * 1024 + 1];

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.upload(REQUEST_ID, AttachmentBusinessType.REQUEST,
                        zipFile("bomb.zip", "large.bin", oversized),
                        user(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(storage);
    }

    @Test
    void cleanupDeletesOnlyStillPendingExpiredDeliveryAttachments() {
        Instant cutoff = Instant.parse("2026-08-13T08:00:00Z");
        AttachmentEntity first = attachment(7L, null, 9L);
        first.setCreatedAt(cutoff.minusSeconds(60));
        AttachmentEntity racedAndBound = attachment(8L, 20L, 9L);
        racedAndBound.setCreatedAt(cutoff.minusSeconds(60));
        when(attachmentMapper.selectExpiredPendingDeliveryForUpdate(cutoff, 100))
                .thenReturn(List.of(first, racedAndBound));
        when(attachmentMapper.deleteExpiredPendingDelivery(7L, cutoff)).thenReturn(1);

        int deleted = service.cleanupExpiredPending(cutoff, 100);

        assertEquals(1, deleted);
        verify(attachmentMapper).deleteExpiredPendingDelivery(7L, cutoff);
        verify(attachmentMapper, never()).deleteExpiredPendingDelivery(8L, cutoff);
    }

    @Test
    void cleanupRejectsUnboundedBatch() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.cleanupExpiredPending(Instant.now(), 101));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(attachmentMapper);
    }

    private MockMultipartFile textFile(String name, String value) {
        return new MockMultipartFile("file", name, "application/octet-stream",
                value.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile zipFile(String fileName, String entryName, byte[] content)
            throws IOException {
        return zipFile(fileName, List.of(entryName), content);
    }

    private MockMultipartFile zipFile(
            String fileName, List<String> entryNames, byte[] content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (String entryName : entryNames) {
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write(content);
                zip.closeEntry();
            }
        }
        return new MockMultipartFile(
                "file", fileName, "application/zip", output.toByteArray());
    }

    private RequestEntity request(Long creatorId, RequestStatus status) {
        RequestEntity request = new RequestEntity();
        request.setId(REQUEST_ID);
        request.setCreatorId(creatorId);
        request.setStatus(status);
        return request;
    }

    private RequestMemberEntity member(Long userId, RequestMemberType type) {
        RequestMemberEntity member = new RequestMemberEntity();
        member.setRequestId(REQUEST_ID);
        member.setUserId(userId);
        member.setMemberType(type);
        return member;
    }

    private AttachmentEntity attachment(Long id, Long businessId, Long uploaderId) {
        AttachmentEntity entity = new AttachmentEntity();
        entity.setId(id);
        entity.setRequestId(REQUEST_ID);
        entity.setBusinessType(AttachmentBusinessType.DELIVERY);
        entity.setBusinessId(businessId);
        entity.setOriginalName("file.txt");
        entity.setStorageKey("2026/08/key");
        entity.setContentType("text/plain");
        entity.setSizeBytes(7L);
        entity.setUploaderId(uploaderId);
        entity.setCreatedAt(Instant.parse("2026-08-13T08:00:00Z"));
        return entity;
    }

    private LoginUser user(Long id, UserRole role) {
        return new LoginUser(id, "user", "hash", "测试用户", role, true, true);
    }
}
