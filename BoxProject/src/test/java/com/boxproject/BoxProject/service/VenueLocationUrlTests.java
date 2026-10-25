package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.entity.Venue;
import com.boxproject.BoxProject.model.VenueRequestDTO;
import com.boxproject.BoxProject.repository.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VenueLocationUrlTests {
    @Test
    void savesReturnsAndClearsOptionalLocationUrl() {
        VenueRepository repository = mock(VenueRepository.class);
        VenueService service = new VenueService(repository, mock(UserRepository.class), mock(VenueMemberRepository.class), mock(AmenityRepository.class), mock(VenueAmenityRepository.class), mock(SportTypeRepository.class), mock(VenueSportRepository.class));
        Venue venue = new Venue();
        venue.setId(1L);
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(venue));
        when(repository.save(any(Venue.class))).thenAnswer(call -> call.getArgument(0));
        VenueRequestDTO request = request();
        request.setLocationUrl("https://maps.app.goo.gl/example");

        assertEquals(request.getLocationUrl(), service.updateVenue(1L, request).getLocationUrl());
        assertEquals(request.getLocationUrl(), venue.getLocationUrl());
        assertEquals(request.getLocationUrl(), service.getVenue(1L).getLocationUrl());

        request.setLocationUrl("");
        assertNull(service.updateVenue(1L, request).getLocationUrl());
        assertNull(venue.getLocationUrl());
    }

    @Test
    void acceptsOptionalHttpsLinkAndRejectsUnsafeScheme() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            VenueRequestDTO request = request();
            assertTrue(validator.validateProperty(request, "locationUrl").isEmpty());
            request.setLocationUrl("");
            assertTrue(validator.validateProperty(request, "locationUrl").isEmpty());
            request.setLocationUrl("https://www.google.com/maps?q=17.4,78.4");
            assertTrue(validator.validateProperty(request, "locationUrl").isEmpty());
            request.setLocationUrl("javascript:alert(1)");
            assertFalse(validator.validateProperty(request, "locationUrl").isEmpty());
        }
    }

    private VenueRequestDTO request() {
        VenueRequestDTO request = new VenueRequestDTO();
        request.setName("Ground");
        request.setPhone("9876543210");
        request.setAddressLine1("Main Road");
        request.setLocality("Locality");
        request.setCity("Hyderabad");
        request.setState("Telangana");
        request.setPostalCode("500001");
        request.setLatitude(new BigDecimal("17.4"));
        request.setLongitude(new BigDecimal("78.4"));
        return request;
    }
}
