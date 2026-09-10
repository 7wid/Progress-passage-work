package cn.edu.techgroup.outsourcing.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordCommand(
        @NotBlank(message = "缺少重置凭证")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "重置凭证格式无效") String token,
        @NotBlank(message = "请输入新密码")
        @Size(min = 8, max = 72, message = "密码长度应为 8～72 个字符") String newPassword) {}
