package com.azasyu.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = "app.gemini.read-timeout=300ms")
class GeminiClientConfigTest {

    private static final byte[] RESPONSE_BODY = "{}".getBytes();
    private static final Duration SERVER_DELAY = Duration.ofSeconds(3);

    @Autowired
    private RestClient geminiRestClient;

    private HttpServer slowServer;

    @BeforeEach
    void startSlowServer() throws Exception {
        slowServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slowServer.createContext("/slow", exchange -> {
            try {
                Thread.sleep(SERVER_DELAY.toMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, RESPONSE_BODY.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(RESPONSE_BODY);
            }
        });
        slowServer.start();
    }

    @AfterEach
    void stopSlowServer() {
        slowServer.stop(0);
    }

    @Test
    void readTimeoutAbortsSlowResponse() {
        String url = "http://127.0.0.1:" + slowServer.getAddress().getPort() + "/slow";

        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> geminiRestClient.get().uri(url).retrieve().body(String.class))
            .isInstanceOf(ResourceAccessException.class);
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        // 서버는 3초를 지연시키지만 read-timeout(300ms)이 먼저 끊어야 한다.
        assertThat(elapsedMillis).isLessThan(SERVER_DELAY.toMillis());
    }
}
