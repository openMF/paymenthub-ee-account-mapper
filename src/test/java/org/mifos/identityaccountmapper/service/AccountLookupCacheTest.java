package org.mifos.identityaccountmapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mifos.identityaccountmapper.config.AccountLookupCacheProperties;
import org.mifos.identityaccountmapper.data.AccountLookupResponseDTO;
import org.mifos.identityaccountmapper.domain.IdentityDetails;
import org.mifos.identityaccountmapper.domain.PaymentModalityDetails;
import org.mifos.identityaccountmapper.repository.MasterRepository;
import org.mifos.identityaccountmapper.repository.PaymentModalityRepository;
import org.mifos.identityaccountmapper.util.CacheConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * The lookup cache used to hold the whole response, keyed by payee identity only, so a repeat lookup got the first
 * request's requestId and validation result back, and two institutions shared an entry. Runs the real Spring cache
 * proxy over mocked repositories.
 */
class AccountLookupCacheTest {

    private final MasterRepository masterRepository = mock(MasterRepository.class);
    private final PaymentModalityRepository paymentModalityRepository = mock(PaymentModalityRepository.class);
    private final SendCallbackService sendCallbackService = mock(SendCallbackService.class);
    private AccountLookupReadService readService;
    private PayeeModalityCache payeeModalityCache;

    @BeforeEach
    void setUp() {
        register("payee-1", "bank-a", "master-a", "account-a");
        register("payee-1", "bank-b", "master-b", "account-b");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(AccountLookupCacheProperties.class, () -> new AccountLookupCacheProperties(60, 60, 100));
        context.registerBean(MasterRepository.class, () -> masterRepository);
        context.registerBean(PaymentModalityRepository.class, () -> paymentModalityRepository);
        context.registerBean(SendCallbackService.class, () -> sendCallbackService);
        context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
        context.register(CacheConfig.class, PayeeModalityCache.class, AccountLookupReadService.class);
        context.refresh();
        readService = context.getBean(AccountLookupReadService.class);
        payeeModalityCache = context.getBean(PayeeModalityCache.class);
    }

    @Test
    void repeatLookupsGetTheirOwnRequestIdAndValidationButHitTheDatabaseOnce() {
        AccountLookupResponseDTO first = readService.lookup("payee-1", "http://cb", "request-1", "bank-a", true);
        AccountLookupResponseDTO second = readService.lookup("payee-1", "http://cb", "request-2", "bank-a", false);

        assertThat(first.getRequestId()).isEqualTo("request-1");
        assertThat(first.getIsValidated()).isTrue();
        assertThat(second.getRequestId()).isEqualTo("request-2");
        assertThat(second.getIsValidated()).isFalse();
        assertThat(second.getPaymentModalityList()).singleElement()
                .satisfies(modality -> assertThat(modality.getFinancialAddress()).isEqualTo("account-a"));
        verify(masterRepository, times(1)).findByPayeeIdentityAndRegisteringInstitutionId("payee-1", "bank-a");
    }

    @Test
    void institutionsDoNotShareAnEntry() {
        AccountLookupResponseDTO fromA = readService.lookup("payee-1", "http://cb", "request-1", "bank-a", true);
        AccountLookupResponseDTO fromB = readService.lookup("payee-1", "http://cb", "request-2", "bank-b", true);

        assertThat(fromA.getPaymentModalityList().get(0).getFinancialAddress()).isEqualTo("account-a");
        assertThat(fromB.getPaymentModalityList().get(0).getFinancialAddress()).isEqualTo("account-b");
    }

    @Test
    void evictingOneInstitutionsEntryReloadsOnlyThatOne() {
        readService.lookup("payee-1", "http://cb", "request-1", "bank-a", true);
        readService.lookup("payee-1", "http://cb", "request-2", "bank-b", true);

        payeeModalityCache.evict("payee-1", "bank-a");
        readService.lookup("payee-1", "http://cb", "request-3", "bank-a", true);
        readService.lookup("payee-1", "http://cb", "request-4", "bank-b", true);

        verify(masterRepository, times(2)).findByPayeeIdentityAndRegisteringInstitutionId("payee-1", "bank-a");
        verify(masterRepository, times(1)).findByPayeeIdentityAndRegisteringInstitutionId("payee-1", "bank-b");
    }

    @Test
    void unknownPayeeIsReportedToTheCallbackAndNotCached() {
        assertThatThrownBy(() -> readService.lookup("payee-9", "http://cb", "request-1", "bank-a", true))
                .hasMessage("Payee Identity does not exist.");
        register("payee-9", "bank-a", "master-9", "account-9");

        assertThat(readService.lookup("payee-9", "http://cb", "request-2", "bank-a", true).getPayeeIdentity()).isEqualTo("payee-9");
        verify(sendCallbackService).sendCallback(anyString(), eq("http://cb"));
    }

    private void register(String payeeIdentity, String institution, String masterId, String account) {
        when(masterRepository.findByPayeeIdentityAndRegisteringInstitutionId(payeeIdentity, institution))
                .thenReturn(Optional.of(new IdentityDetails(masterId, institution, LocalDateTime.of(2026, 1, 1, 0, 0), payeeIdentity)));
        when(paymentModalityRepository.findByMasterId(masterId)).thenReturn(List.of(new PaymentModalityDetails(masterId, account, "00", "dfsp")));
    }
}
