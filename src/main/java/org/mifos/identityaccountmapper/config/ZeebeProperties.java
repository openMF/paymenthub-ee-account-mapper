package org.mifos.identityaccountmapper.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Zeebe gateway address and client sizing.
 *
 * <p>
 * The deployment sets {@code ZEEBE_BROKER_CONTACTPOINT}, {@code ZEEBE_CLIENT_MAX-EXECUTION-THREADS} and
 * {@code ZEEBE_CLIENT_POLL-INTERVAL} as environment variables, so the property names have to stay exactly as they are.
 * </p>
 *
 * <p>
 * Every value is required, as it was when it was a bare {@code @Value} field. {@code @Valid} carries the check into the
 * two nested groups.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "zeebe")
public record ZeebeProperties(@NotNull @Valid Broker broker, @NotNull @Valid Client client) {

    public record Broker(@NotNull String contactpoint) {
    }

    public record Client(@NotNull Integer maxExecutionThreads, @NotNull Integer pollInterval, @NotNull Integer evenlyAllocatedMaxJobs) {
    }
}
