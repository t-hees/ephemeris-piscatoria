package com.tadeo.ephemeris_piscatoria.controller;

import java.net.URI;
import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tadeo.ephemeris_piscatoria.dto.SpecialCatchDto;
import com.tadeo.ephemeris_piscatoria.dto.SpecialCatchWithIdDto;
import com.tadeo.ephemeris_piscatoria.entity.Image;
import com.tadeo.ephemeris_piscatoria.service.TripService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
Special catches are created one at a time, so each request carries at most one image
*/
@RestController
@RequestMapping("/api/trips/{tripId}/special-catches")
@RequiredArgsConstructor
public class SpecialCatchController {

    private final TripService tripService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SpecialCatchWithIdDto> createSpecialCatch(@PathVariable Long tripId,
            @Valid @RequestPart("catch") SpecialCatchDto specialCatchDto,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        SpecialCatchWithIdDto created = tripService.createSpecialCatch(tripId, specialCatchDto, image);
        return ResponseEntity
            .created(URI.create("/api/trips/" + tripId + "/special-catches/" + created.catchId()))
            .body(created);
    }

    @DeleteMapping("/{catchId}")
    public ResponseEntity<Void> deleteSpecialCatch(@PathVariable Long tripId, @PathVariable Long catchId) {
        tripService.deleteSpecialCatch(tripId, catchId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{catchId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long tripId, @PathVariable Long catchId) {
        Image image = tripService.getSpecialCatchImage(tripId, catchId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.getMimeType()))
            // Images of a catch never change, private as they are only visible to their owner
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
            .body(image.getData());
    }
}
