package com.tadeo.fish_project.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.tadeo.fish_project.entity.SpecialCatch;
import com.tadeo.fish_project.entity.User;

public interface SpecialCatchRepository extends CrudRepository<SpecialCatch, Long> {

    String SEARCH_CONDITIONS = "WHERE t.user = :user "
        + "AND (:species IS NULL"
        + "     OR LOWER(f.scientificName) LIKE LOWER(CONCAT('%', :species, '%'))"
        + "     OR EXISTS (SELECT 1 FROM Fish f2 JOIN f2.commonNames cn"
        + "                WHERE f2 = f AND LOWER(cn) LIKE LOWER(CONCAT('%', :species, '%')))) "
        + "AND (:location IS NULL OR LOWER(t.location) LIKE LOWER(CONCAT('%', :location, '%'))) "
        + "AND (:from IS NULL OR t.time >= :from) "
        + "AND (:to IS NULL OR t.time <= :to) "
        + "AND (:minSize IS NULL OR sc.size >= :minSize) "
        + "AND (:minWeight IS NULL OR sc.weight >= :minWeight) "
        + "AND (:withImage = false OR sc.image IS NOT NULL) ";

    // Starts from the trip, the join table trip_special_catches is only known to Trip
    @Query(value = "SELECT new com.tadeo.fish_project.repository.TripSpecialCatch(t, sc) "
                   + "FROM Trip t JOIN t.specialCatches sc JOIN sc.fish f "
                   + SEARCH_CONDITIONS
                   + "ORDER BY t.time DESC, sc.id DESC",
        countQuery = "SELECT COUNT(sc) FROM Trip t JOIN t.specialCatches sc JOIN sc.fish f "
                   + SEARCH_CONDITIONS)
    Page<TripSpecialCatch> search(@Param("user") User user, @Param("species") String species,
        @Param("location") String location,
        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
        @Param("minSize") Long minSize, @Param("minWeight") Long minWeight,
        @Param("withImage") boolean withImage, Pageable pageable);
}
