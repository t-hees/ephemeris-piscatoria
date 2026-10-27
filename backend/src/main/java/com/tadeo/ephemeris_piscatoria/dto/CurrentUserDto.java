package com.tadeo.ephemeris_piscatoria.dto;

import com.tadeo.ephemeris_piscatoria.entity.User;

public record CurrentUserDto(String username, User.Role role){};
