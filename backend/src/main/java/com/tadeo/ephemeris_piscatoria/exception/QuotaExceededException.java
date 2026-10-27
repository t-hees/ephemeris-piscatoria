package com.tadeo.ephemeris_piscatoria.exception;

public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String what, int limit) {
        super("Reached the limit of " + limit + " " + what);
    }
}
