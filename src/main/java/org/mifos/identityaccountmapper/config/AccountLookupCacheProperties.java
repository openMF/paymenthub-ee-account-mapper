package org.mifos.identityaccountmapper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the single Caffeine cache, {@code accountLookupCache}.
 *
 * <p>
 * The keys keep their existing {@code spring.cache.*} spelling. Spring Boot's own cache properties live under the same
 * prefix and ignore the three below, so both bind side by side.
 * </p>
 */
@ConfigurationProperties(prefix = "spring.cache")
public record AccountLookupCacheProperties(@DefaultValue("3") int timeToLive, @DefaultValue("3") int timeToIdle,
        @DefaultValue("10") int maxEntriesHeap) {
}
