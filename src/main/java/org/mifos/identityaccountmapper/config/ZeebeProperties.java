package org.mifos.identityaccountmapper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Zeebe gateway address and client sizing.
 *
 * <p>
 * The deployment sets {@code ZEEBE_BROKER_CONTACTPOINT}, {@code ZEEBE_CLIENT_MAX-EXECUTION-THREADS} and
 * {@code ZEEBE_CLIENT_POLL-INTERVAL} as environment variables, so the property names have to stay exactly as they are.
 * </p>
 */
@ConfigurationProperties(prefix = "zeebe")
public record ZeebeProperties(@DefaultValue Broker broker, @DefaultValue Client client) {

    public record Broker(@DefaultValue("zeebe-zeebe-gateway:26500") String contactpoint) {
    }

    public record Client(@DefaultValue("50") int maxExecutionThreads, @DefaultValue("10") int pollInterval,
            @DefaultValue("100") int evenlyAllocatedMaxJobs) {
    }
}
