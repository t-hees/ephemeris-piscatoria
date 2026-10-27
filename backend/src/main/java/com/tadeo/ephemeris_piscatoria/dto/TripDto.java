package com.tadeo.ephemeris_piscatoria.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.tadeo.ephemeris_piscatoria.entity.Trip;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/*
Special catches are created one at a time with their image, see SpecialCatchController.
Missing simpleCatches mean the trip has none
*/
public record TripDto (@NotBlank @Size(max = 255) String location, Trip.Environment environment,
    @NotNull LocalDateTime time,
    @PositiveOrZero Long hours, Long temperature, Long waterLevel,
    Set<Trip.Weather> weather, @Size(max = 255) String notes,
    @Size(max = 100) List<@Valid SimpleCatchDto> simpleCatches){};
