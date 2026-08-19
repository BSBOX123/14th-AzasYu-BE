package com.azasyu.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.azasyu.domain.auth.dto.LoginRequest;
import com.azasyu.domain.auth.dto.SignUpRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 가입 이메일이 최상위 도메인을 갖추도록 강제하는지 확인한다.
 *
 * <p>기본 {@code @Email}은 도메인에 점을 요구하지 않아 {@code navercom}, {@code gmailcom},
 * {@code localhost}가 전부 통과함. 그래서 {@code SignUpRequest}에만 regexp를 덧붙였음.
 * 로그인은 규칙을 조이기 전에 가입한 계정을 막지 않으려고 기본 검사를 그대로 둠.
 */
class EmailFormatValidationTest {

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

    @ParameterizedTest
    @ValueSource(strings = {
        "hong@navercom",        // 신고된 값 — 점이 없음
        "hong@gmailcom",        // 신고된 값
        "hong@naver",
        "hong@naver.c",         // TLD가 한 글자
        "hong@localhost",       // 단일 호스트명
        "hong@192.168.0.1",     // TLD 자리가 숫자
        "hong@naver..com",
        "hong@.com",
        "hong@-naver.com",
        "@naver.com",
        "hong@",
        "not-an-email"
    })
    void rejectsMalformedSignUpEmail(String email) {
        assertThat(signUpEmailMessage(email))
            .as("%s 은 가입을 거부해야 한다", email)
            .isEqualTo("이메일 형식이 올바르지 않습니다");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "hong@example.com",
        "hong@naver.co.kr",
        "hong.gil-dong+tag@example.com",
        "HONG@EXAMPLE.COM"
    })
    void acceptsWellFormedSignUpEmail(String email) {
        assertThat(violations(new SignUpRequest(email, "홍길동", "password123")))
            .as("%s 은 가입을 허용해야 한다", email)
            .isNull();
    }

    @Test
    void loginStillAcceptsEmailWithoutTopLevelDomain() {
        var request = new LoginRequest("hong@navercom", "password123");

        assertThat(violations(request))
            .as("규칙을 조이기 전에 가입한 계정이 로그인하지 못하면 안 된다")
            .isNull();
    }

    private String signUpEmailMessage(String email) {
        return violations(new SignUpRequest(email, "홍길동", "password123"));
    }

    private <T> String violations(T request) {
        return validator.validate(request).stream()
            .filter(violation -> violation.getPropertyPath().toString().equals("email"))
            .map(ConstraintViolation::getMessage)
            .findFirst()
            .orElse(null);
    }
}
