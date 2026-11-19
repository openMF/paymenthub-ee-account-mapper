package org.mifos.identityaccountmapper.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mifos.identityaccountmapper.data.FetchBeneficiariesResponseDTO;
import org.mifos.identityaccountmapper.domain.ErrorTracking;
import org.mifos.identityaccountmapper.domain.IdentityDetails;
import org.mifos.identityaccountmapper.domain.PaymentModalityDetails;
import org.mifos.identityaccountmapper.service.FetchBeneficiariesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PHEE-474: V1 created the modality columns as BIGINT, so a beneficiary registered with "00" (ACCOUNT_ID) was read back
 * as "0". Runs the Flyway migrations and the JPA mappings against MySQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import(FetchBeneficiariesService.class)
class ModalityRoundTripTest {

    /**
     * Only the JPA layer: the application class's own component scans would pull in Zeebe, the validators and the HTTP
     * clients.
     */
    @SpringBootConfiguration
    @EntityScan(basePackageClasses = PaymentModalityDetails.class)
    @EnableJpaRepositories(basePackageClasses = PaymentModalityRepository.class)
    static class JpaOnly {}

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> MySqlTestDatabase.urlFor("round_trip"));
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> MySqlTestDatabase.ROOT_PASSWORD);
    }

    @Autowired
    private MasterRepository masterRepository;
    @Autowired
    private PaymentModalityRepository paymentModalityRepository;
    @Autowired
    private ErrorTrackingRepository errorTrackingRepository;
    @Autowired
    private FetchBeneficiariesService fetchBeneficiariesService;
    @Autowired
    private EntityManager entityManager;

    @Test
    void everyModalityCodeIsReadBackWithItsLeadingZero() {
        for (String code : new String[] { "00", "01", "02", "03", "04" }) {
            String payee = "payee-" + code;
            masterRepository.save(new IdentityDetails("master-" + code, "tenant-a", LocalDateTime.of(2026, 1, 1, 0, 0), payee));
            paymentModalityRepository.save(new PaymentModalityDetails("master-" + code, "account-" + code, code, "bank-a"));
        }
        // read from the database, not the persistence context
        entityManager.flush();
        entityManager.clear();

        for (String code : new String[] { "00", "01", "02", "03", "04" }) {
            assertThat(paymentModalityRepository.findByMasterId("master-" + code)).singleElement()
                    .satisfies(details -> assertThat(details.getModality()).isEqualTo(code));
            FetchBeneficiariesResponseDTO fetched = fetchBeneficiariesService.fetchBeneficiary("payee-" + code, "tenant-a");
            assertThat(fetched.getPaymentModality()).isEqualTo(code);
        }
        assertThat(fetchBeneficiariesService.fetchAllBeneficiaries(0, 10, "tenant-a").getContent())
                .extracting(FetchBeneficiariesResponseDTO::getPaymentModality).containsExactlyInAnyOrder("00", "01", "02", "03", "04");
    }

    @Test
    void aRejectedRequestKeepsTheModalityItCarried() {
        errorTrackingRepository.save(new ErrorTracking("request-1", "payee-1", "00", "Beneficiary already registered"));
        errorTrackingRepository.save(new ErrorTracking("request-1", "payee-2", "ABC", "Payee Modality Invalid"));
        entityManager.flush();
        entityManager.clear();

        assertThat(errorTrackingRepository.findAll()).extracting(ErrorTracking::getPayeeIdentity, ErrorTracking::getModality)
                .containsExactlyInAnyOrder(tuple("payee-1", "00"), tuple("payee-2", "ABC"));
    }
}
