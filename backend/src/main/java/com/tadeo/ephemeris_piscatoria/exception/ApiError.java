package com.tadeo.ephemeris_piscatoria.exception;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp, String uri){};
