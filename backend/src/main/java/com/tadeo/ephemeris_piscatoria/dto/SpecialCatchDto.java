package com.tadeo.ephemeris_piscatoria.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/*
A new special catch, its image is uploaded along with it as a separate multipart part.
name is the picked common name
*/
public record SpecialCatchDto(@NotNull Long fishId, @PositiveOrZero Long size,
    @PositiveOrZero Long weight, @Size(max = 255) String notes, @Size(max = 255) String name){};
