package com.tadeo.ephemeris_piscatoria.dto;

import java.util.List;

public record AllCatchesDto(List<SimpleCatchDto> simpleCatches, List<SpecialCatchWithIdDto> specialCatches){};
