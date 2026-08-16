package cn.edu.techgroup.outsourcing.modules.user.service;

import cn.edu.techgroup.outsourcing.modules.user.dto.ChangePasswordCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMyProfileCommand;
import cn.edu.techgroup.outsourcing.modules.user.vo.PasswordChangeResultVO;
import cn.edu.techgroup.outsourcing.modules.user.vo.UserProfileVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface UserProfileService {

    UserProfileVO get(LoginUser operator);

    UserProfileVO update(UpdateMyProfileCommand command, LoginUser operator);

    PasswordChangeResultVO changePassword(
            ChangePasswordCommand command,
            String currentSessionId,
            LoginUser operator);
}
