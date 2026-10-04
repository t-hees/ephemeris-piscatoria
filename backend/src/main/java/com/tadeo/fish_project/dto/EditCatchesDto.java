package com.tadeo.fish_project.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
New special catches are created one at a time with their image, see SpecialCatchController
*/
public record EditCatchesDto(@NotNull @Size(max = 100) List<@Valid SimpleCatchDto> simpleCatches,
    @NotNull @Size(max = 100) List<Long> removableSpecialCatchIds){};
