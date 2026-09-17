package org.mifos.identityaccountmapper.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The callback used to go out through RestAssured. These tests fix what the call looks like on the wire, so the swap to
 * RestClient can be checked without deploying anything: same method, same content type, same body, same URL, and a
 * failing callback still does not throw at the caller.
 */
class SendCallbackServiceTest {

    @Test
    void sendsThePayloadAsAJsonPutToTheCallbackUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://callback.example/notify")).andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().string("{\"requestID\":\"abc\"}"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        new SendCallbackService(builder.build()).sendCallback("{\"requestID\":\"abc\"}", "http://callback.example/notify");

        server.verify();
    }

    @Test
    void doesNotThrowWhenTheCallbackTargetAnswersWithAnError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://callback.example/notify")).andRespond(withServerError());

        SendCallbackService service = new SendCallbackService(builder.build());

        assertThatCode(() -> service.sendCallback("{}", "http://callback.example/notify")).doesNotThrowAnyException();
        server.verify();
    }
}
