package com.azasyu.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.azasyu.domain.auth.dto.SignUpRequest;
import com.azasyu.domain.project.dto.CreateProjectRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 검증 실패 메시지가 사용자에게 보여줄 수 있는 문구인지 확인한다.
 *
 * <p>{@code message}를 지정하지 않으면 "크기가 8에서 72 사이여야 합니다" 같은 기본 문구가 나간다.
 * {@code @Size}의 기본 메시지는 문자열과 컬렉션이 공용이라 "길이" 대신 "크기"를 쓴다.
 */
class ValidationMessageTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void shortPasswordReportsLengthNotSize() {
        var request = new SignUpRequest("user@example.com", "홍길동", "short");

        String message = messageOf(validator.validate(request), "password");

        assertThat(message).isEqualTo("비밀번호는 8자 이상 72자 이하로 입력해 주세요");
        assertThat(message).as("기본 문구가 그대로 나가면 안 된다").doesNotContain("크기");
    }

    @Test
    void longProjectNameReportsFriendlyMessage() {
        var request = new CreateProjectRequest("가".repeat(21), "설명");

        String message = messageOf(validator.validate(request), "name");

        assertThat(message).isEqualTo("프로젝트 이름은 20자 이하로 입력해 주세요");
        assertThat(message).doesNotContain("크기");
    }

    @Test
    void invalidEmailReportsFriendlyMessage() {
        var request = new SignUpRequest("not-an-email", "홍길동", "password123");

        assertThat(messageOf(validator.validate(request), "email"))
            .isEqualTo("이메일 형식이 올바르지 않습니다");
    }

    private <T> String messageOf(Set<ConstraintViolation<T>> violations, String field) {
        return violations.stream()
            .filter(violation -> violation.getPropertyPath().toString().equals(field))
            .map(ConstraintViolation::getMessage)
            .findFirst()
            .orElseThrow(() -> new AssertionError(field + " 검증이 실패하지 않았습니다"));
    }
}
