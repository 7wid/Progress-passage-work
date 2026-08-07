package cn.edu.techgroup.outsourcing.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginCommand(
        @NotBlank @Size(max = 64) String account,
        @NotBlank @Size(max = 128) String password) {}
