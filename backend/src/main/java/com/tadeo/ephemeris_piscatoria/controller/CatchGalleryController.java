package com.tadeo.ephemeris_piscatoria.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tadeo.ephemeris_piscatoria.dto.GalleryCatchPageDto;
import com.tadeo.ephemeris_piscatoria.service.TripService;

import lombok.RequiredArgsConstructor;

/*
The special catches of all trips, they are created and deleted through SpecialCatchController
*/
@RestController
@RequestMapping("/api/special-catches")
@RequiredArgsConstructor
public class CatchGalleryController {
    private static final int MAX_PAGE_SIZE = 50;

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<GalleryCatchPageDto> listSpecialCatches(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String species,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long minSize,
        @RequestParam(required = false) Long minWeight,
        @RequestParam(defaultValue = "false") boolean withImage) {
        return ResponseEntity.ok(tripService.listSpecialCatches(page, Math.min(size, MAX_PAGE_SIZE),
            species, location, from, to, minSize, minWeight, withImage));
    }
}
