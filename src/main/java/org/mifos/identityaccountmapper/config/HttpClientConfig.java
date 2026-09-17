package org.mifos.identityaccountmapper.config;

import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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
        // NORMAL, not the JDK default of NEVER: RestAssured followed redirects
        // (RedirectConfig defaults to followRedirects=true, max 100), so a callback
        // target that answers 301 or 302 has to keep working.
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (properties.trustAll()) {
            builder.sslContext(trustAllContext());
            SSLParameters parameters = new SSLParameters();
            // Null turns off hostname verification, which is the other half of what
            // relaxedHTTPSValidation() did.
            parameters.setEndpointIdentificationAlgorithm(null);
            builder.sslParameters(parameters);
        }
        return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(builder.build())).build();
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
