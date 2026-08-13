package com.azasyu.global.ai;

import com.azasyu.global.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiClientConfig {

    @Bean
    RestClient geminiRestClient(AppProperties properties) {
        AppProperties.Gemini gemini = properties.gemini();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(gemini.connectTimeout());
        requestFactory.setReadTimeout(gemini.readTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
