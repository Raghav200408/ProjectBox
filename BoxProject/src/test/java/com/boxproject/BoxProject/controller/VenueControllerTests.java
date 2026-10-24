package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.entity.*;
import com.boxproject.BoxProject.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VenueControllerTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired VenueRepository venues;
    @Autowired SportTypeRepository sports;
    @Autowired VenueSportRepository venueSports;
    @Autowired VenueMemberRepository members;
    @Autowired AmenityRepository amenities;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    VenueAmenityRepository venueAmenities;
    private User owner;
    private SportType sport;
    private Amenity amenity;

    @BeforeEach
    void setup() {
        Role role = roles.findByName("VENUE_OWNER").orElseGet(() -> {
            Role value = new Role(); value.setName("VENUE_OWNER"); return roles.save(value);
        });
        owner = new User(); owner.setFullName("Venue Owner"); owner.setPhone("9123456789");
        owner.setPasswordHash("test-only"); owner.setRole(role); owner = users.saveAndFlush(owner);
        sport = new SportType(); sport.setName("Box Cricket"); sport = sports.saveAndFlush(sport);
        amenity = new Amenity(); amenity.setName("Parking"); amenity = amenities.saveAndFlush(amenity);
    }

    private Map<String, Object> request() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "City Arena"); body.put("phone", "9876543210");
        body.put("addressLine1", "Plot 12"); body.put("locality", "Madhapur");
        body.put("city", "Hyderabad"); body.put("state", "Telangana"); body.put("postalCode", "500081");
        body.put("latitude", 17.4483); body.put("longitude", 78.3915);
        body.put("ownerUserId", owner.getUserId()); body.put("sportTypeIds", List.of(sport.getId()));
        body.put("amenityIds", List.of(amenity.getId()));
        return body;
    }

    @Test
    void createsAllRelationshipsWithoutAuthenticationAndReturnsThemOnRead() throws Exception {
        var result = mvc.perform(post("/api/venues").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.ownerUserId").value(owner.getUserId()))
                .andExpect(jsonPath("$.sportTypeIds[0]").value(sport.getId()))
                .andExpect(jsonPath("$.amenityIds[0]").value(amenity.getId())).andReturn();
        long id = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(venueSports.findAllByVenueIdOrderBySportTypeIdAsc(id)).hasSize(1);
        assertThat(members.findAllByVenueIdAndActiveTrueOrderByCreatedAtAscIdAsc(id)).hasSize(1);
        assertThat(venueAmenities.findAllByVenueIdOrderByAmenityNameAscAmenityIdAsc(id)).hasSize(1);
        var venue = venues.findById(id).orElseThrow();
        assertThat(venue.getLocation().getX()).isEqualTo(78.3915);
        assertThat(venue.getLocation().getY()).isEqualTo(17.4483);
        mvc.perform(get("/api/venues/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sportTypeIds[0]").value(sport.getId()));
        var update = request();
        update.remove("ownerUserId"); update.remove("sportTypeIds"); update.remove("amenityIds");
        update.put("name", "Updated Arena"); update.put("status", "active");
        mvc.perform(put("/api/venues/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Arena"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.ownerUserId").value(owner.getUserId()))
                .andExpect(jsonPath("$.sportTypeIds[0]").value(sport.getId()))
                .andExpect(jsonPath("$.amenityIds[0]").value(amenity.getId()));
        update.put("sportTypeIds", List.of(sport.getId()));
        mvc.perform(put("/api/venues/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(update))).andExpect(status().isBadRequest());
    }

    @Test
    void validatesSelectionsBeforeCreatingAnything() throws Exception {
        long before = venues.count();
        for (String field : List.of("ownerUserId", "sportTypeIds")) {
            var body = request(); body.remove(field);
            mvc.perform(post("/api/venues").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
        for (List<Long> ids : List.of(List.of(sport.getId(), sport.getId()), List.of(Long.MAX_VALUE))) {
            var body = request(); body.put("sportTypeIds", ids);
            mvc.perform(post("/api/venues").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
        sport.setActive(false); sports.saveAndFlush(sport);
        mvc.perform(post("/api/venues").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request())))
                .andExpect(status().isBadRequest());
        assertThat(venues.count()).isEqualTo(before);
    }

    @Test
    void acceptsOmittedAmenitiesAndListsActiveSports() throws Exception {
        var body = request(); body.remove("amenityIds");
        mvc.perform(post("/api/venues").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amenityIds").isEmpty());
        mvc.perform(get("/api/venues/sports")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Box Cricket"));
    }
    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void rollsBackVenueAndRelationshipsWhenAssignmentSaveFails() {
        long venueCount = venues.count();
        long sportCount = venueSports.count();
        long memberCount = members.count();
        try {
            org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("test assignment failure"))
                    .when(venueAmenities).saveAll(org.mockito.ArgumentMatchers.<VenueAmenity>anyList());
            org.assertj.core.api.Assertions.assertThatCode(() ->
                    mvc.perform(post("/api/venues").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request())))
                            .andExpect(status().isConflict()))
                    .doesNotThrowAnyException();
        } finally {
            org.mockito.Mockito.reset(venueAmenities);
            amenities.deleteById(amenity.getId());
            sports.deleteById(sport.getId());
            users.deleteById(owner.getUserId());
        }
        assertThat(venues.count()).isEqualTo(venueCount);
        assertThat(venueSports.count()).isEqualTo(sportCount);
        assertThat(members.count()).isEqualTo(memberCount);
    }

}
