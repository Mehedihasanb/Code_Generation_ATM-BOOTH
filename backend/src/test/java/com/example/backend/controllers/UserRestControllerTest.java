package com.example.backend.controllers;

import com.example.backend.entities.Account;
import com.example.backend.entities.CustomerProfile;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.AccountStatus;
import com.example.backend.entities.enums.AccountType;
import com.example.backend.entities.enums.CustomerStatus;
import com.example.backend.entities.enums.UserRole;
import com.example.backend.repositories.AccountRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // rolls back DB changes after each test
@DisplayName("User management REST endpoints")
class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User createCustomer(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setFirstName("Test");
        user.setLastName("Customer");
        user.setRole(UserRole.CUSTOMER);
        return userRepository.save(user);
    }

    private User createEmployee(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setFirstName("Test");
        user.setLastName("Employee");
        user.setRole(UserRole.EMPLOYEE);
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

    // private CustomerProfile createActiveProfile(User user, String bsn) {
    // CustomerProfile profile = new CustomerProfile();
    // profile.setUser(user);
    // profile.setBsn(bsn);
    // profile.setPhoneNumber("0612345678");
    // profile.setStatus(CustomerStatus.ACTIVE);
    // return customerProfileRepository.save(profile);
    // }
    // Commented by Fernando bc showed that it was unused

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }

    @Nested
    @DisplayName("GET /users")
    class ListCustomers {

        @Test
        void list_unauthenticated_returns401() throws Exception {
            // no token = 401
            mockMvc.perform(get("/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void list_asCustomer_returns403() throws Exception {
            User customer = createCustomer("usr-getall-customer@test.inholland.nl");
            createProfile(customer, "100000001");

            // customers can't access this endpoint
            mockMvc.perform(get("/users")
                    .header("Authorization", bearerToken(customer)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void list_asEmployee_returnsPagedResults() throws Exception {
            User customer = createCustomer("usr-listed@test.inholland.nl");
            createProfile(customer, "100000002");
            User employee = createEmployee("usr-getall-employee@test.inholland.nl");

            mockMvc.perform(get("/users")
                    .header("Authorization", bearerToken(employee)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void list_asEmployee_emptySearch_returnsNoRows() throws Exception {
            User employee = createEmployee("usr-empty-employee@test.inholland.nl");

            // no customers in this test's transaction, so search should return nothing
            mockMvc.perform(get("/users?search=nonexistent-name-xyz987")
                    .header("Authorization", bearerToken(employee)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

    }

    @Nested
    @DisplayName("GET /users/{id}")
    class CustomerDetail {

        @Test
        void detail_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/users/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void detail_asCustomer_returns403() throws Exception {
            User customer = createCustomer("usr-getbyid-customer@test.inholland.nl");

            // Customers cannot look up user details — only employees can.
            mockMvc.perform(get("/users/{id}", customer.getId())
                    .header("Authorization", bearerToken(customer)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void detail_asEmployee_returnsCustomer() throws Exception {
            User customer = createCustomer("usr-detail-customer@test.inholland.nl");
            createProfile(customer, "100000003");
            User employee = createEmployee("usr-detail-employee@test.inholland.nl");

            mockMvc.perform(get("/users/{id}", customer.getId())
                    .header("Authorization", bearerToken(employee)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(customer.getId()))
                    .andExpect(jsonPath("$.email").value(customer.getEmail()));
        }

        @Test
        void detail_unknownId_returns404() throws Exception {
            User employee = createEmployee("usr-missing-detail-employee@test.inholland.nl");

            mockMvc.perform(get("/users/{id}", 999999)
                    .header("Authorization", bearerToken(employee)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("No customer with id 999999"));
        }

        @Test
        void detail_employeeId_returns404() throws Exception {
            User employee = createEmployee("usr-employee-detail-employee@test.inholland.nl");

            mockMvc.perform(get("/users/{id}", employee.getId())
                    .header("Authorization", bearerToken(employee)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("No customer with id " + employee.getId()));
        }

    }

    @Nested
    @DisplayName("PATCH /users/{id}")
    class UpdateCustomer {

        @Test
        void patch_unauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/users/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void patch_asCustomer_returns403() throws Exception {
            User customer = createCustomer("usr-patch-forbidden@test.inholland.nl");

            // customers can't update users
            mockMvc.perform(patch("/users/{id}", customer.getId())
                    .header("Authorization", bearerToken(customer))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patch_asEmployee_updatesProfileFields() throws Exception {
            User customer = createCustomer("usr-patch-customer@test.inholland.nl");
            createProfile(customer, "100000004");
            User employee = createEmployee("usr-patch-employee@test.inholland.nl");

            Map<String, Object> request = new HashMap<>();
            request.put("firstName", "Updated");
            request.put("lastName", "Name");
            request.put("phoneNumber", "0698765432");

            mockMvc.perform(patch("/users/{id}", customer.getId())
                    .header("Authorization", bearerToken(employee))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phoneNumber").value("0698765432"));
        }

        @Test
        void patch_approve_setsStatusActive() throws Exception {
            User customer = createCustomer("usr-approve-customer@test.inholland.nl");
            createProfile(customer, "100000005");
            User employee = createEmployee("usr-approve-employee@test.inholland.nl");

            Map<String, Object> request = new HashMap<>();
            request.put("status", "ACTIVE");
            request.put("absoluteTransferLimit", "1000.00");
            request.put("dailyTransferLimit", "500.00");

            mockMvc.perform(patch("/users/{id}", customer.getId())
                    .header("Authorization", bearerToken(employee))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void patch_approve_provisionsTwoAccounts() throws Exception {
            User customer = createCustomer("usr-accounts-customer@test.inholland.nl");
            createProfile(customer, "100000006");
            User employee = createEmployee("usr-accounts-employee@test.inholland.nl");

            Map<String, Object> request = new HashMap<>();
            request.put("status", "ACTIVE");
            request.put("absoluteTransferLimit", "1000.00");
            request.put("dailyTransferLimit", "500.00");

            mockMvc.perform(patch("/users/{id}", customer.getId())
                    .header("Authorization", bearerToken(employee))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Activation must create exactly one CHECKING and one SAVINGS account for the
            // customer.
            List<Account> accounts = accountRepository.findByUser_Id(customer.getId(), Pageable.unpaged()).getContent();
            long checkingCount = accounts.stream().filter(a -> a.getType() == AccountType.CHECKING).count();
            long savingsCount = accounts.stream().filter(a -> a.getType() == AccountType.SAVINGS).count();
            assertEquals(1, checkingCount, "Expected 1 CHECKING account");
            assertEquals(1, savingsCount, "Expected 1 SAVINGS account");
            accounts.forEach(a -> assertEquals(AccountStatus.ACTIVE, a.getStatus()));
        }

        @Test
        void patch_invalidPhone_returns400() throws Exception {
            User customer = createCustomer("usr-invalid-phone@test.inholland.nl");
            createProfile(customer, "100000007");
            User employee = createEmployee("usr-invalid-phone-emp@test.inholland.nl");

            Map<String, Object> request = new HashMap<>();
            request.put("phoneNumber", "not-a-phone");

            mockMvc.perform(patch("/users/{id}", customer.getId())
                    .header("Authorization", bearerToken(employee))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void patch_negativeLimit_returns400() throws Exception {
            User customer = createCustomer("usr-neg-limit@test.inholland.nl");
            createProfile(customer, "100000008");
            User employee = createEmployee("usr-neg-limit-emp@test.inholland.nl");

            Map<String, Object> request = new HashMap<>();
            request.put("absoluteTransferLimit", "-50.00");

            mockMvc.perform(patch("/users/{id}", customer.getId())
                    .header("Authorization", bearerToken(employee))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

    }
}
