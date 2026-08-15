package cn.edu.techgroup.outsourcing.bootstrap;

import cn.edu.techgroup.outsourcing.config.BootstrapAdminProperties;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(
        prefix = "app.bootstrap-admin",
        name = "enabled",
        havingValue = "true")
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(BootstrapAdminInitializer.class);
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    public BootstrapAdminInitializer(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            BootstrapAdminProperties properties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String account = normalizeRequired(properties.account(), "初始管理员账号不能为空");
        validateAccount(account);

        UserEntity existing = userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getAccount, account));

        if (existing != null) {
            if (existing.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException(
                        "初始管理员账号已经存在，但其角色不是 ADMIN");
            }
            log.info("初始管理员账号已存在，跳过创建：{}", account);
            return;
        }

        String displayName = normalizeRequired(
                properties.displayName(),
                "首次创建管理员时，显示名称不能为空");
        String password = properties.password();
        validateDisplayName(displayName);
        validatePassword(password);

        UserEntity admin = new UserEntity();
        admin.setAccount(account);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName(displayName);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setFailedLoginCount(0);

        if (userMapper.insert(admin) != 1) {
            throw new IllegalStateException("初始管理员创建失败");
        }
        log.info("已创建初始管理员账号：{}", account);
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private void validateAccount(String account) {
        if (account.length() > 64 || !account.matches("^[A-Za-z0-9._-]+$")) {
            throw new IllegalStateException(
                    "初始管理员账号只能包含字母、数字、点、下划线和连字符，且不能超过 64 个字符");
        }
    }

    private void validateDisplayName(String displayName) {
        if (displayName.length() > 80) {
            throw new IllegalStateException("初始管理员显示名称不能超过 80 个字符");
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_BCRYPT_PASSWORD_BYTES
                || password.getBytes(StandardCharsets.UTF_8).length
                        > MAX_BCRYPT_PASSWORD_BYTES
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw new IllegalStateException(
                    "初始管理员密码需为 12～72 个字符、包含字母和数字，且 UTF-8 编码不超过 72 字节");
        }
    }
}
