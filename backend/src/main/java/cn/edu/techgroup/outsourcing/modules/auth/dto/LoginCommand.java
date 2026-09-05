package cn.edu.techgroup.outsourcing.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginCommand(
        @NotBlank(message = "请输入账号")
        @Size(max = 64, message = "账号不能超过 64 个字符")
        String account,
        @NotBlank(message = "请输入密码")
        @Size(max = 128, message = "密码格式不正确")
        String password) {

    public LoginCommand {
        account = account == null ? null : account.trim().toLowerCase(Locale.ROOT);
    }
}
