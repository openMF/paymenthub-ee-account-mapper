package org.mifos.identityaccountmapper.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The one HTTP client this service uses for its outgoing calls.
 *
 * <p>
 * It replaces the RestAssured specification that both call sites used to build inside the request. RestAssured is a
 * testing library and was on the runtime classpath, a new specification was created per call so no connection was ever
 * reused, and {@code relaxedHTTPSValidation()} turned certificate checking off with no way to turn it back on. The
 * client is built once here, and the trust-all behaviour is now {@code http.client.trust-all}.
 * </p>
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient restClient(HttpClientProperties properties) throws NoSuchAlgorithmException, KeyManagementException {
        SSLSocketFactory trustAllSockets = properties.trustAll() ? trustAllContext().getSocketFactory() : null;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {

            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                // Both halves of what relaxedHTTPSValidation() did: accept any certificate
                // chain, and do not check that the certificate is for the host called.
                if (trustAllSockets != null && connection instanceof HttpsURLConnection https) {
                    https.setSSLSocketFactory(trustAllSockets);
                    https.setHostnameVerifier((hostname, session) -> true);
                }
            }
        };
        // Redirects are followed for GET only, which is this factory's default and
        // matches the Apache client under RestAssured: it followed 301 and 302 only
        // for GET and HEAD, so a PUT callback that got one was not followed then either.
        requestFactory.setConnectTimeout(properties.connectTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private SSLContext trustAllContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager trustAll = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] { trustAll }, null);
        return context;
    }
}
