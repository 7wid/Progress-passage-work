package cn.edu.techgroup.outsourcing.modules.user.service.impl;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.user.dto.AdminMemberListQuery;
import cn.edu.techgroup.outsourcing.modules.user.dto.CreateMemberCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMemberCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMemberStatusCommand;
import cn.edu.techgroup.outsourcing.modules.user.entity.SkillTagEntity;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.event.MemberAccessChangedPublisher;
import cn.edu.techgroup.outsourcing.modules.user.mapper.SkillTagMapper;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserActiveOwnerCount;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserSkillMapper;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserSkillRow;
import cn.edu.techgroup.outsourcing.modules.user.service.AdminMemberService;
import cn.edu.techgroup.outsourcing.modules.user.vo.AdminMemberVO;
import cn.edu.techgroup.outsourcing.modules.user.vo.SkillTagVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMemberServiceImpl implements AdminMemberService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SKILL_COUNT = 20;
    private static final int MIN_REASON_LENGTH = 5;
    private static final int MAX_REASON_LENGTH = 500;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserMapper userMapper;
    private final SkillTagMapper skillTagMapper;
    private final UserSkillMapper userSkillMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;
    private final MemberAccessChangedPublisher accessChangedPublisher;

    public AdminMemberServiceImpl(
            UserMapper userMapper,
            SkillTagMapper skillTagMapper,
            UserSkillMapper userSkillMapper,
            PasswordEncoder passwordEncoder,
            AuditRecorder auditRecorder,
            MemberAccessChangedPublisher accessChangedPublisher) {
        this.userMapper = userMapper;
        this.skillTagMapper = skillTagMapper;
        this.userSkillMapper = userSkillMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
        this.accessChangedPublisher = accessChangedPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminMemberVO> list(
            AdminMemberListQuery query,
            LoginUser operator) {
        requireAdmin(operator);
        validateListQuery(query);

        var wrapper = Wrappers.<UserEntity>lambdaQuery()
                .in(UserEntity::getRole, List.of(UserRole.MEMBER, UserRole.ADMIN));
        if (query.keyword() != null) {
            wrapper.and(filter -> filter
                    .like(UserEntity::getAccount, query.keyword())
                    .or()
                    .like(UserEntity::getDisplayName, query.keyword()));
        }
        if (query.role() != null) {
            requireTechnicalRole(query.role());
            wrapper.eq(UserEntity::getRole, query.role());
        }
        if (query.status() != null) {
            wrapper.eq(UserEntity::getStatus, query.status());
        }
        wrapper.orderByAsc(UserEntity::getDisplayName)
                .orderByAsc(UserEntity::getId);

        Page<UserEntity> result = userMapper.selectPage(
                new Page<>(query.page(), query.pageSize()),
                wrapper);
        return PageResponse.of(
                toVOs(result.getRecords()),
                result.getCurrent(),
                result.getSize(),
                result.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMemberVO get(Long memberId, LoginUser operator) {
        requireAdmin(operator);
        UserEntity member = findTechnicalMember(memberId);
        return toVO(
                member,
                skillsByUserIds(List.of(memberId))
                        .getOrDefault(memberId, List.of()),
                userMapper.countActiveOwnerRequests(memberId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillTagVO> skillTags(LoginUser operator) {
        requireAdmin(operator);
        return skillTagMapper.selectList(
                        Wrappers.<SkillTagEntity>lambdaQuery()
                                .eq(SkillTagEntity::getEnabled, true)
                                .orderByAsc(SkillTagEntity::getName)
                                .orderByAsc(SkillTagEntity::getId))
                .stream()
                .map(SkillTagVO::from)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminMemberVO create(
            CreateMemberCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validateCreateCommand(command);
        String reason = normalizeReason(command.reason());
        requireTechnicalRole(command.role());
        validatePassword(command.initialPassword());
        Set<Long> skillIds = validateSkills(command.skillIds());

        UserEntity member = new UserEntity();
        member.setAccount(command.account().trim().toLowerCase(Locale.ROOT));
        member.setPasswordHash(passwordEncoder.encode(command.initialPassword()));
        member.setDisplayName(command.displayName().trim());
        member.setEmail(normalizeEmail(command.email()));
        member.setPhone(trimToNull(command.phone()));
        member.setDepartment(trimToNull(command.department()));
        member.setRole(command.role());
        member.setStatus(UserStatus.ACTIVE);
        member.setFailedLoginCount(0);
        member.setCreatedAt(now());
        member.setUpdatedAt(member.getCreatedAt());

        try {
            if (userMapper.insert(member) != 1 || member.getId() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
            replaceSkills(member.getId(), skillIds);
        } catch (DuplicateKeyException exception) {
            throw duplicateMember();
        }

        auditRecorder.record(
                operator.id(),
                "MEMBER_CREATE",
                "USER",
                member.getId().toString(),
                null,
                snapshotWithReason(member, skillIds, reason));
        return toVO(member, loadSkillVOs(skillIds), 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminMemberVO update(
            Long memberId,
            UpdateMemberCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validateUpdateCommand(command);
        String reason = normalizeReason(command.reason());
        List<UserEntity> lockedUsers =
                userMapper.selectTargetAndActiveAdminsForUpdate(memberId);
        UserEntity member = findTechnicalMemberInLockedRows(
                memberId,
                lockedUsers);
        long activeAdminCount = activeAdminCount(lockedUsers);
        requireExpectedUpdatedAt(member, command.expectedUpdatedAt());
        requireTechnicalRole(command.role());

        if (Objects.equals(memberId, operator.id())
                && member.getRole() != command.role()) {
            throw accessDenied("不能修改自己的管理员角色");
        }
        if (member.getRole() == UserRole.ADMIN
                && member.getStatus() == UserStatus.ACTIVE
                && command.role() != UserRole.ADMIN
                && activeAdminCount <= 1) {
            throw conflict("系统必须至少保留一名启用管理员");
        }

        Set<Long> skillIds = validateSkills(command.skillIds());
        Set<Long> oldSkillIds = currentSkillIds(memberId);
        Map<String, Object> before = snapshot(member, oldSkillIds);
        boolean accessChanged = member.getRole() != command.role()
                || !Objects.equals(
                        member.getDisplayName(),
                        command.displayName().trim());

        member.setDisplayName(command.displayName().trim());
        member.setEmail(normalizeEmail(command.email()));
        member.setPhone(trimToNull(command.phone()));
        member.setDepartment(trimToNull(command.department()));
        member.setRole(command.role());
        member.setUpdatedAt(nextUpdatedAt(member.getUpdatedAt()));

        try {
            if (userMapper.updateAdminProfile(member) != 1) {
                throw conflict("成员已被其他管理员更新");
            }
            replaceSkills(memberId, skillIds);
        } catch (DuplicateKeyException exception) {
            throw duplicateMember();
        }

        auditRecorder.record(
                operator.id(),
                "MEMBER_UPDATE",
                "USER",
                memberId.toString(),
                before,
                snapshotWithReason(member, skillIds, reason));
        if (accessChanged) {
            accessChangedPublisher.publish(member.getAccount());
        }
        return toVO(
                member,
                loadSkillVOs(skillIds),
                userMapper.countActiveOwnerRequests(memberId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminMemberVO updateStatus(
            Long memberId,
            UpdateMemberStatusCommand command,
            LoginUser operator) {
        requireAdmin(operator);
        validateStatusCommand(command);
        String reason = normalizeReason(command.reason());

        // Assignment also locks request before user. Keeping the same order
        // prevents a newly assigned active request from ending with a disabled owner.
        if (command.status() == UserStatus.DISABLED) {
            userMapper.lockActiveOwnerRequestIds(memberId);
        }
        List<UserEntity> lockedUsers =
                userMapper.selectTargetAndActiveAdminsForUpdate(memberId);
        UserEntity member = findTechnicalMemberInLockedRows(
                memberId,
                lockedUsers);
        long activeAdminCount = activeAdminCount(lockedUsers);
        requireExpectedUpdatedAt(member, command.expectedUpdatedAt());

        if (Objects.equals(memberId, operator.id())
                && command.status() == UserStatus.DISABLED) {
            throw accessDenied("不能停用自己的账号");
        }
        if (member.getStatus() == command.status()) {
            return toVO(
                    member,
                    skillsByUserIds(List.of(memberId))
                            .getOrDefault(memberId, List.of()),
                    userMapper.countActiveOwnerRequests(memberId));
        }

        long activeOwnerRequestCount =
                userMapper.countActiveOwnerRequests(memberId);
        if (command.status() == UserStatus.DISABLED
                && activeOwnerRequestCount > 0) {
            throw conflict("该成员仍负责未结束需求，请先转交负责人");
        }
        if (member.getRole() == UserRole.ADMIN
                && member.getStatus() == UserStatus.ACTIVE
                && command.status() == UserStatus.DISABLED
                && activeAdminCount <= 1) {
            throw conflict("系统必须至少保留一名启用管理员");
        }

        Set<Long> skillIds = currentSkillIds(memberId);
        Map<String, Object> before = snapshot(member, skillIds);
        member.setStatus(command.status());
        if (command.status() == UserStatus.ACTIVE) {
            member.setFailedLoginCount(0);
            member.setLockedUntil(null);
        }
        member.setUpdatedAt(nextUpdatedAt(member.getUpdatedAt()));
        if (userMapper.updateAdminStatus(member) != 1) {
            throw conflict("成员已被其他管理员更新");
        }

        auditRecorder.record(
                operator.id(),
                "MEMBER_STATUS",
                "USER",
                memberId.toString(),
                before,
                snapshotWithReason(member, skillIds, reason));
        accessChangedPublisher.publish(member.getAccount());
        return toVO(
                member,
                skillsByUserIds(List.of(memberId))
                        .getOrDefault(memberId, List.of()),
                activeOwnerRequestCount);
    }

    private List<AdminMemberVO> toVOs(List<UserEntity> members) {
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = members.stream()
                .map(UserEntity::getId)
                .toList();
        Map<Long, List<SkillTagVO>> skills = skillsByUserIds(userIds);
        Map<Long, Long> ownerCounts = userMapper.selectActiveOwnerCounts(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserActiveOwnerCount::userId,
                        UserActiveOwnerCount::activeCount));
        return members.stream()
                .map(member -> toVO(
                        member,
                        skills.getOrDefault(member.getId(), List.of()),
                        ownerCounts.getOrDefault(member.getId(), 0L)))
                .toList();
    }

    private Map<Long, List<SkillTagVO>> skillsByUserIds(
            Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userSkillMapper.selectByUserIds(userIds)
                .stream()
                .collect(Collectors.groupingBy(
                        UserSkillRow::userId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                row -> new SkillTagVO(
                                        row.skillId().toString(),
                                        row.skillName()),
                                Collectors.toList())));
    }

    private AdminMemberVO toVO(
            UserEntity member,
            List<SkillTagVO> skills,
            long activeOwnerRequestCount) {
        return new AdminMemberVO(
                member.getId().toString(),
                member.getAccount(),
                member.getDisplayName(),
                member.getEmail(),
                member.getPhone(),
                member.getDepartment(),
                member.getRole(),
                member.getStatus(),
                skills,
                activeOwnerRequestCount,
                member.getCreatedAt(),
                member.getUpdatedAt());
    }

    private UserEntity findTechnicalMember(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw hiddenMember();
        }
        UserEntity member = userMapper.selectById(memberId);
        if (!isTechnicalMember(member)) {
            throw hiddenMember();
        }
        return member;
    }

    private UserEntity findTechnicalMemberInLockedRows(
            Long memberId,
            List<UserEntity> lockedUsers) {
        if (memberId == null || memberId <= 0) {
            throw hiddenMember();
        }
        UserEntity member = lockedUsers.stream()
                .filter(user -> Objects.equals(user.getId(), memberId))
                .findFirst()
                .orElse(null);
        if (!isTechnicalMember(member)) {
            throw hiddenMember();
        }
        return member;
    }

    private long activeAdminCount(List<UserEntity> lockedUsers) {
        return lockedUsers.stream()
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .count();
    }

    private boolean isTechnicalMember(UserEntity member) {
        return member != null
                && (member.getRole() == UserRole.MEMBER
                        || member.getRole() == UserRole.ADMIN);
    }

    private Set<Long> validateSkills(List<Long> skillIds) {
        if (skillIds == null
                || skillIds.size() > MAX_SKILL_COUNT
                || skillIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw invalidArgument("技能标签参数不正确");
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(skillIds);
        if (uniqueIds.size() != skillIds.size()) {
            throw invalidArgument("技能标签不能重复");
        }
        if (uniqueIds.isEmpty()) {
            return uniqueIds;
        }
        Map<Long, SkillTagEntity> found = skillTagMapper.selectByIds(uniqueIds)
                .stream()
                .collect(Collectors.toMap(SkillTagEntity::getId, Function.identity()));
        if (found.size() != uniqueIds.size()
                || uniqueIds.stream().anyMatch(id ->
                        !Boolean.TRUE.equals(found.get(id).getEnabled()))) {
            throw invalidArgument("技能标签不存在或已停用");
        }
        return uniqueIds;
    }

    private void replaceSkills(Long userId, Set<Long> skillIds) {
        userSkillMapper.deleteByUserId(userId);
        for (Long skillId : skillIds) {
            if (userSkillMapper.insert(userId, skillId) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    private Set<Long> currentSkillIds(Long userId) {
        return skillsByUserIds(List.of(userId))
                .getOrDefault(userId, List.of())
                .stream()
                .map(skill -> Long.valueOf(skill.id()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<SkillTagVO> loadSkillVOs(Set<Long> skillIds) {
        if (skillIds.isEmpty()) {
            return List.of();
        }
        return skillTagMapper.selectByIds(skillIds)
                .stream()
                .sorted(Comparator
                        .comparing(SkillTagEntity::getName)
                        .thenComparing(SkillTagEntity::getId))
                .map(SkillTagVO::from)
                .toList();
    }

    private Map<String, Object> snapshot(
            UserEntity member,
            Collection<Long> skillIds) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("account", member.getAccount());
        snapshot.put("displayName", member.getDisplayName());
        snapshot.put("role", member.getRole());
        snapshot.put("status", member.getStatus());
        snapshot.put("skillIds", skillIds);
        return snapshot;
    }

    private Map<String, Object> snapshotWithReason(
            UserEntity member,
            Collection<Long> skillIds,
            String reason) {
        Map<String, Object> snapshot = snapshot(member, skillIds);
        snapshot.put("reason", reason);
        return snapshot;
    }

    private void requireExpectedUpdatedAt(
            UserEntity member,
            Instant expectedUpdatedAt) {
        if (expectedUpdatedAt == null
                || member.getUpdatedAt() == null
                || !member.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS)
                        .equals(expectedUpdatedAt.truncatedTo(ChronoUnit.MILLIS))) {
            throw dataVersionConflict("成员已被其他管理员更新，请刷新后重试");
        }
    }

    private Instant nextUpdatedAt(Instant oldUpdatedAt) {
        Instant current = now();
        if (oldUpdatedAt == null) {
            return current;
        }
        Instant old = oldUpdatedAt.truncatedTo(ChronoUnit.MILLIS);
        return current.isAfter(old) ? current : old.plusMillis(1);
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private String normalizeEmail(String value) {
        String email = trimToNull(value);
        return email == null ? null : email.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            throw invalidArgument("操作原因不能为空");
        }
        String normalized = reason.trim();
        if (normalized.length() < MIN_REASON_LENGTH
                || normalized.length() > MAX_REASON_LENGTH) {
            throw invalidArgument("操作原因应为 5～500 个字符");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_BCRYPT_PASSWORD_BYTES
                || password.getBytes(StandardCharsets.UTF_8).length
                        > MAX_BCRYPT_PASSWORD_BYTES
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw invalidArgument(
                    "初始密码需为 8～72 个字符、包含字母和数字，且 UTF-8 编码不超过 72 字节");
        }
    }

    private void validateListQuery(AdminMemberListQuery query) {
        if (query == null
                || query.page() == null
                || query.pageSize() == null
                || query.page() < 1
                || query.pageSize() < 1
                || query.pageSize() > MAX_PAGE_SIZE) {
            throw invalidArgument("分页参数不正确");
        }
    }

    private void validateCreateCommand(CreateMemberCommand command) {
        if (command == null
                || command.account() == null
                || command.displayName() == null) {
            throw invalidArgument("成员参数不正确");
        }
    }

    private void validateUpdateCommand(UpdateMemberCommand command) {
        if (command == null
                || command.displayName() == null
                || command.role() == null) {
            throw invalidArgument("成员参数不正确");
        }
    }

    private void validateStatusCommand(UpdateMemberStatusCommand command) {
        if (command == null || command.status() == null) {
            throw invalidArgument("成员状态参数不正确");
        }
    }

    private void requireTechnicalRole(UserRole role) {
        if (role != UserRole.MEMBER && role != UserRole.ADMIN) {
            throw invalidArgument("只能管理技术组成员或管理员");
        }
    }

    private void requireAdmin(LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (operator.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private BusinessException invalidArgument(String message) {
        return new BusinessException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private BusinessException duplicateMember() {
        return new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "账号或邮箱已存在");
    }

    private BusinessException hiddenMember() {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "成员不存在");
    }

    private BusinessException accessDenied(String message) {
        return new BusinessException(ErrorCode.ACCESS_DENIED, message);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(
                ErrorCode.REQUEST_STATUS_CONFLICT,
                message);
    }

    private BusinessException dataVersionConflict(String message) {
        return new BusinessException(
                ErrorCode.DATA_VERSION_CONFLICT,
                message);
    }
}
