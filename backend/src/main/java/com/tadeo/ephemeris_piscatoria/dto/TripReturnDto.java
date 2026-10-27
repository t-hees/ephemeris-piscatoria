package com.tadeo.ephemeris_piscatoria.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.tadeo.ephemeris_piscatoria.entity.Trip;

public record TripReturnDto (Long id, String location, Trip.Environment environment, LocalDateTime time,
    Long hours, Long temperature, Long waterLevel,
    Set<Trip.Weather> weather, String notes){};
