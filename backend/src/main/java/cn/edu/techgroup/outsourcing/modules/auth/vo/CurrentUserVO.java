package cn.edu.techgroup.outsourcing.modules.auth.vo;

import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public record CurrentUserVO(
        String id,
        String account,
        String displayName,
        UserRole role) {

    public static CurrentUserVO from(LoginUser user) {
        return new CurrentUserVO(
                user.id().toString(),
                user.account(),
                user.displayName(),
                user.role());
    }
}
