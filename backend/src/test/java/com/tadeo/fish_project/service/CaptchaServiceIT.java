package com.tadeo.fish_project.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.tadeo.fish_project.exception.CaptchaFailedException;

// Calls the real Cloudflare siteverify endpoint with the test keys from
// https://developers.cloudflare.com/turnstile/troubleshooting/testing/
class CaptchaServiceIT {

    private static final String siteKey = "1x00000000000000000000AA";
    private static final String passingSecret = "1x0000000000000000000000000000000AA";
    private static final String failingSecret = "2x0000000000000000000000000000000AA";
    private static final String spentSecret = "3x0000000000000000000000000000000AA";
    private static final String dummyToken = "XXXX.DUMMY.TOKEN.XXXX";

    private CaptchaService captchaService(String secret) {
        return new CaptchaService(RestClient.builder(), secret, siteKey);
    }

    @Test
    void testAcceptsTokenWithPassingSecret() {
        assertDoesNotThrow(() -> captchaService(passingSecret).verify(dummyToken, "1.2.3.4"));
    }

    @Test
    void testRejectsTokenWithFailingSecret() {
        assertThrows(CaptchaFailedException.class,
            () -> captchaService(failingSecret).verify(dummyToken, "1.2.3.4"));
    }

    @Test
    void testRejectsAlreadySpentToken() {
        assertThrows(CaptchaFailedException.class,
            () -> captchaService(spentSecret).verify(dummyToken, "1.2.3.4"));
    }
}
