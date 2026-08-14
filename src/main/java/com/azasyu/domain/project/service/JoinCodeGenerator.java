package com.azasyu.domain.project.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

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
