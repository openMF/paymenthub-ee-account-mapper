package org.mifos.identityaccountmapper.service;

import java.util.List;
import org.mifos.identityaccountmapper.data.PaymentModalityDTO;
import org.mifos.identityaccountmapper.domain.IdentityDetails;
import org.mifos.identityaccountmapper.repository.MasterRepository;
import org.mifos.identityaccountmapper.repository.PaymentModalityRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Caches a registered payee's payment modalities, keyed by payee identity and registering institution.
 *
 * <p>
 * Only the stored rows are cached. The lookup response also carries the request's ID and validation result, so
 * {@link AccountLookupReadService} builds it fresh for every request. This is a separate bean so that calls from those
 * services go through Spring's cache proxy.
 * </p>
 */
@Component
public class PayeeModalityCache {

    public static final String CACHE_NAME = "accountLookupCache";

    private final MasterRepository masterRepository;
    private final PaymentModalityRepository paymentModalityRepository;

    public PayeeModalityCache(MasterRepository masterRepository, PaymentModalityRepository paymentModalityRepository) {
        this.masterRepository = masterRepository;
        this.paymentModalityRepository = paymentModalityRepository;
    }

    /** A payee's stored identity and modalities. */
    public record PayeeModalities(String payeeIdentity, List<PaymentModalityDTO> paymentModalities) {

        public PayeeModalities {
            paymentModalities = List.copyOf(paymentModalities);
        }
    }

    /**
     * Throws if the identity isn't registered with this institution. A thrown lookup isn't cached, so a payee that is
     * registered later is found on the next lookup.
     */
    @Cacheable(CACHE_NAME)
    public PayeeModalities find(String payeeIdentity, String registeringInstitutionId) {
        IdentityDetails identityDetails = masterRepository
                .findByPayeeIdentityAndRegisteringInstitutionId(payeeIdentity, registeringInstitutionId)
                .orElseThrow(() -> new RuntimeException("Payee Identity does not exist."));
        List<PaymentModalityDTO> paymentModalities = paymentModalityRepository.findByMasterId(identityDetails.getMasterId()).stream().map(
                details -> new PaymentModalityDTO(details.getModality(), details.getDestinationAccount(), details.getInstitutionCode()))
                .toList();
        return new PayeeModalities(identityDetails.getPayeeIdentity(), paymentModalities);
    }

    @CacheEvict(CACHE_NAME)
    public void evict(String payeeIdentity, String registeringInstitutionId) {
        // the annotation does the work
    }
}
