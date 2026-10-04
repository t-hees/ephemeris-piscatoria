package com.tadeo.fish_project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
Separate from UserDto so the password policy only applies to new passwords, not to logins of existing accounts.
BCrypt only accepts passwords of up to 72 bytes.
Usernames are limited to plain characters, so nobody can register names that look like others.
captchaToken is only required when the captcha is enabled, see CaptchaService.
*/
public record RegisterDto (
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,30}",
        message = "must be 3 to 30 letters, digits, '_', '.' or '-'") String username,
    @NotBlank @Size(min = 8, max = 72) String password,
    String captchaToken){};
