package com.tadeo.ephemeris_piscatoria.dto;

import jakarta.validation.constraints.NotBlank;

public record UserDto (@NotBlank String username, @NotBlank String password){};
