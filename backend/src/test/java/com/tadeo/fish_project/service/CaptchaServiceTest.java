package com.tadeo.fish_project.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tadeo.fish_project.exception.CaptchaFailedException;

class CaptchaServiceTest {

    private MockRestServiceServer server;
    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        captchaService = new CaptchaService(builder.build(), "secret", "sitekey");
    }

    private void expectVerification(String responseJson) {
        server.expect(requestTo(CaptchaService.verifyUrl))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().formDataContains(java.util.Map.of(
                "secret", "secret", "response", "token", "remoteip", "1.2.3.4")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    @Test
    void testAcceptsValidToken() {
        expectVerification("{\"success\": true, \"error-codes\": []}");
        assertDoesNotThrow(() -> captchaService.verify("token", "1.2.3.4"));
        server.verify();
    }

    @Test
    void testRejectsInvalidToken() {
        expectVerification("{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}");
        assertThrows(CaptchaFailedException.class, () -> captchaService.verify("token", "1.2.3.4"));
    }

    @Test
    void testRejectsMissingTokenWithoutRequest() {
        assertThrows(CaptchaFailedException.class, () -> captchaService.verify(null, "1.2.3.4"));
        assertThrows(CaptchaFailedException.class, () -> captchaService.verify(" ", "1.2.3.4"));
        server.verify();
    }

    @Test
    void testFailsClosedWhenCloudflareIsUnreachable() {
        server.expect(requestTo(CaptchaService.verifyUrl))
            .andRespond(withException(new SocketTimeoutException("timeout")));
        assertThrows(CaptchaFailedException.class, () -> captchaService.verify("token", "1.2.3.4"));

        server.reset();
        server.expect(requestTo(CaptchaService.verifyUrl)).andRespond(withServerError());
        assertThrows(CaptchaFailedException.class, () -> captchaService.verify("token", "1.2.3.4"));
    }

    @Test
    void testDisabledWithoutSecret() {
        CaptchaService disabled = new CaptchaService(RestClient.builder().build(), "", "");
        assertNull(disabled.getSiteKey());
        assertDoesNotThrow(() -> disabled.verify(null, null));
        assertEquals("sitekey", captchaService.getSiteKey());
    }
}
