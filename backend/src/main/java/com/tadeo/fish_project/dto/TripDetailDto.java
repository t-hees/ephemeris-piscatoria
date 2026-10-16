package com.tadeo.fish_project.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.tadeo.fish_project.entity.Trip;

/*
A trip with all of its catches, for editing it as a whole
*/
public record TripDetailDto (Long id, String location, Trip.Environment environment, LocalDateTime time,
    Long hours, Long temperature, Long waterLevel,
    Set<Trip.Weather> weather, String notes,
    List<SimpleCatchDto> simpleCatches, List<SpecialCatchWithIdDto> specialCatches){};
