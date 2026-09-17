package org.mifos.identityaccountmapper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Account lookup settings.
 *
 * <p>
 * These two keys sit at the root of the configuration ({@code account_validation_enabled},
 * {@code account_validator_connector}) rather than under a prefix, because that is how the helm chart and the
 * application.yml have always spelled them. Renaming them would silently break a deployment, so the record binds at the
 * root instead. Spring's relaxed binding treats the underscore spelling and the field names as the same property;
 * LookupPropertiesTest pins that down.
 * </p>
 */
@ConfigurationProperties
public record LookupProperties(@DefaultValue("false") boolean accountValidationEnabled,
        @DefaultValue("gsma") String accountValidatorConnector) {
}
