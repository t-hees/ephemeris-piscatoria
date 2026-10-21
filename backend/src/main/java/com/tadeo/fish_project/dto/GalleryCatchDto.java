package com.tadeo.fish_project.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.tadeo.fish_project.entity.Trip;

/*
A special catch with the basic fields of its trip, for the gallery of all special catches
*/
public record GalleryCatchDto(SpecialCatchWithIdDto specialCatch, Long tripId, String location,
    Trip.Environment environment, LocalDateTime time, Long hours, Set<Trip.Weather> weather) {};
