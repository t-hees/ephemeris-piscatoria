package com.tadeo.fish_project.exception;

public class CaptchaFailedException extends RuntimeException {
    public CaptchaFailedException() {
        super("Captcha verification failed");
    }
}
