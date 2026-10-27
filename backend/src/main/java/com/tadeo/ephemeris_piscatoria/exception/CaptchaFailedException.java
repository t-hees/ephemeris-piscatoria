package com.tadeo.ephemeris_piscatoria.exception;

public class CaptchaFailedException extends RuntimeException {
    public CaptchaFailedException() {
        super("Captcha verification failed");
    }
}
