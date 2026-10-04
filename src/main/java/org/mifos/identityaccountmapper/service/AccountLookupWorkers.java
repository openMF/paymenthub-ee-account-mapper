package org.mifos.identityaccountmapper.service;

import static org.mifos.identityaccountmapper.util.AccountMapperEnum.WORKER_ACCOUNT_LOOKUP_CALLBACK;

import io.camunda.zeebe.client.ZeebeClient;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.mifos.identityaccountmapper.config.ZeebeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountLookupWorkers {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ZeebeClient zeebeClient;
    private final ZeebeProperties zeebeProperties;

    public AccountLookupWorkers(ZeebeClient zeebeClient, ZeebeProperties zeebeProperties) {
        this.zeebeClient = zeebeClient;
        this.zeebeProperties = zeebeProperties;
    }

    @PostConstruct
    public void setupWorkers() {
        logger.info("## generating {} zeebe worker", WORKER_ACCOUNT_LOOKUP_CALLBACK);
        zeebeClient.newWorker().jobType(WORKER_ACCOUNT_LOOKUP_CALLBACK.getValue()).handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> existingVariables = job.getVariablesAsMap();

            logger.debug("Zeebe variables: {}", existingVariables);

            client.newCompleteCommand(job.getKey()).send();
        }).name(WORKER_ACCOUNT_LOOKUP_CALLBACK.getValue()).maxJobsActive(zeebeProperties.client().evenlyAllocatedMaxJobs()).open();

    }
}
