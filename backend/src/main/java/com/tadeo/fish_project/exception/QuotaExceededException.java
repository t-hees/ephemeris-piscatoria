package com.tadeo.fish_project.exception;

public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String what, int limit) {
        super("Reached the limit of " + limit + " " + what);
    }
}
