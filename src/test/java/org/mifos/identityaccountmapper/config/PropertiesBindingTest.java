package org.mifos.identityaccountmapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * The property names in this service are not the ones a record would produce on its own: some sit at the root of the
 * configuration, some use underscores, and the deployment sets others as environment variables with a dash in the
 * middle. These tests use the real spellings, so that if a rename ever creeps in the build says so instead of a
 * deployment going quiet.
 */
class PropertiesBindingTest {

    /** Starts from the shipped application.yml, so each test only overrides what it is about. */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withInitializer(new ConfigDataApplicationContextInitializer()).withUserConfiguration(TestConfig.class);

    @Test
    void bindsTheRootLevelLookupKeysWithTheirUnderscoreSpelling() {
        runner.withPropertyValues("account_validation_enabled=true", "account_validator_connector=mojaloop").run(context -> {
            LookupProperties properties = context.getBean(LookupProperties.class);
            assertThat(properties.accountValidationEnabled()).isTrue();
            assertThat(properties.accountValidatorConnector()).isEqualTo("mojaloop");
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

    @Test
    void bindsEveryRecordFromTheShippedApplicationYml() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(LookupProperties.class).accountValidationEnabled()).isFalse();
            assertThat(context.getBean(ZeebeProperties.class).client().maxExecutionThreads()).isEqualTo(50);
            assertThat(context.getBean(AccountLookupCacheProperties.class).timeToLive()).isEqualTo(3);
            assertThat(context.getBean(AsyncProperties.class).queueCapacity()).isEqualTo(50);
        });
    }

    /**
     * Every key was a bare {@code @Value} before, so a missing one stopped startup. Each record is checked on its own,
     * because which one fails first when several are missing is not deterministic.
     */
    @ParameterizedTest
    @ValueSource(classes = { OnlyLookup.class, OnlyZeebe.class, OnlyAsync.class, OnlyCache.class })
    void refusesToStartWhenTheSectionIsMissing(Class<?> onlyOneRecord) {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(onlyOneRecord).run(PropertiesBindingTest::failedOnValidation);
    }

    @Test
    void refusesToStartWhenOneKeyIsMissing() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(OnlyLookup.class).withPropertyValues("account_validator_connector=gsma")
                .run(PropertiesBindingTest::failedOnValidation);
    }

    @Test
    void refusesToStartWhenANumberIsSetToNothing() {
        // An empty value on an Integer binds to null, so @NotNull fires, as the bare @Value did.
        runner.withPropertyValues("zeebe.client.poll-interval=").run(PropertiesBindingTest::failedOnValidation);
    }

    @Test
    void acceptsAStringSetToNothing() {
        // A bare @Value accepted an empty string, and @NotNull does too: no stricter than before.
        runner.withPropertyValues("zeebe.broker.contactpoint=").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ZeebeProperties.class).broker().contactpoint()).isEmpty();
        });
    }

    private static void failedOnValidation(AssertableApplicationContext context) {
        assertThat(context).getFailure().hasRootCauseInstanceOf(BindValidationException.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ LookupProperties.class, ZeebeProperties.class, AsyncProperties.class,
            AccountLookupCacheProperties.class, HttpClientProperties.class })
    static class TestConfig {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LookupProperties.class)
    static class OnlyLookup {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ZeebeProperties.class)
    static class OnlyZeebe {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AsyncProperties.class)
    static class OnlyAsync {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AccountLookupCacheProperties.class)
    static class OnlyCache {}
}
