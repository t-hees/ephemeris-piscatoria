package com.tadeo.ephemeris_piscatoria.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.tadeo.ephemeris_piscatoria.entity.Fish;
import com.tadeo.ephemeris_piscatoria.entity.SimpleCatch;
import com.tadeo.ephemeris_piscatoria.entity.SpecialCatch;
import com.tadeo.ephemeris_piscatoria.entity.Trip;
import com.tadeo.ephemeris_piscatoria.entity.User;
import com.tadeo.ephemeris_piscatoria.exception.FishNotFoundException;
import com.tadeo.ephemeris_piscatoria.exception.ImageNotFoundException;
import com.tadeo.ephemeris_piscatoria.exception.ImageUploadForbiddenException;
import com.tadeo.ephemeris_piscatoria.exception.QuotaExceededException;
import com.tadeo.ephemeris_piscatoria.exception.SpecialCatchNotFoundException;
import com.tadeo.ephemeris_piscatoria.exception.TripNotFoundException;
import com.tadeo.ephemeris_piscatoria.entity.Image;
import com.tadeo.ephemeris_piscatoria.dto.GalleryCatchDto;
import com.tadeo.ephemeris_piscatoria.dto.GalleryCatchPageDto;
import com.tadeo.ephemeris_piscatoria.dto.SpecialCatchDto;
import com.tadeo.ephemeris_piscatoria.dto.TripDetailDto;
import com.tadeo.ephemeris_piscatoria.dto.TripDto;
import com.tadeo.ephemeris_piscatoria.dto.TripPageDto;
import com.tadeo.ephemeris_piscatoria.dto.TripReturnDto;
import com.tadeo.ephemeris_piscatoria.dto.AllCatchesDto;
import com.tadeo.ephemeris_piscatoria.dto.SimpleCatchDto;
import com.tadeo.ephemeris_piscatoria.dto.SpecialCatchWithIdDto;
import com.tadeo.ephemeris_piscatoria.repository.SpecialCatchRepository;
import com.tadeo.ephemeris_piscatoria.repository.TripRepository;
import com.tadeo.ephemeris_piscatoria.repository.TripSpecialCatch;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripService {
    // Limits for the public demo, so a single account can't fill the database
    static final int MAX_TRIPS_PER_USER = 500;
    static final int MAX_SPECIAL_CATCHES_PER_TRIP = 50;

    private final TripRepository tripRepository;
    private final SpecialCatchRepository specialCatchRepository;
    private final UserService userService;
    private final FishService fishSerice;
    private final ImageService imageService;

    @Transactional
    public TripReturnDto createTrip(TripDto tripDto) {
        User user = userService.getUser();
        if (tripRepository.countByUser(user) >= MAX_TRIPS_PER_USER) {
            throw new QuotaExceededException("trips", MAX_TRIPS_PER_USER);
        }
        Trip trip = Trip.builder()
            .simpleCatches(new HashSet<SimpleCatch>())
            .specialCatches(new HashSet<SpecialCatch>())
            .weather(new HashSet<>())
            .user(user)
            .build();
        applyTripDto(trip, tripDto, user);

        return toReturnDto(tripRepository.save(trip));
    }

    /*
    Replaces the fields and simple catches, special catches are kept
    */
    @Transactional
    public TripReturnDto updateTrip(Long id, TripDto tripDto) {
        User user = userService.getUser();
        Trip trip = tripRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new TripNotFoundException(id));
        applyTripDto(trip, tripDto, user);
        // The simple catches are saved by persistence cascade
        return toReturnDto(tripRepository.save(trip));
    }

    public void deleteTrip(Long id) {
        Trip trip = tripRepository.findByIdAndUser(id, userService.getUser())
            .orElseThrow(() -> new TripNotFoundException(id));
        tripRepository.delete(trip);
    }

    @Transactional
    public TripDetailDto getTrip(Long id) {
        Trip trip = findOwnTrip(id);
        TripReturnDto fields = toReturnDto(trip);
        AllCatchesDto catches = toAllCatchesDto(trip);
        return new TripDetailDto(fields.id(), fields.location(), fields.environment(), fields.time(), fields.hours(),
            fields.temperature(), fields.waterLevel(), fields.weather(), fields.notes(),
            catches.simpleCatches(), catches.specialCatches());
    }

    /*
    image may be null for a catch without image
    */
    @Transactional
    public SpecialCatchWithIdDto createSpecialCatch(Long tripId, SpecialCatchDto dto, MultipartFile image) {
        User user = userService.getUser();
        Trip trip = tripRepository.findByIdAndUser(tripId, user)
            .orElseThrow(() -> new TripNotFoundException(tripId));
        boolean hasImage = image != null && !image.isEmpty();
        if (hasImage && !user.isAdmin()) {
            throw new ImageUploadForbiddenException();
        }
        if (trip.getSpecialCatches().size() >= MAX_SPECIAL_CATCHES_PER_TRIP) {
            throw new QuotaExceededException("special catches per trip", MAX_SPECIAL_CATCHES_PER_TRIP);
        }
        Fish fish = fishSerice.findById(dto.fishId())
            .orElseThrow(() -> new FishNotFoundException(dto.fishId()));
        if (dto.name() != null) rememberName(user, fish, dto.name());

        SpecialCatch specialCatch = specialCatchRepository.save(SpecialCatch.builder()
            .fish(fish)
            .image(hasImage ? imageService.createImage(image) : null)
            .size(dto.size())
            .weight(dto.weight())
            .notes(dto.notes())
            .build());
        trip.getSpecialCatches().add(specialCatch);
        tripRepository.save(trip);
        return toSpecialCatchDto(tripId, specialCatch, user.getFishNames());
    }

    /*
    The catch has to belong to a trip of the authenticated user, its image is deleted along with it
    */
    @Transactional
    public void deleteSpecialCatch(Long tripId, Long catchId) {
        Trip trip = findOwnTrip(tripId);
        SpecialCatch specialCatch = findSpecialCatch(trip, catchId)
            .orElseThrow(() -> new SpecialCatchNotFoundException(catchId));
        trip.getSpecialCatches().remove(specialCatch);
        tripRepository.save(trip);
    }

    /*
    The catch has to belong to a trip of the authenticated user
    */
    public Image getSpecialCatchImage(Long tripId, Long catchId) {
        return findSpecialCatch(findOwnTrip(tripId), catchId)
            .map(SpecialCatch::getImage)
            .orElseThrow(() -> new ImageNotFoundException(catchId));
    }

    @Transactional
    public AllCatchesDto getAllCatches(Long tripId) {
        return toAllCatchesDto(findOwnTrip(tripId));
    }

    private AllCatchesDto toAllCatchesDto(Trip trip) {
        Long tripId = trip.getId();
        Map<Long, String> fishNames = trip.getUser().getFishNames();

        List<SimpleCatchDto> simpleCatches = trip.getSimpleCatches().stream()
            .map(simpleCatch -> new SimpleCatchDto(
                simpleCatch.getFish().getId(),
                simpleCatch.getAmount(),
                Optional.ofNullable(fishNames.get(simpleCatch.getFish().getId())),
                Optional.of(simpleCatch.getFish().getScientificName())
            ))
            .collect(Collectors.toList());

        List<SpecialCatchWithIdDto> specialCatches = trip.getSpecialCatches().stream()
            .sorted(Comparator.comparing(SpecialCatch::getId))
            .map(specialCatch -> toSpecialCatchDto(tripId, specialCatch, fishNames))
            .collect(Collectors.toList());

        return new AllCatchesDto(simpleCatches, specialCatches);
    }

    public TripPageDto listAllTrips(int page, int size, String location, Trip.Environment environment,
        LocalDate from, LocalDate to, Integer minCatches) {

        User user = userService.getUser();
        LocalDateTime fromDateTime = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = (to != null) ? to.atTime(23, 59, 59) : null;
        Page<Trip> tripPage = tripRepository.search(user,
            (location != null && !location.isBlank()) ? location : null,
            environment, fromDateTime, toDateTime,
            (minCatches != null && minCatches <= 0) ? null : minCatches,
            PageRequest.of(page, size));
        List<TripReturnDto> trips = tripPage.getContent().stream()
            .map(TripService::toReturnDto)
            .collect(Collectors.toList());
        return new TripPageDto(trips, tripPage.hasNext(), tripPage.getTotalElements());
    }

    /*
    Transactional as the weather of the trips is loaded lazily
    */
    @Transactional
    public GalleryCatchPageDto listSpecialCatches(int page, int size, String species, String location,
        LocalDate from, LocalDate to, Long minSize, Long minWeight, boolean withImage) {

        User user = userService.getUser();
        Map<Long, String> fishNames = user.getFishNames();
        Page<TripSpecialCatch> catchPage = specialCatchRepository.search(user,
            (species != null && !species.isBlank()) ? species.trim() : null,
            (location != null && !location.isBlank()) ? location : null,
            (from != null) ? from.atStartOfDay() : null,
            (to != null) ? to.atTime(23, 59, 59) : null,
            minSize, minWeight, withImage,
            PageRequest.of(page, size));
        List<GalleryCatchDto> catches = catchPage.getContent().stream()
            .map(tripCatch -> {
                Trip trip = tripCatch.trip();
                return new GalleryCatchDto(
                    toSpecialCatchDto(trip.getId(), tripCatch.specialCatch(), fishNames),
                    trip.getId(),
                    trip.getLocation(),
                    trip.getEnvironment(),
                    trip.getTime(),
                    (trip.getDuration() != null) ? trip.getDuration().toHours() : null,
                    Set.copyOf(trip.getWeather()));
            })
            .collect(Collectors.toList());
        return new GalleryCatchPageDto(catches, catchPage.hasNext(), catchPage.getTotalElements());
    }

    private Trip findOwnTrip(Long tripId) {
        return tripRepository.findByIdAndUser(tripId, userService.getUser())
            .orElseThrow(() -> new TripNotFoundException(tripId));
    }

    private static Optional<SpecialCatch> findSpecialCatch(Trip trip, Long catchId) {
        return trip.getSpecialCatches().stream()
            .filter(specialCatch -> specialCatch.getId().equals(catchId))
            .findFirst();
    }

    /*
    The collections are changed in place, so orphan removal deletes replaced simple catches
    */
    private void applyTripDto(Trip trip, TripDto tripDto, User user) {
        Set<SimpleCatch> simpleCatches = toSimpleCatches(user,
            (tripDto.simpleCatches() != null) ? tripDto.simpleCatches() : List.of());
        trip.setLocation(tripDto.location());
        trip.setEnvironment(tripDto.environment());
        trip.setTime(tripDto.time());
        trip.setDuration((tripDto.hours() != null) ? Duration.ofHours(tripDto.hours()) : null);
        trip.setTemperature(tripDto.temperature());
        trip.setWaterLevel(tripDto.waterLevel());
        trip.getWeather().clear();
        if (tripDto.weather() != null) trip.getWeather().addAll(tripDto.weather());
        trip.setNotes(tripDto.notes());
        trip.getSimpleCatches().clear();
        trip.getSimpleCatches().addAll(simpleCatches);
    }

    private Set<SimpleCatch> toSimpleCatches(User user, List<SimpleCatchDto> dtos) {
        return dtos.stream().map((dto) -> {
            Fish fish = fishSerice.findById(dto.fishId())
                .orElseThrow(() -> new FishNotFoundException(dto.fishId()));
            dto.name().ifPresent(name -> rememberName(user, fish, name));
            return SimpleCatch.builder()
                .fish(fish)
                .amount(dto.amount())
                .build();
        }).collect(Collectors.toSet());
    }

    /*
    Only names of the fish are accepted, the map is saved with the user at the end of the transaction
    */
    private static void rememberName(User user, Fish fish, String name) {
        if (fish.getCommonNames().contains(name)) {
            user.getFishNames().put(fish.getId(), name);
        }
    }

    private static SpecialCatchWithIdDto toSpecialCatchDto(Long tripId, SpecialCatch specialCatch,
        Map<Long, String> fishNames) {
        // Checks the id only, so the lazy image itself isn't loaded
        boolean hasImage = specialCatch.getImage() != null;
        return new SpecialCatchWithIdDto(
            specialCatch.getId(),
            specialCatch.getFish().getId(),
            hasImage ? "/api/trips/" + tripId + "/special-catches/" + specialCatch.getId() + "/image" : null,
            specialCatch.getSize(),
            specialCatch.getWeight(),
            specialCatch.getNotes(),
            fishNames.get(specialCatch.getFish().getId()),
            specialCatch.getFish().getScientificName()
        );
    }

    private static TripReturnDto toReturnDto(Trip trip) {
        return new TripReturnDto(
            trip.getId(),
            trip.getLocation(),
            trip.getEnvironment(),
            trip.getTime(),
            (trip.getDuration() != null) ? trip.getDuration().toHours() : null,
            trip.getTemperature(),
            trip.getWaterLevel(),
            Set.copyOf(trip.getWeather()),
            trip.getNotes()
        );
    }
}
