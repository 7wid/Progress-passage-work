package cn.edu.techgroup.outsourcing.modules.user.service;

import cn.edu.techgroup.outsourcing.modules.user.dto.RegisterUserCommand;
import cn.edu.techgroup.outsourcing.modules.user.vo.RegistrationStatusVO;
import cn.edu.techgroup.outsourcing.modules.user.vo.UserProfileVO;

public interface UserRegistrationService {

    RegistrationStatusVO status();

    UserProfileVO register(RegisterUserCommand command);
}
