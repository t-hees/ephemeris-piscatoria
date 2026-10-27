package com.tadeo.ephemeris_piscatoria.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.tadeo.ephemeris_piscatoria.entity.Trip;
import com.tadeo.ephemeris_piscatoria.entity.User;

public interface TripRepository extends CrudRepository<Trip, Long> {
    Optional<Trip> findByIdAndUser(Long id, User user);

    void deleteAllByUser(User user);

    long countByUser(User user);

    @Query(value = "SELECT t FROM Trip t WHERE t.user = :user "
                   + "AND (:location IS NULL OR LOWER(t.location) LIKE LOWER(CONCAT('%', :location, '%'))) "
                   + "AND (:environment IS NULL OR t.environment = :environment) "
                   + "AND (:from IS NULL OR t.time >= :from) "
                   + "AND (:to IS NULL OR t.time <= :to) "
                   + "AND (:minCatches IS NULL OR"
                   + "     COALESCE((SELECT SUM(sc.amount) FROM Trip t2"
                   + "               JOIN t2.simpleCatches sc"
                   + "               WHERE t2 = t), 0)"
                   + "     + SIZE(t.specialCatches)"
                   + "     >= :minCatches)"
                   + "ORDER BY t.time DESC")
    Page<Trip> search(@Param("user") User user, @Param("location") String location,
        @Param("environment") Trip.Environment environment,
        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
        @Param("minCatches") Integer minCatches, Pageable pageable);
}
