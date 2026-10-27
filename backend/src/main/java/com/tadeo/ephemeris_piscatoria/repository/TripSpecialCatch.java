package com.tadeo.ephemeris_piscatoria.repository;

import com.tadeo.ephemeris_piscatoria.entity.SpecialCatch;
import com.tadeo.ephemeris_piscatoria.entity.Trip;

/*
A special catch along with its trip, as SpecialCatch has no reference to the trip it belongs to
*/
public record TripSpecialCatch(Trip trip, SpecialCatch specialCatch) {};
