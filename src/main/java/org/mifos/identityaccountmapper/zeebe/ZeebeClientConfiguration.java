package org.mifos.identityaccountmapper.zeebe;

import io.camunda.zeebe.client.ZeebeClient;
import java.time.Duration;
import org.mifos.identityaccountmapper.config.ZeebeProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZeebeClientConfiguration {

    @Bean
    public ZeebeClient zeebeClient(ZeebeProperties properties) {
        return ZeebeClient.newClientBuilder().gatewayAddress(properties.broker().contactpoint()).usePlaintext()
                .defaultJobPollInterval(Duration.ofMillis(properties.client().pollInterval())).defaultJobWorkerMaxJobsActive(2000)
                .numJobWorkerExecutionThreads(properties.client().maxExecutionThreads()).build();
    }
}
