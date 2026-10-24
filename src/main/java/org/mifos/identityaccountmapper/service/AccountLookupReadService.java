package org.mifos.identityaccountmapper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.mifos.identityaccountmapper.data.AccountLookupResponseDTO;
import org.mifos.identityaccountmapper.service.PayeeModalityCache.PayeeModalities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountLookupReadService {

    private final PayeeModalityCache payeeModalityCache;
    private final SendCallbackService sendCallbackService;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(AccountLookupReadService.class);

    @Autowired
    public AccountLookupReadService(PayeeModalityCache payeeModalityCache, SendCallbackService sendCallbackService,
            ObjectMapper objectMapper) {
        this.payeeModalityCache = payeeModalityCache;
        this.sendCallbackService = sendCallbackService;
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the response for this request from the payee's (cached) modalities. If the payee isn't registered, the
     * error is sent to the callback URL and then rethrown.
     */
    public AccountLookupResponseDTO lookup(String payeeIdentity, String callbackURL, String requestId, String registeringInstitutionId,
            Boolean isValidated) {
        PayeeModalities payee;
        try {
            payee = payeeModalityCache.find(payeeIdentity, registeringInstitutionId);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
            try {
                sendCallbackService.sendCallback(objectMapper.writeValueAsString(e.getMessage()), callbackURL);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            throw e;
        }
        return new AccountLookupResponseDTO(requestId, payee.payeeIdentity(), new ArrayList<>(payee.paymentModalities()), isValidated);
    }
}
