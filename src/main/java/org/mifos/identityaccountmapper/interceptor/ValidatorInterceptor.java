package org.mifos.identityaccountmapper.interceptor;

import static org.mifos.connector.common.exception.PaymentHubError.ExtValidationError;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mifos.connector.common.channel.dto.PhErrorDTO;
import org.mifos.connector.common.exception.PaymentHubErrorCategory;
import org.mifos.connector.common.validation.ValidatorBuilder;
import org.mifos.identityaccountmapper.api.implementation.RegisterBeneficiaryApiController;
import org.mifos.identityaccountmapper.api.implementation.UpdateBeneficiaryApiController;
import org.mifos.identityaccountmapper.util.IdentityMapperValidatorsEnum;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class ValidatorInterceptor implements HandlerInterceptor {

    private static final String RESOURCE = "ValidatorInterceptor";
    private static final String CALLBACK_URL = "X-CallbackURL";
    private static final String REGISTERING_INSTITUTION_ID = "X-Registering-Institution-ID";

    // The two controllers whose requests carry these headers. The checks were
    // written out twice, once per controller, with identical bodies.
    private static final Set<Class<?>> VALIDATED_CONTROLLERS = Set.of(RegisterBeneficiaryApiController.class,
            UpdateBeneficiaryApiController.class);

    private final ObjectMapper objectMapper;

    public ValidatorInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        log.debug("request at interceptor");

        if (!(handler instanceof HandlerMethod handlerMethod) || !VALIDATED_CONTROLLERS.contains(handlerMethod.getBeanType())) {
            return true;
        }

        // Using ValidatorBuilder for header validation
        final ValidatorBuilder validatorBuilder = new ValidatorBuilder();
        validatorBuilder.reset().resource(RESOURCE).parameter(CALLBACK_URL).value(request.getHeader(CALLBACK_URL))
                .isNullWithFailureCode(IdentityMapperValidatorsEnum.INVALID_CALLBACK_URL);

        validatorBuilder.reset().resource(RESOURCE).parameter(REGISTERING_INSTITUTION_ID)
                .value(request.getHeader(REGISTERING_INSTITUTION_ID))
                .isNullWithFailureCode(IdentityMapperValidatorsEnum.INVALID_REGISTERING_INSTITUTION_ID);

        if (!validatorBuilder.hasError()) {
            return true;
        }

        validatorBuilder.errorCategory(PaymentHubErrorCategory.Validation.toString())
                .errorCode(IdentityMapperValidatorsEnum.IDENTITY_MAPPER_HEADER_VALIDATION_ERROR.getCode())
                .errorDescription(IdentityMapperValidatorsEnum.IDENTITY_MAPPER_HEADER_VALIDATION_ERROR.getMessage())
                .developerMessage(IdentityMapperValidatorsEnum.IDENTITY_MAPPER_HEADER_VALIDATION_ERROR.getMessage())
                .defaultUserMessage(IdentityMapperValidatorsEnum.IDENTITY_MAPPER_HEADER_VALIDATION_ERROR.getMessage());

        PhErrorDTO.PhErrorDTOBuilder phErrorDTOBuilder = new PhErrorDTO.PhErrorDTOBuilder(ExtValidationError.getErrorCode());
        phErrorDTOBuilder.fromValidatorBuilder(validatorBuilder);

        // Converting PHErrorDTO in JSON Format
        String jsonResponse = objectMapper.writeValueAsString(phErrorDTOBuilder.build());

        // Setting response status and writing the error message
        response.setHeader("Content-Type", "application/json");
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.getWriter().write(jsonResponse);

        return false;
    }
}
