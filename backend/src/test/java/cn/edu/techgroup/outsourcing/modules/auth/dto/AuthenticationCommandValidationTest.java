package cn.edu.techgroup.outsourcing.modules.auth.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.edu.techgroup.outsourcing.modules.user.dto.RegisterUserCommand;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class AuthenticationCommandValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void normalizesLoginAccountButPreservesPassword() {
        LoginCommand command = new LoginCommand(" ADMIN ", " Password1 ");

        assertEquals("admin", command.account());
        assertEquals(" Password1 ", command.password());
        assertTrue(VALIDATOR.validate(command).isEmpty());
    }

    @Test
    void rejectsRegistrationAccountWithUnstableDelimiter() {
        RegisterUserCommand command = command(".student", "Password1", null);

        assertFalse(VALIDATOR.validate(command).isEmpty());
    }

    @Test
    void acceptsInternationalPhoneSyntaxAndRejectsLetters() {
        assertTrue(VALIDATOR.validate(command("student01", "Password1", "+86 138-0000-0000"))
                .isEmpty());
        assertFalse(VALIDATOR.validate(command("student01", "Password1", "call-me"))
                .isEmpty());
    }

    private RegisterUserCommand command(String account, String password, String phone) {
        return new RegisterUserCommand(
                account,
                password,
                "学生用户",
                "student@example.edu.cn",
                phone,
                "计算机学院");
    }
}
