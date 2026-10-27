package com.tadeo.ephemeris_piscatoria.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordDto (@NotBlank String password){};
