package org.mifos.identityaccountmapper.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.mifos.identityaccountmapper.config.AccountLookupCacheProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Ported from Ehcache 2 to Caffeine: Spring Framework 6 removed
// EhCacheCacheManager (and CachingConfigurerSupport), and net.sf.ehcache is a
// pre-Jakarta dead end. Semantics preserved for the single heap-based cache:
// time_to_live -> expireAfterWrite, time_to_idle -> expireAfterAccess,
// max_entries_heap -> maximumSize. The old off-heap/disk size settings were
// inert (overflowToOffHeap was false) and have no Caffeine equivalent.
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(AccountLookupCacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("accountLookupCache");
        cacheManager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(properties.timeToLive()))
                .expireAfterAccess(Duration.ofSeconds(properties.timeToIdle())).maximumSize(properties.maxEntriesHeap()));
        return cacheManager;
    }
}
