package cn.edu.techgroup.outsourcing.security;

import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public CustomUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getAccount, username));
        if (user == null) {
            throw new UsernameNotFoundException("Invalid account or password");
        }
        boolean accountNonLocked =
                user.getLockedUntil() == null || user.getLockedUntil().isBefore(Instant.now());
        return new LoginUser(
                user.getId(),
                user.getAccount(),
                user.getPasswordHash(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus() == UserStatus.ACTIVE,
                accountNonLocked);
    }
}
