package com.tadeo.fish_project.controller;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tadeo.fish_project.entity.Trip;
import com.tadeo.fish_project.service.TripService;
import com.tadeo.fish_project.dto.AllCatchesDto;
import com.tadeo.fish_project.dto.TripDetailDto;
import com.tadeo.fish_project.dto.TripDto;
import com.tadeo.fish_project.dto.TripPageDto;
import com.tadeo.fish_project.dto.TripReturnDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {
    private static final int MAX_PAGE_SIZE = 50;

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<TripPageDto> listTrips(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) Trip.Environment environment,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Integer minCatches) {
        TripPageDto trips = tripService.listAllTrips(page, Math.min(size, MAX_PAGE_SIZE), location, environment, from, to, minCatches);
        return ResponseEntity.ok(trips);
    }

    @PostMapping
    public ResponseEntity<TripReturnDto> createTrip(@Valid @RequestBody TripDto tripDto) {
        TripReturnDto trip = tripService.createTrip(tripDto);
        return ResponseEntity.created(URI.create("/api/trips/" + trip.id())).body(trip);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripDetailDto> getTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTrip(id));
    }

    /*
    Replaces the fields and simple catches, special catches are changed through SpecialCatchController
    */
    @PutMapping("/{id}")
    public ResponseEntity<TripReturnDto> updateTrip(@PathVariable Long id, @Valid @RequestBody TripDto tripDto) {
        return ResponseEntity.ok(tripService.updateTrip(id, tripDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/catches")
    public ResponseEntity<AllCatchesDto> getCatches(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getAllCatches(id));
    }
}
