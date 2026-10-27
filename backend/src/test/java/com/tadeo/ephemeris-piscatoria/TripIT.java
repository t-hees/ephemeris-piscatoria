package com.tadeo.ephemeris_piscatoria;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.tadeo.ephemeris_piscatoria.dto.AllCatchesDto;
import com.tadeo.ephemeris_piscatoria.dto.SimpleCatchDto;
import com.tadeo.ephemeris_piscatoria.dto.SpecialCatchDto;
import com.tadeo.ephemeris_piscatoria.dto.TripDetailDto;
import com.tadeo.ephemeris_piscatoria.dto.TripDto;
import com.tadeo.ephemeris_piscatoria.dto.TripPageDto;
import com.tadeo.ephemeris_piscatoria.dto.TripReturnDto;
import com.tadeo.ephemeris_piscatoria.entity.Trip;
import com.tadeo.ephemeris_piscatoria.exception.ApiError;
import com.tadeo.ephemeris_piscatoria.util.TestFishUtils;
import com.tadeo.ephemeris_piscatoria.util.TestUserAuth;
import com.tadeo.ephemeris_piscatoria.util.TestUtils;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TripIT {

    @Autowired
    TestUserAuth testUserAuth;

    @Autowired
    TestUtils testUtils;

    @Autowired
    TestFishUtils testFishUtils;

    private Long someFishId = null;

    private final TripDto tripDto = new TripDto(
        "cool lake",
        Trip.Environment.LAKE,
        LocalDateTime.of(2026, 4, 5, 6, 30),
        3l, 30l, 30000l,
        Set.of(Trip.Weather.CLEAR_SKY, Trip.Weather.LIGHT_RAIN),
        "notes", null);

    private TripPageDto getTripPage(String queryString) {
        return testUserAuth.exchangeRestWithAuth("/api/trips?" + queryString, HttpMethod.GET,
            new ParameterizedTypeReference<TripPageDto>() {}, null,
            HttpStatus.OK, "Failed to get trips");
    }

    private static String tripUrl(Long tripId) {
        return "/api/trips/" + tripId;
    }

    private static String catchesUrl(Long tripId) {
        return tripUrl(tripId) + "/catches";
    }

    private List<TripReturnDto> getAllTrips() {
        return getTripPage("page=0&size=50").trips();
    }

    private List<TripReturnDto> searchTrips(String queryString) {
        return getTripPage("page=0&size=50&" + queryString).trips();
    }

    private TripReturnDto createTrip(TripDto dto) {
        return testUserAuth.exchangeRestWithAuth("/api/trips", HttpMethod.POST,
            new ParameterizedTypeReference<TripReturnDto>() {}, dto, HttpStatus.CREATED, "Failed to create trip");
    }

    private TripDto tripAt(String location, LocalDateTime time) {
        return new TripDto(location, Trip.Environment.RIVER, time, null, null, null, Set.of(), null, null);
    }

    private static TripDto withSimpleCatches(TripDto dto, List<SimpleCatchDto> simpleCatches) {
        return new TripDto(dto.location(), dto.environment(), dto.time(), dto.hours(), dto.temperature(),
            dto.waterLevel(), dto.weather(), dto.notes(), simpleCatches);
    }

    private Long firstTripId() {
        List<TripReturnDto> trips = getAllTrips();
        assertFalse(trips.isEmpty());
        return trips.getFirst().id();
    }

    private static SimpleCatchDto simpleCatch(Long fishId, int amount, String name) {
        return new SimpleCatchDto(fishId, amount, Optional.ofNullable(name), Optional.empty());
    }

    private TripReturnDto updateTrip(Long tripId, TripDto dto) {
        return testUserAuth.exchangeRestWithAuth(tripUrl(tripId), HttpMethod.PUT,
            new ParameterizedTypeReference<TripReturnDto>() {}, dto, HttpStatus.OK, "Failed updating trip");
    }

    // Keeps the fields of tripDto, which all tests start with
    private void editCatches(Long tripId, SimpleCatchDto... simpleCatches) {
        updateTrip(tripId, withSimpleCatches(tripDto, List.of(simpleCatches)));
    }

    private TripDetailDto getTrip(Long tripId) {
        return testUserAuth.exchangeRestWithAuth(tripUrl(tripId), HttpMethod.GET,
            new ParameterizedTypeReference<TripDetailDto>() {}, null, HttpStatus.OK, "Failed to get trip");
    }

    private AllCatchesDto getCatches(Long tripId) {
        return testUserAuth.exchangeRestWithAuth(catchesUrl(tripId), HttpMethod.GET,
            new ParameterizedTypeReference<AllCatchesDto>() {}, null,
            HttpStatus.OK, "Failed to get all catches");
    }

    private ApiError expectEntityNotFound(String username, String url, HttpMethod method, Object data) {
        ApiError error = testUserAuth.exchangeErrorAs(username, url, method, data,
            HttpStatus.NOT_FOUND, "Expected request to be rejected");
        assertEquals("ENTITY_NOT_FOUND", error.code());
        return error;
    }

    private void expectValidationFail(String url, HttpMethod method, Object data) {
        ApiError error = testUserAuth.exchangeErrorAs(TestUserAuth.username, url, method, data,
            HttpStatus.BAD_REQUEST, "Expected invalid request to be rejected");
        assertEquals("VALIDATION_FAILED", error.code());
    }

    @BeforeEach
    void initTrips() {
        testUtils.cleanDatabase();
        someFishId = testFishUtils.initializeTestFish();
        createTrip(tripDto);
    }

    @Test
    void testCreateTrip() {
        TripReturnDto trip = getAllTrips().getFirst();
        assertEquals(
            new TripReturnDto(trip.id(), tripDto.location(), tripDto.environment(), tripDto.time(),
                tripDto.hours(), tripDto.temperature(), tripDto.waterLevel(), tripDto.weather(), tripDto.notes()),
            trip
        );
    }

    @Test
    void testCreateTripReturnsCreatedTrip() {
        ResponseEntity<TripReturnDto> response = testUtils.exchange("/api/trips", HttpMethod.POST,
            new ParameterizedTypeReference<TripReturnDto>() {}, tripAt("created", LocalDateTime.of(2026, 2, 2, 2, 0)),
            testUserAuth.authHeadersFor(TestUserAuth.username));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        TripReturnDto created = response.getBody();
        assertEquals("created", created.location());
        assertEquals(URI.create("/api/trips/" + created.id()), response.getHeaders().getLocation());
    }

    @Test
    void testCreateInvalidTrip() {
        expectValidationFail("/api/trips", HttpMethod.POST, new TripDto(" ", null, LocalDateTime.of(2026, 1, 1, 0, 0),
            null, null, null, null, null, null));
        expectValidationFail("/api/trips", HttpMethod.POST, new TripDto("no time", null, null,
            null, null, null, null, null, null));
        expectValidationFail("/api/trips", HttpMethod.POST, new TripDto("negative hours", null,
            LocalDateTime.of(2026, 1, 1, 0, 0), -1l, null, null, null, null, null));
        assertEquals(1, getAllTrips().size());
    }

    @Test
    void testCreateTripWithSimpleCatches() {
        Long tripId = createTrip(withSimpleCatches(tripAt("with catches", LocalDateTime.of(2026, 3, 1, 8, 0)),
            List.of(simpleCatch(someFishId, 3, TestFishUtils.someFishCommonName)))).id();

        assertEquals(
            List.of(new SimpleCatchDto(someFishId, 3, Optional.of(TestFishUtils.someFishCommonName),
                Optional.of(TestFishUtils.someFishScientificName))),
            getCatches(tripId).simpleCatches()
        );
    }

    @Test
    void testCreateTripWithUnknownFish() {
        expectEntityNotFound(TestUserAuth.username, "/api/trips", HttpMethod.POST,
            withSimpleCatches(tripDto, List.of(simpleCatch(-1l, 1, null))));
        assertEquals(1, getAllTrips().size(), "Trip with an unknown fish must not be created");
    }

    @Test
    void testUpdateTrip() {
        Long tripId = firstTripId();
        TripDto changed = new TripDto("other lake", Trip.Environment.RIVER, LocalDateTime.of(2026, 5, 6, 7, 0),
            null, 12l, null, Set.of(Trip.Weather.FOGGY), "changed notes", null);

        TripReturnDto updated = updateTrip(tripId, changed);

        TripReturnDto expected = new TripReturnDto(tripId, changed.location(), changed.environment(), changed.time(),
            null, changed.temperature(), null, changed.weather(), changed.notes());
        assertEquals(expected, updated, "Updating should return the resulting trip");
        assertEquals(List.of(expected), getAllTrips());
    }

    @Test
    void testUpdateTripKeepsSpecialCatches() {
        Long tripId = firstTripId();
        testUserAuth.postMultipartAs(TestUserAuth.username, tripUrl(tripId) + "/special-catches",
            TestUtils.specialCatchParts(new SpecialCatchDto(someFishId, null, null, null, null), null),
            new ParameterizedTypeReference<String>() {});

        editCatches(tripId, simpleCatch(someFishId, 1, null));

        assertEquals(1, getCatches(tripId).specialCatches().size());
    }

    @Test
    void testInvalidUpdate() {
        Long tripId = firstTripId();
        expectValidationFail(tripUrl(tripId), HttpMethod.PUT, new TripDto(" ", null,
            LocalDateTime.of(2026, 1, 1, 0, 0), null, null, null, null, null, null));
        expectEntityNotFound(TestUserAuth.username, tripUrl(-1l), HttpMethod.PUT, tripDto);
        assertEquals(tripDto.location(), getAllTrips().getFirst().location());
    }

    @Test
    void testGetTrip() {
        Long tripId = firstTripId();
        editCatches(tripId, simpleCatch(someFishId, 2, TestFishUtils.someFishCommonName));

        assertEquals(
            new TripDetailDto(tripId, tripDto.location(), tripDto.environment(), tripDto.time(), tripDto.hours(),
                tripDto.temperature(), tripDto.waterLevel(), tripDto.weather(), tripDto.notes(),
                getCatches(tripId).simpleCatches(), List.of()),
            getTrip(tripId)
        );
        expectEntityNotFound(TestUserAuth.username, tripUrl(-1l), HttpMethod.GET, null);
    }

    @Test
    void testEditCatchesWithInvalidAmount() {
        expectValidationFail(tripUrl(firstTripId()), HttpMethod.PUT,
            withSimpleCatches(tripDto, List.of(simpleCatch(someFishId, 0, null))));
    }

    @Test
    void testEditCatchesWithTooManyEntries() {
        List<SimpleCatchDto> tooMany = LongStream.range(0, 101)
            .mapToObj(i -> simpleCatch(someFishId, 1, null)).toList();
        expectValidationFail(tripUrl(firstTripId()), HttpMethod.PUT, withSimpleCatches(tripDto, tooMany));
    }

    @Test
    void testMalformedRequests() {
        ApiError invalidId = testUserAuth.exchangeErrorAs(TestUserAuth.username, "/api/trips/abc", HttpMethod.DELETE,
            null, HttpStatus.BAD_REQUEST, "Non-numeric trip id should be rejected");
        assertEquals("MALFORMED_REQUEST", invalidId.code());

        ApiError wrongMethod = testUserAuth.exchangeErrorAs(TestUserAuth.username, "/api/trips", HttpMethod.DELETE,
            null, HttpStatus.METHOD_NOT_ALLOWED, "Deleting the trip collection should not be supported");
        assertEquals("METHOD_NOT_ALLOWED", wrongMethod.code());
    }

    @Test
    void testCreateTripWithoutOptionalFields() {
        testUtils.cleanDatabase();
        createTrip(new TripDto("minimal", null, LocalDateTime.of(2026, 1, 1, 0, 0),
            null, null, null, null, null, null));

        TripReturnDto trip = getAllTrips().getFirst();
        assertAll(
            () -> assertEquals("minimal", trip.location()),
            () -> assertNull(trip.hours()),
            () -> assertNull(trip.notes())
        );
    }

    @Test
    void testDeleteTrip() {
        testUserAuth.exchangeNoContentWithAuth("/api/trips/" + firstTripId(), HttpMethod.DELETE, null,
            "Deletion failed");
        assertTrue(getAllTrips().isEmpty(), "Unexpected result after deletion");
    }

    @Test
    void testDeleteNonexistentTrip() {
        ApiError error = expectEntityNotFound(TestUserAuth.username, "/api/trips/-1", HttpMethod.DELETE, null);
        assertTrue(error.message().contains("Trip"), "Unexpected error message: " + error.message());
    }

    @Test
    void testOtherUserCannotAccessTrip() {
        Long tripId = firstTripId();
        String other = TestUserAuth.otherUsername;

        TripPageDto otherTrips = testUserAuth.exchangeRestAs(other, "/api/trips", HttpMethod.GET,
            new ParameterizedTypeReference<TripPageDto>() {}, null, HttpStatus.OK, "Failed to get trips");
        assertTrue(otherTrips.trips().isEmpty(), "Trips of other users must not be listed");

        expectEntityNotFound(other, catchesUrl(tripId), HttpMethod.GET, null);
        expectEntityNotFound(other, tripUrl(tripId), HttpMethod.GET, null);
        expectEntityNotFound(other, tripUrl(tripId), HttpMethod.PUT,
            withSimpleCatches(tripAt("taken over", LocalDateTime.of(2026, 1, 1, 0, 0)),
                List.of(simpleCatch(someFishId, 1, null))));
        expectEntityNotFound(other, tripUrl(tripId), HttpMethod.DELETE, null);

        assertEquals(List.of(tripId), getAllTrips().stream().map(TripReturnDto::id).toList(),
            "Trip of the owner must still exist");
        assertEquals(tripDto.location(), getAllTrips().getFirst().location(), "Trip of the owner must be unchanged");
        assertTrue(getCatches(tripId).simpleCatches().isEmpty(), "Catches of the owner must be unchanged");
    }

    @Test
    void testSearchTrips() {
        // @BeforeEach already created "cool lake" (LAKE) on 2026-04-05
        createTrip(new TripDto("River Bend", Trip.Environment.RIVER, LocalDateTime.of(2026, 6, 1, 9, 0),
            2l, 20l, 100l, Set.of(Trip.Weather.CLOUDY), "river notes", null));
        createTrip(new TripDto("Ocean Pier", Trip.Environment.OCEAN, LocalDateTime.of(2026, 7, 15, 10, 0),
            4l, 22l, 200l, Set.of(Trip.Weather.CLEAR_SKY), "ocean notes", null));

        assertAll(
            "Filtering by location should be a case-insensitive substring match",
            () -> assertEquals(1, searchTrips("location=river").size()),
            () -> assertEquals(1, searchTrips("location=COOL").size()),
            () -> assertEquals(0, searchTrips("location=nonexistent").size())
        );

        assertEquals(1, searchTrips("environment=OCEAN").size(), "Filtering by environment should match exactly");

        assertEquals(2, searchTrips("from=2026-05-01&to=2026-08-01").size(),
            "Date range should include River Bend and Ocean Pier but exclude the earlier lake trip");

        assertEquals(1, searchTrips("location=river&environment=RIVER").size(),
            "Combined filters should apply together");
    }

    @Test
    void testSearchByMinCatches() {
        // first trip with 1 special and 1 simple fish
        Long tripId = firstTripId();
        testUserAuth.postMultipartAs(TestUserAuth.username, tripUrl(tripId) + "/special-catches",
            TestUtils.specialCatchParts(new SpecialCatchDto(someFishId, null, null, null, null), null),
            new ParameterizedTypeReference<String>() {});
        editCatches(tripId, simpleCatch(someFishId, 1, null));

        // new trip with 2 + 3 simple fish
        createTrip(new TripDto("River Bend", Trip.Environment.RIVER, LocalDateTime.of(2026, 6, 1, 9, 0),
            2l, 20l, 100l, Set.of(Trip.Weather.CLOUDY), "river notes",
            List.of(simpleCatch(someFishId, 2, null), simpleCatch(testFishUtils.otherFishId(), 3, null))));

        assertEquals(2, searchTrips("minCatches=2").size(), "Min catches should encompass both simple and special catches (and be >=)");
        assertEquals(1, searchTrips("minCatches=5").size(), "The trip with only 2 fish should be filtered out");
        assertEquals(0, searchTrips("minCatches=6").size(), "Both tripss should be filtered out");
    }

    @Test
    void testSearchDateRangeIncludesWholeBoundaryDays() {
        createTrip(tripAt("late", LocalDateTime.of(2026, 6, 1, 23, 30)));
        createTrip(tripAt("early", LocalDateTime.of(2026, 6, 1, 0, 0)));

        assertEquals(2, searchTrips("from=2026-06-01&to=2026-06-01").size());
    }

    @Test
    void testPaging() {
        testUtils.cleanDatabase();
        for (int day = 1; day <= 12; day++) {
            createTrip(tripAt("trip " + day, LocalDateTime.of(2026, 1, day, 12, 0)));
        }

        TripPageDto first = getTripPage("page=0&size=5");
        TripPageDto last = getTripPage("page=2&size=5");
        assertAll(
            () -> assertEquals(12, first.totalElements()),
            () -> assertTrue(first.hasNext()),
            () -> assertEquals(List.of("trip 12", "trip 11", "trip 10", "trip 9", "trip 8"),
                first.trips().stream().map(TripReturnDto::location).toList(),
                "Trips should be ordered newest first"),
            () -> assertFalse(last.hasNext()),
            () -> assertEquals(List.of("trip 2", "trip 1"),
                last.trips().stream().map(TripReturnDto::location).toList())
        );
    }

    @Test
    void testPageSizeIsCapped() {
        for (int i = 0; i < 50; i++) {
            createTrip(tripAt("trip " + i, LocalDateTime.of(2025, 1, 1, 0, 0).plusDays(i)));
        }

        TripPageDto page = getTripPage("page=0&size=1000");
        assertAll(
            () -> assertEquals(50, page.trips().size()),
            () -> assertEquals(51, page.totalElements()),
            () -> assertTrue(page.hasNext())
        );
    }

    @Test
    void testEditCatches() {
        Long tripId = firstTripId();

        // Add catches
        editCatches(tripId, simpleCatch(someFishId, 4, TestFishUtils.someFishCommonName));

        // Get catches, the picked common name is returned along with the scientific name
        assertEquals(
            List.of(new SimpleCatchDto(someFishId, 4, Optional.of(TestFishUtils.someFishCommonName),
                Optional.of(TestFishUtils.someFishScientificName))),
            getCatches(tripId).simpleCatches()
        );

        // Delete catches
        editCatches(tripId);
        assertTrue(getCatches(tripId).simpleCatches().isEmpty(), "Getting catches after deletion should be empty");
    }

    @Test
    void testUpdateWithoutSimpleCatchesRemovesThem() {
        Long tripId = firstTripId();
        editCatches(tripId, simpleCatch(someFishId, 4, null));
        updateTrip(tripId, tripDto);
        assertTrue(getCatches(tripId).simpleCatches().isEmpty());
    }

    @Test
    void testEditCatchesIgnoresUnknownName() {
        Long tripId = firstTripId();
        editCatches(tripId, simpleCatch(someFishId, 1, "not a name of it"));
        assertEquals(Optional.empty(), getCatches(tripId).simpleCatches().getFirst().name());
    }

    @Test
    void testPickedNameAppliesToEarlierCatches() {
        Long congerId = testFishUtils.fishId(TestFishUtils.congerScientificName);
        Long firstTrip = firstTripId();
        Long secondTrip = createTrip(tripAt("second", LocalDateTime.of(2026, 5, 1, 0, 0))).id();

        editCatches(firstTrip, simpleCatch(congerId, 1, "Meeraal"));
        editCatches(secondTrip, simpleCatch(congerId, 1, "Conger"));

        assertEquals(Optional.of("Conger"), getCatches(firstTrip).simpleCatches().getFirst().name());
    }

    @Test
    void testPickedNamesArePerUser() {
        Long congerId = testFishUtils.fishId(TestFishUtils.congerScientificName);
        Long tripId = firstTripId();
        editCatches(tripId, simpleCatch(congerId, 1, "Meeraal"));

        String other = TestUserAuth.otherUsername;
        testUserAuth.exchangeRestAs(other, "/api/trips", HttpMethod.POST,
            new ParameterizedTypeReference<TripReturnDto>() {},
            withSimpleCatches(tripDto, List.of(simpleCatch(congerId, 1, "Conger"))),
            HttpStatus.CREATED, "Failed to create trip");

        assertEquals(Optional.of("Meeraal"), getCatches(tripId).simpleCatches().getFirst().name());
    }

    @Test
    void testEditCatchesReplacesSimpleCatches() {
        Long tripId = firstTripId();

        editCatches(tripId, simpleCatch(someFishId, 4, null));
        editCatches(tripId, simpleCatch(someFishId, 2, null));

        List<SimpleCatchDto> simpleCatches = getCatches(tripId).simpleCatches();
        assertEquals(1, simpleCatches.size(), "Simple catches should be replaced, not appended");
        assertEquals(2, simpleCatches.getFirst().amount());
    }

    @Test
    void testEditCatchesWithUnknownFish() {
        Long tripId = firstTripId();
        expectEntityNotFound(TestUserAuth.username, tripUrl(tripId), HttpMethod.PUT,
            withSimpleCatches(tripDto, List.of(simpleCatch(-1l, 1, null))));
        assertTrue(getCatches(tripId).simpleCatches().isEmpty());
    }

    @Test
    void testGetCatchesOfNonexistentTrip() {
        expectEntityNotFound(TestUserAuth.username, catchesUrl(-1l), HttpMethod.GET, null);
    }
}
