package org.mifos.pheeidaccountvalidatorimpl.gsmaconnector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mifos.pheeidaccountvalidatorimpl.service.AccountValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service(value = "gsma")
public class GSMAAccountValidation extends AccountValidationService {

    @Value("${gsma-connector.contactpoint}")
    public String gsmaConnectorContactPoint;
    @Value("${gsma-connector.endpoint.account-status}")
    public String accountStatusEndpoint;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GSMAAccountValidation(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /** Keeps the old behaviour: the status code is read below, so a 4xx or 5xx must not throw here. */
    private static void keepTheResponse(HttpRequest request, ClientHttpResponse response) {
        // deliberately nothing
    }

    @Override
    public Boolean validateAccount(String financialAddress, String tenant, String paymentModality, String payeeIdentity,
            String callbackURL) {
        accountStatusEndpoint = accountStatusEndpoint.replaceAll("identifierType", paymentModality);
        // accountStatusEndpoint = accountStatusEndpoint.replaceAll("identifierId", financialAddress);
        accountStatusEndpoint = accountStatusEndpoint.replaceAll("identifierId", payeeIdentity);

        ResponseEntity<String> response = restClient.get().uri(URI.create(gsmaConnectorContactPoint + accountStatusEndpoint))
                .header("Platform-TenantId", tenant).retrieve()
                .onStatus(status -> true, GSMAAccountValidation::keepTheResponse).toEntity(String.class);

        Integer statusCode = response.getStatusCode().value();
        String responseBody = response.getBody();
        Map<String, String> responseMap = null;
        try {
            responseMap = objectMapper.readValue(responseBody, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (!statusCode.equals(200)) {
            return false;
        } else if (statusCode.equals(200) && responseMap.get("accountStatus").equals("savingsAccountStatusType.active")) {
            return true;
        } else {
            return false;
        }
    }
}
