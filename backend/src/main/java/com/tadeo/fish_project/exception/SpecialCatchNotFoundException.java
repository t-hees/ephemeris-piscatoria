package com.tadeo.fish_project.exception;

public class SpecialCatchNotFoundException extends EntityNotFoundException {
    public SpecialCatchNotFoundException(Long catchId) {
        super("Failed to find special catch: " + String.valueOf(catchId));
    }
}
