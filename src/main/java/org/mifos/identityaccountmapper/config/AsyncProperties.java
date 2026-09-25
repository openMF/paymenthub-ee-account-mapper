package org.mifos.identityaccountmapper.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Size of the pool that runs the {@code @Async} lookup and payment-modality methods. All three are required, as they
 * were when they were bare {@code @Value} fields.
 */
@Validated
@ConfigurationProperties(prefix = "async")
public record AsyncProperties(@NotNull Integer corePoolSize, @NotNull Integer maxPoolSize, @NotNull Integer queueCapacity) {
}
