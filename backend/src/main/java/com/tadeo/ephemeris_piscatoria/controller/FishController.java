package com.tadeo.ephemeris_piscatoria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tadeo.ephemeris_piscatoria.dto.FishNameMappingDto;
import com.tadeo.ephemeris_piscatoria.service.FishService;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fish")
@RequiredArgsConstructor
public class FishController {
    private final FishService fishService;

    @GetMapping
    public ResponseEntity<List<FishNameMappingDto>> searchByCommonName(@RequestParam String name) {
        return ResponseEntity.ok(fishService.searchByCommonName(name));
    }
}
