package org.mifos.identityaccountmapper.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the single Caffeine cache, {@code accountLookupCache}.
 *
 * <p>
 * The keys keep their existing {@code spring.cache.*} spelling. Spring Boot's own cache properties live under the same
 * prefix and ignore the three below, so both bind side by side.
 * </p>
 *
 * <p>
 * All three are required, as they were when they were bare {@code @Value} fields: {@code @NotNull} on a wrapper type is
 * what stops startup when one is missing. A primitive would silently bind to 0.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "spring.cache")
public record AccountLookupCacheProperties(@NotNull Integer timeToLive, @NotNull Integer timeToIdle, @NotNull Integer maxEntriesHeap) {
}
