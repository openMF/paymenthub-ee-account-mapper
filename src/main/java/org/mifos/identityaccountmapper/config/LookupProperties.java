package org.mifos.identityaccountmapper.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Account lookup settings.
 *
 * <p>
 * These two keys sit at the root of the configuration ({@code account_validation_enabled},
 * {@code account_validator_connector}) rather than under a prefix, because that is how the helm chart and the
 * application.yml have always spelled them. Renaming them would silently break a deployment, so the record binds at the
 * root instead. Spring's relaxed binding treats the underscore spelling and the field names as the same property;
 * PropertiesBindingTest pins that down.
 * </p>
 *
 * <p>
 * Both are required, as they were when they were bare {@code @Value} fields. This matters most for
 * {@code account_validation_enabled}: with a default, a missing key would switch account validation off without a word.
 * </p>
 */
@Validated
@ConfigurationProperties
public record LookupProperties(@NotNull Boolean accountValidationEnabled, @NotNull String accountValidatorConnector) {
}
