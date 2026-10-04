package com.tadeo.fish_project.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tadeo.fish_project.entity.User;
import com.tadeo.fish_project.repository.UserRepository;

/*
The public demo lets anyone register, so accounts of regular users and everything they created
are deleted after demo.user-ttl. Admin accounts are kept. A ttl of zero keeps all accounts.
*/
@Service
public class DemoAccountCleanup {
    private static final Logger logger = LoggerFactory.getLogger(DemoAccountCleanup.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final Duration userTtl;

    public DemoAccountCleanup(UserRepository userRepository, UserService userService,
            @Value("${demo.user-ttl}") Duration userTtl) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.userTtl = userTtl;
    }

    public boolean isEnabled() {
        return !userTtl.isZero() && !userTtl.isNegative();
    }

    /*
    null if the account is never deleted
    */
    public LocalDateTime expiresAt(User user) {
        return (isEnabled() && !user.isAdmin()) ? user.getCreatedAt().plus(userTtl) : null;
    }

    @Scheduled(fixedDelayString = "${demo.cleanup-interval}")
    public void deleteExpiredAccounts() {
        if (!isEnabled()) return;
        LocalDateTime cutoff = LocalDateTime.now().minus(userTtl);
        for (User user : userRepository.findAllByRoleAndCreatedAtBefore(User.Role.USER, cutoff)) {
            try {
                userService.deleteWithTrips(user);
                logger.info("Deleted expired demo account {}", user.getUsername());
            } catch (Exception e) {
                // One broken account shouldn't stop the cleanup of the others
                logger.error("Failed to delete expired demo account {}", user.getUsername(), e);
            }
        }
    }
}
