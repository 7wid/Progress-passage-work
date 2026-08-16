package cn.edu.techgroup.outsourcing.modules.file.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.config.AppProperties;
import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.file.entity.AttachmentEntity;
import cn.edu.techgroup.outsourcing.modules.file.enums.AttachmentBusinessType;
import cn.edu.techgroup.outsourcing.modules.file.mapper.AttachmentMapper;
import cn.edu.techgroup.outsourcing.modules.file.service.AttachmentService;
import cn.edu.techgroup.outsourcing.modules.file.storage.FileStorage;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentSnapshotVO;
import cn.edu.techgroup.outsourcing.modules.file.vo.AttachmentVO;
import cn.edu.techgroup.outsourcing.modules.file.vo.FileDownload;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentServiceImpl implements AttachmentService {
    private static final Logger log = LoggerFactory.getLogger(AttachmentServiceImpl.class);
    private static final int SNIFF_LIMIT = 64 * 1024;
    private static final int MAX_ZIP_ENTRIES = 1000;
    private static final long MAX_ZIP_ENTRY_BYTES = 50L * 1024 * 1024;
    private static final long MAX_ZIP_TOTAL_BYTES = 100L * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("txt", "text/plain"), Map.entry("md", "text/markdown"),
            Map.entry("csv", "text/csv"), Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("webp", "image/webp"), Map.entry("zip", "application/zip"));
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md", "csv");
    private static final Set<String> LEGACY_OFFICE = Set.of("doc", "xls", "ppt");
    private static final Set<String> ZIP_EXTENSIONS = Set.of("zip", "docx", "xlsx", "pptx");

    private final AttachmentMapper attachmentMapper;
    private final RequestMapper requestMapper;
    private final RequestMemberMapper requestMemberMapper;
    private final FileStorage storage;
    private final AppProperties properties;
    private final UserMapper userMapper;

    public AttachmentServiceImpl(
            AttachmentMapper attachmentMapper,
            RequestMapper requestMapper,
            RequestMemberMapper requestMemberMapper,
            FileStorage storage,
            AppProperties properties,
            UserMapper userMapper) {
        this.attachmentMapper = attachmentMapper;
        this.requestMapper = requestMapper;
        this.requestMemberMapper = requestMemberMapper;
        this.storage = storage;
        this.properties = properties;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttachmentVO upload(Long requestId, AttachmentBusinessType businessType,
            MultipartFile file, LoginUser operator) {
        requireAuthenticated(operator);
        if (businessType == null) {
            throw invalid("请选择附件业务类型");
        }
        RequestEntity request = findVisibleRequest(requestId, operator);
        validateUploadPermission(request, businessType, operator);
        validateCountBeforeUpload(requestId, businessType, operator.id());
        ValidatedFile validated = validateFile(file);

        String storageKey;
        try {
            storageKey = storage.store(file);
        } catch (IOException exception) {
            log.error("Failed to store attachment for request {}", requestId, exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "附件保存失败，请稍后重试");
        }
        registerRollbackCleanup(storageKey);

        RequestEntity lockedRequest = requestMapper.selectByIdForUpdate(requestId);
        if (lockedRequest == null) {
            throw hiddenRequest();
        }
        assertVisible(lockedRequest, operator);
        validateUploadPermission(lockedRequest, businessType, operator);
        validateCountBeforeUpload(requestId, businessType, operator.id());

        AttachmentEntity entity = new AttachmentEntity();
        entity.setRequestId(requestId);
        entity.setBusinessType(businessType);
        entity.setBusinessId(businessType == AttachmentBusinessType.REQUEST ? requestId : null);
        entity.setOriginalName(validated.originalName());
        entity.setStorageKey(storageKey);
        entity.setContentType(validated.contentType());
        entity.setSizeBytes(file.getSize());
        entity.setUploaderId(operator.id());
        entity.setCreatedAt(Instant.now());
        if (attachmentMapper.insert(entity) != 1 || entity.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "附件元数据保存失败");
        }
        return toVO(
                entity,
                canDelete(entity, lockedRequest, operator),
                operator.displayName());
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentSnapshotVO list(Long requestId, AttachmentBusinessType businessType,
            boolean pendingOnly, LoginUser viewer) {
        RequestEntity request = findVisibleRequest(requestId, viewer);
        if (businessType == null) {
            throw invalid("请选择附件业务类型");
        }
        if (pendingOnly && businessType != AttachmentBusinessType.DELIVERY) {
            throw invalid("只有交付附件支持查看待绑定文件");
        }
        List<AttachmentEntity> entities = pendingOnly
                ? attachmentMapper.selectPendingDelivery(requestId, viewer.id())
                : attachmentMapper.selectBoundByRequest(requestId, businessType.getValue());
        Map<Long, String> uploaderNames = loadUploaderNames(entities);
        boolean canUpload = canUpload(request, businessType, viewer)
                && currentCount(requestId, businessType, viewer.id()) < properties.maxFileCount();
        return new AttachmentSnapshotVO(
                requestId.toString(), businessType, canUpload,
                entities.stream()
                        .map(entity -> toVO(entity,
                                canDelete(entity, request, viewer),
                                uploaderNames.getOrDefault(entity.getUploaderId(), "未知用户")))
                        .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePending(Long requestId, Long attachmentId, LoginUser operator) {
        requireAuthenticated(operator);
        if (attachmentId == null || attachmentId <= 0) {
            throw hiddenAttachment();
        }
        AttachmentEntity entity = attachmentMapper.selectById(attachmentId);
        if (entity == null || !Objects.equals(entity.getRequestId(), requestId)) {
            throw hiddenAttachment();
        }
        RequestEntity request = findRequest(requestId);
        if (entity.getBusinessType() == AttachmentBusinessType.REQUEST) {
            if (!Objects.equals(entity.getBusinessId(), requestId)
                    || !canManageRequestAttachment(request, operator)) {
                throw hiddenAttachment();
            }
            if (!requestAttachmentStatusAllowsChange(request)) {
                throw new BusinessException(
                        ErrorCode.REQUEST_STATUS_CONFLICT,
                        "当前需求状态不允许删除需求附件");
            }
            if (attachmentMapper.deleteRequestAttachment(attachmentId, requestId) != 1) {
                throw hiddenAttachment();
            }
        } else if (entity.getBusinessType() == AttachmentBusinessType.DELIVERY
                && entity.getBusinessId() == null
                && Objects.equals(entity.getUploaderId(), operator.id())) {
            if (attachmentMapper.deleteOwnPending(
                    attachmentId, requestId, operator.id()) != 1) {
                throw hiddenAttachment();
            }
        } else {
            throw hiddenAttachment();
        }
        deleteAfterCommit(entity.getStorageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload download(Long attachmentId, LoginUser viewer) {
        requireAuthenticated(viewer);
        if (attachmentId == null || attachmentId <= 0) {
            throw hiddenAttachment();
        }
        AttachmentEntity entity = attachmentMapper.selectById(attachmentId);
        if (entity == null) {
            throw hiddenAttachment();
        }
        if (entity.getBusinessId() == null) {
            if (!Objects.equals(entity.getUploaderId(), viewer.id())) {
                throw hiddenAttachment();
            }
        } else {
            findVisibleRequest(entity.getRequestId(), viewer);
        }
        try {
            Resource resource = storage.load(entity.getStorageKey());
            return new FileDownload(entity.getOriginalName(), entity.getContentType(),
                    entity.getSizeBytes(), resource);
        } catch (IOException exception) {
            log.error("Attachment metadata exists but content is missing: {}", entity.getId(), exception);
            throw hiddenAttachment();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentVO> findBoundDeliveryAttachments(
            Long requestId, Long deliveryId, LoginUser viewer) {
        findVisibleRequest(requestId, viewer);
        return attachmentMapper.selectBoundByRequest(
                        requestId, AttachmentBusinessType.DELIVERY.getValue())
                .stream()
                .filter(entity -> Objects.equals(entity.getBusinessId(), deliveryId))
                .map(entity -> toVO(entity, false,
                        loadUploaderName(entity.getUploaderId())))
                .toList();
    }

    @Override
    public List<AttachmentVO> bindPendingDeliveryAttachments(
            Long requestId, Long deliveryId, List<Long> attachmentIds, LoginUser operator) {
        List<Long> ids = normalizeIds(attachmentIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<AttachmentEntity> entities = attachmentMapper.selectByIdsForUpdate(ids);
        if (entities.size() != ids.size()
                || entities.stream().anyMatch(entity ->
                        !Objects.equals(entity.getRequestId(), requestId)
                                || entity.getBusinessType() != AttachmentBusinessType.DELIVERY
                                || entity.getBusinessId() != null
                                || !Objects.equals(entity.getUploaderId(), operator.id()))) {
            throw invalid("交付附件不存在、已绑定或不属于当前用户");
        }
        if (attachmentMapper.bindPendingToDelivery(
                requestId, deliveryId, operator.id(), ids) != ids.size()) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_CONFLICT,
                    "交付附件已发生变化，请刷新后重试");
        }
        entities.forEach(entity -> entity.setBusinessId(deliveryId));
        return entities.stream().map(entity -> toVO(
                entity, false, operator.displayName())).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredPending(Instant cutoff, int limit) {
        if (cutoff == null || limit < 1 || limit > 100) {
            throw invalid("暂存附件清理参数不正确");
        }
        List<AttachmentEntity> expired =
                attachmentMapper.selectExpiredPendingDeliveryForUpdate(cutoff, limit);
        int deleted = 0;
        for (AttachmentEntity entity : expired) {
            if (entity.getBusinessType() != AttachmentBusinessType.DELIVERY
                    || entity.getBusinessId() != null
                    || entity.getCreatedAt() == null
                    || !entity.getCreatedAt().isBefore(cutoff)) {
                continue;
            }
            if (attachmentMapper.deleteExpiredPendingDelivery(entity.getId(), cutoff) == 1) {
                deleteAfterCommit(entity.getStorageKey());
                deleted++;
            }
        }
        return deleted;
    }

    private void validateUploadPermission(RequestEntity request,
            AttachmentBusinessType type, LoginUser operator) {
        if (!canUpload(request, type, operator)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "当前用户或需求状态不允许上传附件");
        }
    }

    private boolean canUpload(RequestEntity request,
            AttachmentBusinessType type, LoginUser operator) {
        if (operator == null) {
            return false;
        }
        if (type == AttachmentBusinessType.REQUEST) {
            boolean statusAllowed = request.getStatus() == RequestStatus.DRAFT
                    || request.getStatus() == RequestStatus.PENDING_REVIEW
                    || request.getStatus() == RequestStatus.NEED_MORE_INFO;
            return statusAllowed && (operator.role() == UserRole.ADMIN
                    || (operator.role() == UserRole.REQUESTER
                            && Objects.equals(request.getCreatorId(), operator.id())));
        }
        if (type == AttachmentBusinessType.DELIVERY) {
            if (request.getStatus() != RequestStatus.IN_PROGRESS) {
                return false;
            }
            return operator.role() == UserRole.ADMIN || (operator.role() == UserRole.MEMBER
                    && isOwner(request.getId(), operator.id()));
        }
        return false;
    }

    private boolean isOwner(Long requestId, Long userId) {
        List<RequestMemberEntity> members = requestMemberMapper.selectByRequestId(requestId);
        return members.stream().anyMatch(member -> Objects.equals(member.getUserId(), userId)
                && member.getMemberType() == RequestMemberType.OWNER);
    }

    private void validateCountBeforeUpload(Long requestId,
            AttachmentBusinessType type, Long uploaderId) {
        if (currentCount(requestId, type, uploaderId) >= properties.maxFileCount()) {
            throw invalid("附件数量不能超过 " + properties.maxFileCount() + " 个");
        }
    }

    private long currentCount(Long requestId, AttachmentBusinessType type, Long uploaderId) {
        return type == AttachmentBusinessType.REQUEST
                ? attachmentMapper.countBoundByRequest(requestId, type.getValue())
                : attachmentMapper.countPendingDelivery(requestId, uploaderId);
    }

    private ValidatedFile validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw invalid("附件不能为空");
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw invalid("单个附件不能超过 20 MB");
        }
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            throw invalid("附件名称不能为空");
        }
        originalName = originalName.trim();
        if (originalName.length() > 255 || originalName.contains("/")
                || originalName.contains("\\") || originalName.chars().anyMatch(
                        value -> value == 0 || Character.isISOControl(value))) {
            throw invalid("附件名称不合法");
        }
        int dot = originalName.lastIndexOf('.');
        String extension = dot < 1 || dot == originalName.length() - 1
                ? "" : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String contentType = CONTENT_TYPES.get(extension);
        if (contentType == null) {
            throw invalid("不支持该附件类型");
        }
        try {
            validateContent(file, extension);
        } catch (IOException exception) {
            throw invalid("无法读取附件内容");
        }
        return new ValidatedFile(originalName, contentType);
    }

    private void validateContent(MultipartFile file, String extension) throws IOException {
        byte[] prefix;
        try (InputStream input = file.getInputStream()) {
            prefix = input.readNBytes(SNIFF_LIMIT);
        }
        boolean valid;
        if (TEXT_EXTENSIONS.contains(extension)) {
            valid = validText(prefix);
        } else if ("pdf".equals(extension)) {
            valid = startsWith(prefix, new int[] {0x25, 0x50, 0x44, 0x46, 0x2D});
        } else if ("png".equals(extension)) {
            valid = startsWith(prefix, new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        } else if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            valid = startsWith(prefix, new int[] {0xFF, 0xD8, 0xFF});
        } else if ("webp".equals(extension)) {
            valid = prefix.length >= 12 && ascii(prefix, 0, "RIFF") && ascii(prefix, 8, "WEBP");
        } else if (LEGACY_OFFICE.contains(extension)) {
            valid = startsWith(prefix,
                    new int[] {0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1});
        } else if (ZIP_EXTENSIONS.contains(extension)) {
            valid = validZip(file, extension);
        } else {
            valid = false;
        }
        if (!valid) {
            throw invalid("附件内容与文件扩展名不匹配");
        }
    }

    private boolean validZip(MultipartFile file, String extension) throws IOException {
        boolean hasContentTypes = false;
        boolean hasExpectedFolder = "zip".equals(extension);
        String expectedPrefix = switch (extension) {
            case "docx" -> "word/";
            case "xlsx" -> "xl/";
            case "pptx" -> "ppt/";
            default -> "";
        };
        int entries = 0;
        long totalBytes = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ZIP_ENTRIES) {
                    return false;
                }
                String name = entry.getName();
                if (!validZipEntryName(name)) {
                    return false;
                }
                String lowerName = name.toLowerCase(Locale.ROOT);
                if (!"zip".equals(extension)
                        && (lowerName.equals("vbaproject.bin")
                                || lowerName.endsWith("/vbaproject.bin"))) {
                    return false;
                }
                hasContentTypes |= "[Content_Types].xml".equals(name);
                hasExpectedFolder |= !expectedPrefix.isEmpty() && name.startsWith(expectedPrefix);
                long entryBytes = 0;
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    entryBytes += read;
                    totalBytes += read;
                    if (entryBytes > MAX_ZIP_ENTRY_BYTES
                            || totalBytes > MAX_ZIP_TOTAL_BYTES) {
                        return false;
                    }
                }
            }
        } catch (java.util.zip.ZipException exception) {
            return false;
        }
        return "zip".equals(extension) ? entries > 0 : hasContentTypes && hasExpectedFolder;
    }

    private boolean validZipEntryName(String name) {
        if (!StringUtils.hasText(name) || name.indexOf('\0') >= 0) {
            return false;
        }
        String normalizedSeparators = name.replace('\\', '/');
        if (normalizedSeparators.startsWith("/")
                || normalizedSeparators.matches("^[A-Za-z]:/.*")) {
            return false;
        }
        try {
            java.nio.file.Path normalized =
                    java.nio.file.Path.of(normalizedSeparators).normalize();
            return !normalized.isAbsolute()
                    && !normalized.startsWith("..")
                    && !normalized.toString().isBlank();
        } catch (java.nio.file.InvalidPathException exception) {
            return false;
        }
    }

    private boolean validText(byte[] bytes) {
        if (bytes.length == 0) {
            return false;
        }
        for (byte value : bytes) {
            if (value == 0) {
                return false;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    private boolean startsWith(byte[] bytes, int[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((bytes[index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean ascii(byte[] bytes, int offset, String expected) {
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index] != (byte) expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private List<Long> normalizeIds(List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }
        if (attachmentIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw invalid("交付附件标识不正确");
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>(attachmentIds);
        if (unique.size() > properties.maxFileCount()) {
            throw invalid("每次交付最多绑定 " + properties.maxFileCount() + " 个附件");
        }
        return new ArrayList<>(unique);
    }

    private RequestEntity findVisibleRequest(Long requestId, LoginUser viewer) {
        requireAuthenticated(viewer);
        RequestEntity request = findRequest(requestId);
        assertVisible(request, viewer);
        return request;
    }

    private void assertVisible(RequestEntity request, LoginUser viewer) {
        switch (viewer.role()) {
            case REQUESTER -> {
                if (!Objects.equals(request.getCreatorId(), viewer.id())) {
                    throw hiddenRequest();
                }
            }
            case MEMBER -> {
                if (request.getStatus() == RequestStatus.DRAFT) {
                    throw hiddenRequest();
                }
            }
            case ADMIN -> { }
            default -> throw hiddenRequest();
        }
    }

    private boolean canDelete(
            AttachmentEntity entity,
            RequestEntity request,
            LoginUser viewer) {
        if (entity.getBusinessType() == AttachmentBusinessType.REQUEST) {
            return Objects.equals(entity.getBusinessId(), request.getId())
                    && canManageRequestAttachment(request, viewer)
                    && requestAttachmentStatusAllowsChange(request);
        }
        return entity.getBusinessType() == AttachmentBusinessType.DELIVERY
                && entity.getBusinessId() == null
                && Objects.equals(entity.getUploaderId(), viewer.id());
    }

    private boolean canManageRequestAttachment(
            RequestEntity request,
            LoginUser viewer) {
        return viewer.role() == UserRole.ADMIN
                || (viewer.role() == UserRole.REQUESTER
                        && Objects.equals(request.getCreatorId(), viewer.id()));
    }

    private boolean requestAttachmentStatusAllowsChange(RequestEntity request) {
        return request.getStatus() == RequestStatus.PENDING_REVIEW
                || request.getStatus() == RequestStatus.NEED_MORE_INFO;
    }

    private RequestEntity findRequest(Long requestId) {
        if (requestId == null || requestId <= 0) {
            throw hiddenRequest();
        }
        RequestEntity request = requestMapper.selectById(requestId);
        if (request == null) {
            throw hiddenRequest();
        }
        return request;
    }

    private void requireAuthenticated(LoginUser user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private AttachmentVO toVO(
            AttachmentEntity entity, boolean canDelete, String uploaderName) {
        return new AttachmentVO(entity.getId().toString(), entity.getRequestId().toString(),
                entity.getBusinessType(), entity.getBusinessId() == null
                        ? null : entity.getBusinessId().toString(),
                entity.getOriginalName(), entity.getContentType(), entity.getSizeBytes(),
                entity.getUploaderId().toString(), uploaderName, entity.getCreatedAt(), canDelete);
    }

    private Map<Long, String> loadUploaderNames(List<AttachmentEntity> entities) {
        Set<Long> ids = entities.stream().map(AttachmentEntity::getUploaderId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectAssignmentUsersByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        UserEntity::getId, UserEntity::getDisplayName));
    }

    private String loadUploaderName(Long uploaderId) {
        if (uploaderId == null) {
            return "未知用户";
        }
        UserEntity user = userMapper.selectById(uploaderId);
        return user == null ? "未知用户" : user.getDisplayName();
    }

    private void registerRollbackCleanup(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(storageKey);
                }
            }
        });
    }

    private void deleteAfterCommit(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(storageKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(storageKey);
            }
        });
    }

    private void deleteQuietly(String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (IOException exception) {
            log.error("Failed to remove attachment content: {}", storageKey, exception);
        }
    }

    private BusinessException hiddenRequest() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "需求不存在");
    }

    private BusinessException hiddenAttachment() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "附件不存在");
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private record ValidatedFile(String originalName, String contentType) {}
}
