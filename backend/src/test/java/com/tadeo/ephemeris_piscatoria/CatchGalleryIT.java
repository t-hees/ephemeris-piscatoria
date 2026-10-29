package com.tadeo.ephemeris_piscatoria;

import static com.tadeo.ephemeris_piscatoria.util.TestUtils.encodedImage;
import static com.tadeo.ephemeris_piscatoria.util.TestUtils.specialCatchParts;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.tadeo.ephemeris_piscatoria.dto.GalleryCatchDto;
import com.tadeo.ephemeris_piscatoria.dto.GalleryCatchPageDto;
import com.tadeo.ephemeris_piscatoria.dto.SpecialCatchDto;
import com.tadeo.ephemeris_piscatoria.dto.SpecialCatchWithIdDto;
import com.tadeo.ephemeris_piscatoria.dto.TripDto;
import com.tadeo.ephemeris_piscatoria.dto.TripReturnDto;
import com.tadeo.ephemeris_piscatoria.entity.Trip;
import com.tadeo.ephemeris_piscatoria.util.TestFishUtils;
import com.tadeo.ephemeris_piscatoria.util.TestUserAuth;
import com.tadeo.ephemeris_piscatoria.util.TestUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class CatchGalleryIT {

    @Autowired
    TestUserAuth testUserAuth;

    @Autowired
    TestUtils testUtils;

    @Autowired
    TestFishUtils testFishUtils;

    private Long someFishId;
    private Long congerId;

    private Long createTrip(String username, String location, LocalDateTime time) {
        return testUserAuth.exchangeRestAs(username, "/api/trips", HttpMethod.POST,
            new ParameterizedTypeReference<TripReturnDto>() {},
            new TripDto(location, Trip.Environment.LAKE, time, 3l, null, null, Set.of(Trip.Weather.FOGGY), null, null),
            HttpStatus.CREATED, "Failed to create trip").id();
    }

    private Long createTrip(String location, LocalDateTime time) {
        return createTrip(TestUserAuth.username, location, time);
    }

    private SpecialCatchWithIdDto createSpecialCatch(Long tripId, Long fishId, Long size, Long weight, byte[] image) {
        ResponseEntity<SpecialCatchWithIdDto> response = testUserAuth.postMultipartAs(TestUserAuth.username,
            "/api/trips/" + tripId + "/special-catches",
            specialCatchParts(new SpecialCatchDto(fishId, size, weight, null, null), image),
            new ParameterizedTypeReference<SpecialCatchWithIdDto>() {});
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private GalleryCatchPageDto getPage(String username, String queryString) {
        return testUserAuth.exchangeRestAs(username, "/api/special-catches?" + queryString, HttpMethod.GET,
            new ParameterizedTypeReference<GalleryCatchPageDto>() {}, null, HttpStatus.OK, "Failed to get catches");
    }

    private List<Long> search(String queryString) {
        return getPage(TestUserAuth.username, "page=0&size=50&" + queryString).catches().stream()
            .map(galleryCatch -> galleryCatch.specialCatch().catchId())
            .toList();
    }

    @BeforeEach
    void init() {
        testUtils.cleanDatabase();
        // Only admins may upload images
        testUserAuth.createAdmin(TestUserAuth.username);
        someFishId = testFishUtils.initializeTestFish();
        congerId = testFishUtils.otherFishId();
    }

    @Test
    void testListIncludesTripFields() {
        Long tripId = createTrip("cool lake", LocalDateTime.of(2026, 4, 5, 6, 30));
        SpecialCatchWithIdDto created = createSpecialCatch(tripId, someFishId, 24l, 30l, encodedImage("png"));

        GalleryCatchPageDto page = getPage(TestUserAuth.username, "");
        assertEquals(List.of(new GalleryCatchDto(created, tripId, "cool lake", Trip.Environment.LAKE,
            LocalDateTime.of(2026, 4, 5, 6, 30), 3l, Set.of(Trip.Weather.FOGGY))), page.catches());
        assertNotNull(page.catches().getFirst().specialCatch().imageUrl());
        assertFalse(page.hasNext());
        assertEquals(1, page.totalElements());
    }

    @Test
    void testOtherUsersCatchesAreNotListed() {
        createSpecialCatch(createTrip("cool lake", LocalDateTime.of(2026, 4, 5, 6, 30)), someFishId, null, null, null);
        createTrip(TestUserAuth.otherUsername, "other lake", LocalDateTime.of(2026, 4, 5, 6, 30));

        assertTrue(getPage(TestUserAuth.otherUsername, "").catches().isEmpty());
    }

    @Test
    void testSearchFilters() {
        Long lakeTrip = createTrip("Cool Lake", LocalDateTime.of(2026, 4, 5, 6, 30));
        Long riverTrip = createTrip("River Bend", LocalDateTime.of(2026, 6, 1, 9, 0));
        Long small = createSpecialCatch(lakeTrip, someFishId, 20l, 100l, null).catchId();
        Long withImage = createSpecialCatch(lakeTrip, congerId, 80l, 2000l, encodedImage("png")).catchId();
        Long large = createSpecialCatch(riverTrip, congerId, 120l, 5000l, null).catchId();

        assertAll(
            () -> assertEquals(List.of(large, withImage), search("species=conger"),
                "Species should match the scientific name case-insensitively"),
            () -> assertEquals(List.of(large, withImage), search("species=meeraal"),
                "Species should match any common name"),
            () -> assertEquals(List.of(small), search("species=Aalmutter")),
            () -> assertEquals(List.of(withImage, small), search("location=lake")),
            () -> assertEquals(List.of(large), search("from=2026-06-01&to=2026-06-01")),
            () -> assertEquals(List.of(large, withImage), search("minSize=80")),
            () -> assertEquals(List.of(large), search("minWeight=2001")),
            () -> assertEquals(List.of(withImage), search("withImage=true")),
            () -> assertEquals(List.of(withImage), search("species=conger&location=lake")),
            () -> assertEquals(List.of(), search("species=nonexistent"))
        );
    }

    @Test
    void testPagingOrdersNewestTripFirst() {
        Long olderTrip = createTrip("older", LocalDateTime.of(2026, 1, 1, 12, 0));
        Long newerTrip = createTrip("newer", LocalDateTime.of(2026, 2, 1, 12, 0));
        Long olderFirst = createSpecialCatch(olderTrip, someFishId, null, null, null).catchId();
        Long olderSecond = createSpecialCatch(olderTrip, someFishId, null, null, null).catchId();
        Long newer = createSpecialCatch(newerTrip, someFishId, null, null, null).catchId();

        GalleryCatchPageDto first = getPage(TestUserAuth.username, "page=0&size=2");
        GalleryCatchPageDto last = getPage(TestUserAuth.username, "page=1&size=2");
        assertAll(
            () -> assertEquals(3, first.totalElements()),
            () -> assertTrue(first.hasNext()),
            () -> assertEquals(List.of(newer, olderSecond),
                first.catches().stream().map(c -> c.specialCatch().catchId()).toList()),
            () -> assertFalse(last.hasNext()),
            () -> assertEquals(List.of(olderFirst),
                last.catches().stream().map(c -> c.specialCatch().catchId()).toList())
        );
    }

    @Test
    void testPageSizeIsCapped() {
        // A trip holds at most 50 special catches
        Long firstTrip = createTrip("first", LocalDateTime.of(2026, 1, 1, 12, 0));
        Long secondTrip = createTrip("second", LocalDateTime.of(2026, 1, 2, 12, 0));
        for (int i = 0; i < 50; i++) {
            createSpecialCatch(firstTrip, someFishId, null, null, null);
        }
        createSpecialCatch(secondTrip, someFishId, null, null, null);

        GalleryCatchPageDto page = getPage(TestUserAuth.username, "page=0&size=1000");
        assertAll(
            () -> assertEquals(50, page.catches().size()),
            () -> assertEquals(51, page.totalElements()),
            () -> assertTrue(page.hasNext())
        );
    }

    @Test
    void testCatchWithoutImageHasNoImageUrl() {
        createSpecialCatch(createTrip("lake", LocalDateTime.of(2026, 4, 5, 6, 30)), someFishId, null, null, null);
        assertNull(getPage(TestUserAuth.username, "").catches().getFirst().specialCatch().imageUrl());
    }
}
