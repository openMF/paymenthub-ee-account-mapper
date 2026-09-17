package org.mifos.identityaccountmapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * The property names in this service are not the ones a record would produce on its own: some sit at the root of the
 * configuration, some use underscores, and the deployment sets others as environment variables with a dash in the
 * middle. These tests use the real spellings, so that if a rename ever creeps in the build says so instead of a
 * deployment going quiet.
 */
class PropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsTheRootLevelLookupKeysWithTheirUnderscoreSpelling() {
        runner.withPropertyValues("account_validation_enabled=true", "account_validator_connector=mojaloop").run(context -> {
            LookupProperties properties = context.getBean(LookupProperties.class);
            assertThat(properties.accountValidationEnabled()).isTrue();
            assertThat(properties.accountValidatorConnector()).isEqualTo("mojaloop");
        });
    }

    @Test
    void fallsBackToTheShippedDefaultsWhenNothingIsSet() {
        runner.run(context -> {
            LookupProperties properties = context.getBean(LookupProperties.class);
            assertThat(properties.accountValidationEnabled()).isFalse();
            assertThat(properties.accountValidatorConnector()).isEqualTo("gsma");
        });
    }

    @Test
    void bindsTheZeebeKeysTheDeploymentSetsAsEnvironmentVariables() {
        // ZEEBE_BROKER_CONTACTPOINT and ZEEBE_CLIENT_MAX-EXECUTION-THREADS in the
        // deployment arrive as these property names.
        runner.withPropertyValues("zeebe.broker.contactpoint=paymenthub-infra-zeebe-gateway:26500", "zeebe.client.max-execution-threads=10",
                "zeebe.client.poll-interval=10", "zeebe.client.evenly-allocated-max-jobs=100").run(context -> {
                    ZeebeProperties properties = context.getBean(ZeebeProperties.class);
                    assertThat(properties.broker().contactpoint()).isEqualTo("paymenthub-infra-zeebe-gateway:26500");
                    assertThat(properties.client().maxExecutionThreads()).isEqualTo(10);
                    assertThat(properties.client().pollInterval()).isEqualTo(10);
                    assertThat(properties.client().evenlyAllocatedMaxJobs()).isEqualTo(100);
                });
    }

    @Test
    void buildsTheZeebeGroupsFromDefaultsWhenTheWholeSectionIsMissing() {
        runner.run(context -> {
            ZeebeProperties properties = context.getBean(ZeebeProperties.class);
            assertThat(properties.broker()).isNotNull();
            assertThat(properties.client()).isNotNull();
            assertThat(properties.client().maxExecutionThreads()).isEqualTo(50);
        });
    }

    @Test
    void bindsTheCacheKeysWithTheirUnderscoreSpelling() {
        runner.withPropertyValues("spring.cache.time_to_live=7", "spring.cache.time_to_idle=8", "spring.cache.max_entries_heap=9")
                .run(context -> {
                    AccountLookupCacheProperties properties = context.getBean(AccountLookupCacheProperties.class);
                    assertThat(properties.timeToLive()).isEqualTo(7);
                    assertThat(properties.timeToIdle()).isEqualTo(8);
                    assertThat(properties.maxEntriesHeap()).isEqualTo(9);
                });
    }

    @Test
    void bindsTheAsyncPoolSizes() {
        runner.withPropertyValues("async.core-pool-size=2", "async.max-pool-size=3", "async.queue-capacity=4").run(context -> {
            AsyncProperties properties = context.getBean(AsyncProperties.class);
            assertThat(properties.corePoolSize()).isEqualTo(2);
            assertThat(properties.maxPoolSize()).isEqualTo(3);
            assertThat(properties.queueCapacity()).isEqualTo(4);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ LookupProperties.class, ZeebeProperties.class, AsyncProperties.class,
            AccountLookupCacheProperties.class, HttpClientProperties.class })
    static class TestConfig {}
}
