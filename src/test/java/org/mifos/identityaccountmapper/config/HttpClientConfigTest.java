package org.mifos.identityaccountmapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * RestAssured followed redirects on its own, the JDK client does not. This starts a small server that answers 302 once,
 * so the swap cannot quietly drop a callback whose target moved.
 */
class HttpClientConfigTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().add("Location", "/here");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/here", exchange -> {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void followsARedirectTheWayRestAssuredDid() throws Exception {
        RestClient restClient = new HttpClientConfig().restClient(new HttpClientProperties(true, Duration.ofSeconds(5)));

        ResponseEntity<String> response = restClient.get().uri(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/moved"))
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("ok");
    }
}
