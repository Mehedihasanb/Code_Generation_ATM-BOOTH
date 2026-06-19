package com.example.backend.controllers;

import com.example.backend.entities.CustomerProfile;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.CustomerStatus;
import com.example.backend.entities.enums.UserRole;
import com.example.backend.repositories.CustomerProfileRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.util.JwtUtil;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // rolls back DB changes after each test
@DisplayName("Authentication REST endpoints")
class AuthRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;


    // password needs to be hashed for login to work in tests
    private User createCustomerWithPassword(String email, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.CUSTOMER);
        return userRepository.save(user);
    }

    private CustomerProfile createProfile(User user, String bsn) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setBsn(bsn);
        profile.setPhoneNumber("0612345678");
        profile.setStatus(CustomerStatus.PENDING);
        return customerProfileRepository.save(profile);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }





    @Nested
    @DisplayName("POST /auth/register")
    class SignUp {

    @Test
    void signUp_validPayload_returns200() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "booth-auth-register@test.inholland.nl");
        request.put("password", "Password1!");
        request.put("firstName", "Alice");
        request.put("lastName", "Smith");
        request.put("bsn", "555666777");
        request.put("phoneNumber", "0612345678");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("booth-auth-register@test.inholland.nl"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.id").isNumber());
    }



    @Test
    void signUp_missingEmail_returns400() throws Exception {
        Map<String, Object> request = new HashMap<>();
        // no email field — should fail validation
        request.put("password", "Password1!");
        request.put("firstName", "Alice");
        request.put("lastName", "Smith");
        request.put("bsn", "123456789");
        request.put("phoneNumber", "0612345678");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void signUp_weakPassword_returns400() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "booth-auth-weak@test.inholland.nl");
        // too simple — no uppercase or special character
        request.put("password", "weakpassword");
        request.put("firstName", "Alice");
        request.put("lastName", "Smith");
        request.put("bsn", "123456789");
        request.put("phoneNumber", "0612345678");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void signUp_duplicateEmail_returns400() throws Exception {
        // create a user with this email first so the second registration fails
        createCustomerWithPassword("booth-auth-dup@test.inholland.nl", "Password1!");

        Map<String, Object> request = new HashMap<>();
        request.put("email", "booth-auth-dup@test.inholland.nl");
        request.put("password", "Password1!");
        request.put("firstName", "Alice");
        request.put("lastName", "Smith");
        request.put("bsn", "999888777");
        request.put("phoneNumber", "0612345679");

        // duplicate email = 400
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    }
    @Nested
    @DisplayName("POST /auth/login")
    class Login {

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        User user = createCustomerWithPassword("booth-auth-login@test.inholland.nl", "Password1!");
        createProfile(user, "555666888");

        Map<String, Object> request = new HashMap<>();
        request.put("email", "booth-auth-login@test.inholland.nl");
        request.put("password", "Password1!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token.value").isString())
                .andExpect(jsonPath("$.token.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("booth-auth-login@test.inholland.nl"));
    }



    @Test
    void login_wrongPassword_returns401() throws Exception {
        createCustomerWithPassword("booth-auth-wrongpass@test.inholland.nl", "Password1!");

        Map<String, Object> request = new HashMap<>();
        request.put("email", "booth-auth-wrongpass@test.inholland.nl");
        // wrong password
        request.put("password", "WrongPassword1!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }



    @Test
    void login_unknownEmail_returns401() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "nobody@test.inholland.nl");
        request.put("password", "Password1!");

        // unknown email should return 401
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }


    }
    @Nested
    @DisplayName("GET /auth/me")
    class CurrentUser {

    @Test
    void currentUser_authenticated_returnsProfile() throws Exception {
        User user = createCustomerWithPassword("booth-auth-me@test.inholland.nl", "Password1!");
        createProfile(user, "444555666");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("booth-auth-me@test.inholland.nl"))
                .andExpect(jsonPath("$.id").value(user.getId()));
    }



    @Test
    void currentUser_unauthenticated_returns401() throws Exception {
        // no token = 401
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    }
}
