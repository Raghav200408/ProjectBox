package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.entity.User;
import com.boxproject.BoxProject.constants.UserStatus;
import com.boxproject.BoxProject.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import com.boxproject.BoxProject.security.PasswordHasher;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.security.user.name=test-admin", "spring.security.user.password=test-only-password"})
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class UserControllerTests {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordHasher passwordEncoder;

    @Test
    void createsUserAndReturnsDtoWithoutPassword() throws Exception {
        create(details("+919876543210", "Arjun@Example.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.fullName").value("Arjun Kumar"))
                .andExpect(jsonPath("$.email").value("arjun@example.com"))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User user = userRepository.findAll().get(0);
        assertThat(user.getRole().getName()).isEqualTo("PLAYER");
        assertThat(user.getRole().getId()).isNotNull();
        assertThat(passwordEncoder.matches("StrongPass123!", user.getPasswordHash())).isTrue();
        assertThat(user.getPasswordHash()).isNotEqualTo("StrongPass123!");
    }

    @Test
    void readsUpdatesAndSoftDeletesUser() throws Exception {
        Long id = createId(details("+919876543210", "arjun@example.com"));
        User existing = userRepository.findById(id).orElseThrow();
        String originalHash = existing.getPasswordHash();
        userRepository.saveAndFlush(existing);

        mvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(org.hamcrest.Matchers.is(id), Long.class));

        Map<String, Object> update = details("+919876543211", "new@example.com");
        update.put("fullName", "Arjun Updated");
        update.remove("password");
        update.put("status", "blocked");
        update.put("role", "VENUE_OWNER");
        mvc.perform(put("/api/users/player/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Arjun Updated"))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.phoneVerifiedAt").doesNotExist())
                .andExpect(jsonPath("$.emailVerifiedAt").doesNotExist());
        assertThat(userRepository.findById(id).orElseThrow().getPasswordHash()).isEqualTo(originalHash);

        mvc.perform(get("/api/users/all")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/users/{id}", id).with(csrf())).andExpect(status().isNoContent());
        User deleted = userRepository.findById(id).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getStatus()).isEqualTo(UserStatus.DELETED);
        mvc.perform(get("/api/users/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/users/all")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void replacesPasswordWhenProvided() throws Exception {
        Long id = createId(details("+919876543210", null));
        Map<String, Object> update = details("+919876543210", null);
        update.put("password", "Replacement123!");
        mvc.perform(put("/api/users/player/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk());
        assertThat(passwordEncoder.matches("Replacement123!", userRepository.findById(id).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void rejectsDuplicateContactAndNormalizesInternationalPhone() throws Exception {
        createId(details("+919876543210", "arjun@example.com"));
        create(details("919876543210", "other@example.com")).andExpect(status().isConflict());
        create(details("+919876543211", "ARJUN@example.com")).andExpect(status().isConflict());
    }

    @Test
    void rejectsConflictingUpdatesButAllowsUnchangedContact() throws Exception {
        Long first = createId(details("+919876543210", "one@example.com"));
        Long second = createId(details("+919876543211", "two@example.com"));
        Map<String, Object> update = details("+919876543210", "one@example.com");
        update.remove("password");
        mvc.perform(put("/api/users/player/{id}", first).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/users/player/{id}", second).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isConflict());
    }

    @Test
    void allowsDifferentUsersWithoutEmail() throws Exception {
        create(details("+919876543210", null)).andExpect(status().isCreated());
        create(details("+919876543211", "")).andExpect(status().isCreated());
    }

    @Test
    void validatesInputAndBcryptByteLimit() throws Exception {
        Map<String, Object> invalid = details("invalid", "not-an-email");
        invalid.put("fullName", " ");
        invalid.put("password", "short");
        create(invalid).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.phone").exists());
        Map<String, Object> unicode = details("+919876543210", null);
        unicode.put("password", "अ".repeat(25));
        create(unicode).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"appOwner, APP_OWNER", "venueOwner, VENUE_OWNER", "player, PLAYER"})
    void roleSpecificRequestsAssignTheEndpointRoleAndValidateInput(String endpoint, String expectedRole) throws Exception {
        Map<String, Object> request = details("+919876543210", null);
        request.put("role", "UNTRUSTED_ROLE");
        mvc.perform(post("/api/users/" + endpoint).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value(expectedRole));

        request.put("fullName", " ");
        mvc.perform(post("/api/users/" + endpoint).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @ParameterizedTest
    @CsvSource({"appOwner, APP_OWNER", "venueOwner, VENUE_OWNER", "player, PLAYER"})
    void reusesRoleRequestForUpdatesAndRequiresPasswordOnlyOnCreation(String endpoint, String expectedRole) throws Exception {
        Map<String, Object> request = details("+919876543210", null);
        request.remove("password");
        mvc.perform(post("/api/users/" + endpoint).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.password").exists());

        request.put("password", "StrongPass123!");
        String body = mvc.perform(post("/api/users/" + endpoint).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(body).get("userId").asLong();
        String hash = userRepository.findById(id).orElseThrow().getPasswordHash();

        request.remove("password");
        request.put("fullName", "Updated name");
        mvc.perform(put("/api/users/" + endpoint + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Updated name"))
                .andExpect(jsonPath("$.role").value(expectedRole));
        assertThat(userRepository.findById(id).orElseThrow().getPasswordHash()).isEqualTo(hash);

        String wrongEndpoint = endpoint.equals("player") ? "venueOwner" : "player";
        mvc.perform(put("/api/users/" + wrongEndpoint + "/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotDeleteThroughUpdateOrSupplyBlankPassword() throws Exception {
        Long id = createId(details("+919876543210", null));
        Map<String, Object> update = details("+919876543210", null);
        update.put("status", "deleted");
        mvc.perform(put("/api/users/player/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
        update.remove("status");
        update.put("password", "        ");
        mvc.perform(put("/api/users/player/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundAndBadRequestForInvalidIdentifiers() throws Exception {
        mvc.perform(get("/api/users/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(get("/api/users/not-a-number")).andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void endpointsRemainProtectedByExistingSpringSecurity() throws Exception {
        mvc.perform(get("/api/users/all").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void existingBasicAuthenticationStillWorks() throws Exception {
        mvc.perform(get("/api/users/all").with(httpBasic("test-admin", "test-only-password")))
                .andExpect(status().isOk());
    }

    private Map<String, Object> details(String phone, String email) {
        Map<String, Object> request = new HashMap<>();
        request.put("fullName", "Arjun Kumar");
        request.put("phone", phone);
        request.put("email", email);
        request.put("password", "StrongPass123!");
        return request;
    }

    private ResultActions create(Map<String, Object> request) throws Exception {
        return mvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)));
    }

    private Long createId(Map<String, Object> request) throws Exception {
        String body = create(request).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("userId").asLong();
    }
}
