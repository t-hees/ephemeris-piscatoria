package com.tadeo.fish_project.repository;

import com.tadeo.fish_project.entity.SpecialCatch;
import com.tadeo.fish_project.entity.Trip;

/*
A special catch along with its trip, as SpecialCatch has no reference to the trip it belongs to
*/
public record TripSpecialCatch(Trip trip, SpecialCatch specialCatch) {};
