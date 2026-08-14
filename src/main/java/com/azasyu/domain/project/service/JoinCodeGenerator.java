package com.azasyu.domain.project.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 프로젝트 참여 코드 생성기.
 *
 * <p>육안으로 혼동하기 쉬운 문자(I, O, 0, 1)를 뺀 32자 알파벳으로 8자리 코드를 만듦.
 * 중복 여부는 호출부에서 확인함.
 */
@Component
public class JoinCodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }
}
