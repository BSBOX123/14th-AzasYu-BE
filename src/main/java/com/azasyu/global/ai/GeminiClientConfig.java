package com.azasyu.global.ai;

import com.azasyu.global.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Gemini 호출 전용 RestClient 설정.
 *
 * <p>타임아웃이 없으면 Gemini가 응답하지 않을 때 요청 스레드가 무한정 점유됨.
 * 현장 네트워크 상황에 맞춰 환경변수로 조정할 수 있도록 설정값으로 뺌.
 */
@Configuration
public class GeminiClientConfig {

    /** 연결·읽기 타임아웃을 적용한 RestClient. 값은 {@code app.gemini.*} 설정을 따름. */
    @Bean
    RestClient geminiRestClient(AppProperties properties) {
        AppProperties.Gemini gemini = properties.gemini();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(gemini.connectTimeout());
        requestFactory.setReadTimeout(gemini.readTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
