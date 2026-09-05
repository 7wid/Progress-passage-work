package cn.edu.techgroup.outsourcing.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserCommand(
        @NotBlank(message = "请输入账号")
        @Size(min = 3, max = 64, message = "账号长度应为 3～64 个字符")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*[A-Za-z0-9]$",
                message = "账号须以字母或数字开头和结尾，可包含点、下划线和连字符")
        String account,
        @NotBlank(message = "请输入密码")
        @Size(min = 8, max = 72, message = "密码长度应为 8～72 个字符")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String password,
        @NotBlank(message = "请输入姓名或称呼")
        @Size(max = 80, message = "姓名或称呼不能超过 80 个字符")
        String displayName,
        @NotBlank(message = "请输入邮箱")
        @Email(message = "请输入有效邮箱地址")
        @Size(max = 160, message = "邮箱不能超过 160 个字符")
        String email,
        @Size(max = 32, message = "手机号不能超过 32 个字符")
        @Pattern(
                regexp = "^$|^\\+?[0-9()\\- ]{6,32}$",
                message = "请输入有效手机号")
        String phone,
        @Size(max = 160, message = "院系或组织不能超过 160 个字符")
        String department) {

    public RegisterUserCommand {
        account = account == null ? null : account.trim();
        displayName = displayName == null ? null : displayName.trim();
        email = email == null ? null : email.trim();
        phone = trimToNull(phone);
        department = trimToNull(department);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
