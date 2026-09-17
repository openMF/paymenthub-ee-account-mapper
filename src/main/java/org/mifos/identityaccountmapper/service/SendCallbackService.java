package org.mifos.identityaccountmapper.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.mifos.identityaccountmapper.data.CallbackRequestDTO;
import org.mifos.identityaccountmapper.data.FailedCaseDTO;
import org.mifos.identityaccountmapper.domain.ErrorTracking;
import org.mifos.identityaccountmapper.util.UniqueIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SendCallbackService {

    private static final Logger logger = LoggerFactory.getLogger(SendCallbackService.class);

    private final RestClient restClient;

    public SendCallbackService(RestClient restClient) {
        this.restClient = restClient;
    }

    public void sendCallback(String body, String callbackURL) {
        logger.debug(body);
        logger.debug(callbackURL);
        ResponseEntity<String> response = restClient.put().uri(URI.create(callbackURL)).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().onStatus(status -> true, SendCallbackService::keepTheResponse).toEntity(String.class);

        logger.debug(response.getBody());
        logger.debug(String.valueOf(response.getStatusCode().value()));
    }

    /**
     * Keeps the old behaviour: RestAssured did not throw on a 4xx or 5xx either, it handed the response back and the
     * caller decided. Without this, RestClient would throw instead.
     */
    private static void keepTheResponse(HttpRequest request, ClientHttpResponse response) {
        // deliberately nothing
    }

    public CallbackRequestDTO createRequestBody(List<ErrorTracking> errorTrackingList, String requestId) {
        List<FailedCaseDTO> failedCaseList = new ArrayList<>();
        int numberFailedCases = 0;
        for (ErrorTracking error : errorTrackingList) {
            failedCaseList.add(new FailedCaseDTO(error.getPayeeIdentity(), error.getModality(), error.getErrorDescription()));
            numberFailedCases++;
        }

        return new CallbackRequestDTO(UniqueIDGenerator.generateUniqueNumber(12), requestId, numberFailedCases, failedCaseList);
    }
}
