package com.tadeo.ephemeris_piscatoria.dto;

import java.util.List;

public record GalleryCatchPageDto(List<GalleryCatchDto> catches, boolean hasNext, long totalElements){};
