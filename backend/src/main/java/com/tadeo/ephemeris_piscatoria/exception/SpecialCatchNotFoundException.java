package com.tadeo.ephemeris_piscatoria.exception;

public class SpecialCatchNotFoundException extends EntityNotFoundException {
    public SpecialCatchNotFoundException(Long catchId) {
        super("Failed to find special catch: " + String.valueOf(catchId));
    }
}
