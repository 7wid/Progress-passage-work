package cn.edu.techgroup.outsourcing.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record ForgotPasswordCommand(
        @NotBlank(message = "请输入绑定邮箱")
        @Email(message = "请输入有效邮箱")
        @Size(max = 160, message = "邮箱不能超过 160 个字符") String email) {
    public ForgotPasswordCommand {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
