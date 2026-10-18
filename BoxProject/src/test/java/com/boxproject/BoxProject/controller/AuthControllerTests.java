package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.constants.UserStatus;
import com.boxproject.BoxProject.model.PlayerRequestDTO;
import com.boxproject.BoxProject.repository.UserRepository;
import com.boxproject.BoxProject.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {
    @Autowired MockMvc mvc;
    @Autowired UserService service;
    @Autowired UserRepository users;
    @Autowired ObjectMapper mapper;
    @Value("${jwt.secret}") String secret;

    private Long createUser() {
        PlayerRequestDTO request = new PlayerRequestDTO();
        request.setFullName("Login User");
        request.setPhone("9876543210");
        request.setEmail("login@example.com");
        request.setPassword("StrongPass123!");
        return service.createPlayer(request).getUserId();
    }

    private String login(String email, String password, int expectedStatus) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void logsInWithEmailAndUsesTokenWithoutSessionOrCsrf() throws Exception {
        Long id = createUser();
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", "LOGIN@EXAMPLE.COM", "password", "StrongPass123!"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        String token = mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/users/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(put("/api/users/player/{id}", id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated\",\"phone\":\"9876543210\",\"email\":\"login@example.com\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/users/{id}", id)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongPasswordsUnknownEmailsAndInvalidInput() throws Exception {
        createUser();
        String wrong = login("login@example.com", "WrongPassword", 401);
        String unknown = login("missing@example.com", "WrongPassword", 401);
        assertThat(mapper.readTree(wrong).get("message").asText())
                .isEqualTo(mapper.readTree(unknown).get("message").asText());
        login("invalid-email", "StrongPass123!", 400);
        login("login@example.com", "", 400);
        login("login@example.com", "अ".repeat(25), 400);
    }

    @Test
    void rejectsBlockedAndDeletedAccountsIncludingPreviouslyIssuedTokens() throws Exception {
        Long id = createUser();
        String token = mapper.readTree(login("login@example.com", "StrongPass123!", 200)).get("token").asText();
        var user = users.findById(id).orElseThrow();
        user.setStatus(UserStatus.BLOCKED);
        users.saveAndFlush(user);
        login("login@example.com", "StrongPass123!", 401);
        mvc.perform(get("/api/users/all").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        service.deleteUser(id);
        login("login@example.com", "StrongPass123!", 401);
        mvc.perform(get("/api/users/all").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedExpiredAndIncorrectlySignedTokens() throws Exception {
        Long id = createUser();
        var key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder().issuer("boxproject").subject(id.toString())
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60))).signWith(key).compact();
        String forged = Jwts.builder().issuer("boxproject").subject(id.toString())
                .issuedAt(new Date()).expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor("different-secret-at-least-thirty-two-bytes".getBytes(StandardCharsets.UTF_8))).compact();
        for (String token : new String[]{"invalid.jwt.token", expired, forged}) {
            mvc.perform(get("/api/users/all").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }
}
