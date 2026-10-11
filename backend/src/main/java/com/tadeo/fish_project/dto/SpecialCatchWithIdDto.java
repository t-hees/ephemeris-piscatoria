package com.tadeo.fish_project.dto;

/*
imageUrl is null for catches without an image, name is null if the user never picked a common name
*/
public record SpecialCatchWithIdDto(
    Long catchId, Long fishId, String imageUrl,
    Long size, Long weight, String notes, String name, String scientificName) {};
