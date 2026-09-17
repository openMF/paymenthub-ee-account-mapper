package org.mifos.identityaccountmapper.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the shared outgoing HTTP client.
 *
 * <p>
 * {@code trust-all} defaults to true because that is what the code did before: both call sites built a RestAssured
 * specification with {@code relaxedHTTPSValidation()}. It is a property now so a deployment whose targets have real
 * certificates can turn it off, instead of the choice being compiled in.
 * </p>
 */
@ConfigurationProperties(prefix = "http.client")
public record HttpClientProperties(@DefaultValue("true") boolean trustAll, @DefaultValue("30s") Duration connectTimeout) {
}
