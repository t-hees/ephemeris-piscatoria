package com.tadeo.ephemeris_piscatoria.exception;

public class ImageNotFoundException extends EntityNotFoundException {
    public ImageNotFoundException(Long catchId) {
        super("Failed to find image of catch: " + String.valueOf(catchId));
    }
}
