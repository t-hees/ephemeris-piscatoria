package com.tadeo.ephemeris_piscatoria.service;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tadeo.ephemeris_piscatoria.exception.CaptchaFailedException;

/*
Verifies Cloudflare Turnstile tokens of the registration form. Without a configured secret
(local development, tests) every registration is accepted.
*/
@Service
public class CaptchaService {
    private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);
    static final String verifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final RestClient restClient;
    private final String secret;
    private final String siteKey;

    record VerifyResponse(boolean success, @JsonProperty("error-codes") List<String> errorCodes) {}

    @Autowired
    public CaptchaService(RestClient.Builder restClientBuilder,
            @Value("${turnstile.secret:}") String secret,
            @Value("${turnstile.site-key:}") String siteKey) {
        this(restClientBuilder.requestFactory(requestFactoryWithTimeouts()).build(), secret, siteKey);
    }

    CaptchaService(RestClient restClient, String secret, String siteKey) {
        this.restClient = restClient;
        this.secret = secret;
        this.siteKey = siteKey;
        if (!isEnabled()) {
            logger.warn("No TURNSTILE_SECRET and TURNSTILE_SITE_KEY configured, registration has no captcha");
        }
    }

    // A hanging verification would block the registration request
    private static SimpleClientHttpRequestFactory requestFactoryWithTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    public boolean isEnabled() {
        return !secret.isBlank() && !siteKey.isBlank();
    }

    /*
    null when the captcha is disabled, the frontend then doesn't show the widget
    */
    public String getSiteKey() {
        return isEnabled() ? siteKey : null;
    }

    /*
    Fails closed: if Cloudflare can't be reached, registrations are rejected until it can
    */
    public void verify(String token, String remoteIp) {
        if (!isEnabled()) return;
        if (token == null || token.isBlank()) {
            throw new CaptchaFailedException();
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);
        if (remoteIp != null) form.add("remoteip", remoteIp);

        VerifyResponse response;
        try {
            response = restClient.post()
                .uri(verifyUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(VerifyResponse.class);
        } catch (RestClientException e) {
            logger.warn("Turnstile verification request failed", e);
            throw new CaptchaFailedException();
        }
        if (response == null || !response.success()) {
            logger.info("Rejected captcha token: {}", (response != null) ? response.errorCodes() : "empty response");
            throw new CaptchaFailedException();
        }
    }
}
