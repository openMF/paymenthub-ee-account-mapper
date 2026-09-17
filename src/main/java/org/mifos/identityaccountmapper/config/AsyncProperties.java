package org.mifos.identityaccountmapper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Size of the pool that runs the {@code @Async} lookup and payment-modality methods. */
@ConfigurationProperties(prefix = "async")
public record AsyncProperties(@DefaultValue("5") int corePoolSize, @DefaultValue("5") int maxPoolSize,
        @DefaultValue("50") int queueCapacity) {
}
