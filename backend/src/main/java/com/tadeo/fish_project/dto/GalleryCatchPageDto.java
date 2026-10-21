package com.tadeo.fish_project.dto;

import java.util.List;

public record GalleryCatchPageDto(List<GalleryCatchDto> catches, boolean hasNext, long totalElements){};
