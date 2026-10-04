package com.tadeo.fish_project.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.tadeo.fish_project.entity.User;
import com.tadeo.fish_project.service.UserService;

/*
Creates the admin account from the environment on startup. The admin never has to register,
so nobody can take its username through the public registration first.
*/
@Component
public class AdminInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserService userService;
    private final String username;
    private final String password;

    public AdminInitializer(UserService userService,
            @Value("${admin.username:}") String username,
            @Value("${admin.password:}") String password) {
        this.userService = userService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (username.isBlank() || password.isBlank()) {
            logger.info("No admin configured, set ADMIN_USERNAME and ADMIN_PASSWORD to create one");
            return;
        }
        if (userService.userExists(username)) {
            return;
        }
        userService.createUser(username, password, User.Role.ADMIN);
        logger.info("Created admin account {}", username);
    }
}
