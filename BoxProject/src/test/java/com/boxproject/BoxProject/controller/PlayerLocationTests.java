package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.model.*;
import com.boxproject.BoxProject.repository.*;
import com.boxproject.BoxProject.security.JwtUtil;
import com.boxproject.BoxProject.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlayerLocationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserService users;
    @Autowired UserRepository accounts;
    @Autowired PlayerLocationRepository locations;
    @Autowired JwtUtil jwt;
    @MockitoSpyBean VenueRepository venues;

    UserResponseDTO player(String phone) {
        var request = new PlayerRequestDTO(); request.setFullName("Player"); request.setPhone(phone);
        request.setEmail(phone + "@example.com"); request.setPassword("StrongPass123!");
        return users.createPlayer(request);
    }

    String fix(double latitude, double longitude, double accuracy, Instant time) throws Exception {
        return mapper.writeValueAsString(Map.of("latitude", latitude, "longitude", longitude, "accuracyMeters", accuracy, "capturedAt", time.toString()));
    }

    @Test void updatesOneRowAndIgnoresOlderFixesWithoutExposingAnotherPlayer() throws Exception {
        var first = player("9876543210"); var second = player("9876543211");
        var token = jwt.generateToken(first); Instant now = Instant.now();
        for (var body : List.of(fix(17, 78, 12, now.minusSeconds(30)), fix(18, 79, 20, now), fix(16, 77, 10, now.minusSeconds(10)))) {
            mvc.perform(put("/api/players/me/location").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        }
        assertThat(locations.count()).isEqualTo(1);
        assertThat(locations.findById(first.getUserId()).orElseThrow().getLatitude()).isEqualTo(18);
        mvc.perform(get("/api/players/me/location").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.latitude").value(18))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/players/me/location").header("Authorization", "Bearer " + jwt.generateToken(second)))
                .andExpect(status().isNotFound());
        String extraId = fix(19, 80, 10, now.plusMillis(1));
        extraId = extraId.substring(0, extraId.length() - 1) + ",\"userId\":" + second.getUserId() + "}";
        mvc.perform(put("/api/players/me/location").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(extraId)).andExpect(status().isNoContent());
        assertThat(locations.findById(second.getUserId())).isEmpty();
    }

    @Test void rejectsBadOrStaleFixesAndSearchesFromFreshRequest() throws Exception {
        var player = player("9876543210"); String token = jwt.generateToken(player); Instant now = Instant.now();
        for (String body : List.of(fix(91, 78, 10, now), fix(17, 181, 10, now), fix(17, 78, -1, now),
                fix(17, 78, 1001, now), fix(17, 78, 10, now.minusSeconds(121)), fix(17, 78, 10, now.plusSeconds(60)))) {
            mvc.perform(put("/api/players/me/location").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        assertThat(locations.count()).isZero();
        doReturn(List.of()).when(venues).findNearbyActive(17.2, 78.3);
        mvc.perform(post("/api/players/me/nearby").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(fix(17.2, 78.3, 15, Instant.now())))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(venues).findNearbyActive(17.2, 78.3);
        assertThat(locations.findById(player.getUserId()).orElseThrow().getLongitude()).isEqualTo(78.3);
    }

    @Test void preventsAccountTakeoverAndRequiresActivePlayerToken() throws Exception {
        var first = player("9876543210"); var second = player("9876543211"); String token = jwt.generateToken(first);
        mvc.perform(get("/api/players/me/location")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/players/me/location").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        for (String route : List.of("/api/users/appOwner", "/api/users/venueOwner")) {
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
            mvc.perform(post(route).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(put("/api/users/player/" + second.getUserId()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/users/" + second.getUserId()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        var adminRequest = new AppOwnerRequestDTO(); adminRequest.setFullName("Admin"); adminRequest.setPhone("9876543212"); adminRequest.setPassword("StrongPass123!");
        String admin = jwt.generateToken(users.createAppOwner(adminRequest));
        mvc.perform(get("/api/players/me/location").header("Authorization", "Bearer " + admin)).andExpect(status().isForbidden());
        mvc.perform(post("/api/users/venueOwner").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("fullName", "Owner", "phone", "9876543213", "password", "StrongPass123!"))))
                .andExpect(status().isCreated());
        var account = accounts.findById(first.getUserId()).orElseThrow(); account.setStatus("blocked"); accounts.saveAndFlush(account);
        mvc.perform(get("/api/players/me/location").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
}
