package com.tadeo.ephemeris_piscatoria.repository;

import com.tadeo.ephemeris_piscatoria.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByUsername(String username);

    List<User> findAllByRoleAndCreatedAtBefore(User.Role role, LocalDateTime createdAt);
}
