package com.tadeo.ephemeris_piscatoria.dto;

import java.util.Optional;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/*
name is the picked common name, scientificName is only returned
*/
public record SimpleCatchDto(@NotNull Long fishId, @NotNull @Positive Integer amount, Optional<String> name,
    Optional<String> scientificName){};
