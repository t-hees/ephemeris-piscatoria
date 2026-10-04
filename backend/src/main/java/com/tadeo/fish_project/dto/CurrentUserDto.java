package com.tadeo.fish_project.dto;

import com.tadeo.fish_project.entity.User;

public record CurrentUserDto(String username, User.Role role){};
