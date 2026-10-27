package com.tadeo.ephemeris_piscatoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordDto (@NotBlank String oldPassword, @NotBlank @Size(min = 8, max = 72) String newPassword){};
