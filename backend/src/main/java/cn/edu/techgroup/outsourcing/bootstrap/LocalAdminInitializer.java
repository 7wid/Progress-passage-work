package cn.edu.techgroup.outsourcing.bootstrap;

import cn.edu.techgroup.outsourcing.config.BootstrapAdminProperties;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Profile("local")
public class LocalAdminInitializer implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(LocalAdminInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    public LocalAdminInitializer(
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
    if (!properties.enabled()) {
        log.info("本地管理员初始化已关闭");
        return;
    }

    if (!StringUtils.hasText(properties.account())) {
        throw new IllegalStateException("本地管理员账号不能为空");
    }

    String account = properties.account().trim();

    UserEntity existing = userMapper.selectOne(
            Wrappers.<UserEntity>lambdaQuery()
                    .eq(UserEntity::getAccount, account));

    if (existing != null) {
        if (existing.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException(
                    "初始管理员账号已经存在，但其角色不是 ADMIN");
        }

        log.info("本地管理员账号已存在，跳过创建：{}", account);
        return;
    }

    // 只有第一次真正创建管理员时才要求密码和显示名称。
    if (!StringUtils.hasText(properties.password())
            || !StringUtils.hasText(properties.displayName())) {
        throw new IllegalStateException(
                "首次创建本地管理员时，必须提供密码和显示名称");
    }

    String password = properties.password();
    String displayName = properties.displayName().trim();

    if (password.length() < 12) {
        throw new IllegalStateException(
                "本地管理员密码不得少于 12 个字符");
    }

    UserEntity admin = new UserEntity();
    admin.setAccount(account);
    admin.setPasswordHash(passwordEncoder.encode(password));
    admin.setDisplayName(displayName);
    admin.setRole(UserRole.ADMIN);
    admin.setStatus(UserStatus.ACTIVE);
    admin.setFailedLoginCount(0);

    int insertedRows = userMapper.insert(admin);
    if (insertedRows != 1) {
        throw new IllegalStateException("本地管理员创建失败");
    }

    log.info("已创建本地管理员账号：{}", account);
}
}