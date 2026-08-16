package cn.edu.techgroup.outsourcing.modules.user.event;

public record PasswordChangedEvent(String account, String currentSessionId) {
}
