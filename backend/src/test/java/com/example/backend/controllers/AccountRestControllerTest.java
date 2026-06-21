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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional 
@DisplayName("Account REST endpoints")
class AccountRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;


    private User createCustomer(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setFirstName("Test");
        user.setLastName("Customer");
        user.setRole(UserRole.CUSTOMER);
        User saved = userRepository.save(user);
        createProfile(saved, CustomerStatus.ACTIVE);
        return saved;
    }

    private CustomerProfile createProfile(User user, CustomerStatus status) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setBsn(String.format("%09d", user.getId()));
        profile.setPhoneNumber("0612345678");
        profile.setStatus(status);
        return customerProfileRepository.save(profile);
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

    private Account createAccount(User owner, String iban) {
        return createAccount(owner, iban, AccountType.CHECKING, AccountStatus.ACTIVE);
    }

    private Account createAccount(User owner, String iban, AccountType type, AccountStatus status) {
        Account account = new Account();
        account.setUser(owner);
        account.setIban(iban);
        account.setType(type);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("0.00"));
        account.setDailyTransferLimit(new BigDecimal("5000.00"));
        account.setStatus(status);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }




    @Nested
    @DisplayName("GET /accounts (employee listing)")
    class ListAllAccounts {

    @Test
    void listAll_asCustomer_returns403() throws Exception {
        User customer = createCustomer("ac-customer-forbidden@test.inholland.nl");

        mockMvc.perform(get("/accounts")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }



    @Test
    void listAll_asEmployee_seesEveryAccount() throws Exception {
        User customer1 = createCustomer("ac-emp-c1@test.inholland.nl");
        User customer2 = createCustomer("ac-emp-c2@test.inholland.nl");
        User employee  = createEmployee("ac-emp@test.inholland.nl");

        createAccount(customer1, "RHINOEMPC101");
        createAccount(customer2, "RHINOEMPC201");

        // size=100 so both accounts show up on the first page
        mockMvc.perform(get("/accounts?size=100")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].userId",
                        hasItems(customer1.getId(), customer2.getId())));
    }



    @Test
    void listAll_asEmployee_filtersByCustomerName() throws Exception {
        User matchingCustomer = createCustomer("ac-search-match@test.inholland.nl");
        User otherCustomer = createCustomer("ac-search-other@test.inholland.nl");
        User employee = createEmployee("ac-search-employee@test.inholland.nl");

        matchingCustomer.setFirstName("Alicia");
        userRepository.save(matchingCustomer);

        createAccount(matchingCustomer, "RHINOSEARCH01");
        createAccount(otherCustomer, "RHINOSEARCH02");

        mockMvc.perform(get("/accounts?name=lici&size=100")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].iban",
                        hasItem("RHINOSEARCH01")))
                .andExpect(jsonPath("$.content[*].iban",
                        not(hasItem("RHINOSEARCH02"))));
    }



    @Test
    void listAll_asEmployee_composesNameAndType() throws Exception {
        User customer = createCustomer("ac-compose-match@test.inholland.nl");
        User employee = createEmployee("ac-compose-employee@test.inholland.nl");

        createAccount(customer, "RHINOCOMPOSECHK", AccountType.CHECKING, AccountStatus.ACTIVE);
        createAccount(customer, "RHINOCOMPOSESAV", AccountType.SAVINGS, AccountStatus.ACTIVE);

        mockMvc.perform(get("/accounts?name=compose&type=SAVINGS&size=100")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type",
                        everyItem(is("SAVINGS"))))
                .andExpect(jsonPath("$.content[*].iban",
                        hasItem("RHINOCOMPOSESAV")))
                .andExpect(jsonPath("$.content[*].iban",
                        not(hasItem("RHINOCOMPOSECHK"))));
    }



    @Test
    void listAll_unauthenticated_returns401() throws Exception {
        // No Authorization header — Spring Security rejects the request before reaching the controller.
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isUnauthorized());
    }


    }
    @Nested
    @DisplayName("GET /accounts/me")
    class ListMyAccounts {

    @Test
    void listMine_returnsOnlyOwnAccounts() throws Exception {
        User customer1 = createCustomer("ac-c1@test.inholland.nl");
        User customer2 = createCustomer("ac-c2@test.inholland.nl");

        createAccount(customer1, "RHINOC101");
        createAccount(customer2, "RHINOC201");

        mockMvc.perform(get("/accounts/me?size=100")
                        .header("Authorization", bearerToken(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].iban",
                        hasItem("RHINOC101")))
                .andExpect(jsonPath("$.content[*].iban",
                        not(hasItem("RHINOC201"))))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist());
    }



    @Test
    void listMine_ignoresPrivateQueryParams() throws Exception {
        User customer1 = createCustomer("ac-name-c1@test.inholland.nl");
        User customer2 = createCustomer("ac-name-c2-target@test.inholland.nl");

        createAccount(customer1, "RHINONAMEC101");
        createAccount(customer2, "RHINONAMEC201");

        mockMvc.perform(get("/accounts/me?userId=" + customer2.getId() + "&name=target&size=100")
                        .header("Authorization", bearerToken(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].iban",
                        hasItem("RHINONAMEC101")))
                .andExpect(jsonPath("$.content[*].iban",
                        not(hasItem("RHINONAMEC201"))));
    }



    @Test
    void listMine_whenPending_returns403() throws Exception {
        User customer = createCustomer("ac-pending@test.inholland.nl");
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.PENDING);
        customerProfileRepository.save(profile);

        createAccount(customer, "RHINOPENDING01");

        mockMvc.perform(get("/accounts/me")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }


    }
    @Nested
    @DisplayName("GET /accounts/transfer-targets")
    class TransferTargetSearch {

    @Test
    void searchTargets_byIbanPrefix_returnsMatch() throws Exception {
        User searchingCustomer = createCustomer("ac-lookup-iban-searcher@test.inholland.nl");
        User matchingCustomer = createCustomer("ac-lookup-iban-match@test.inholland.nl");

        matchingCustomer.setFirstName("Iban");
        matchingCustomer.setLastName("Holder");
        userRepository.saveAndFlush(matchingCustomer);

        createAccount(searchingCustomer, "RHINOIBANOWN");
        createAccount(matchingCustomer, "NL99INHO0000001234", AccountType.CHECKING, AccountStatus.ACTIVE);

        mockMvc.perform(get("/accounts/transfer-targets")
                        .param("name", "nl99")
                        .param("size", "100")
                        .header("Authorization", bearerToken(searchingCustomer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].iban").value("NL99INHO0000001234"));
    }


    @Test
    void searchTargets_byFirstAndLastName_returnsMatch() throws Exception {
        User searchingCustomer = createCustomer("ac-lookup-multi-searcher@test.inholland.nl");
        User matchingCustomer = createCustomer("ac-lookup-multi-match@test.inholland.nl");

        matchingCustomer.setFirstName("Xylophone");
        matchingCustomer.setLastName("Zenith");
        userRepository.saveAndFlush(matchingCustomer);

        createAccount(searchingCustomer, "RHINOMULTIOWN");
        createAccount(matchingCustomer, "RHINOMULTICHK", AccountType.CHECKING, AccountStatus.ACTIVE);

        mockMvc.perform(get("/accounts/transfer-targets")
                        .param("name", "xylophone zen")
                        .param("size", "100")
                        .header("Authorization", bearerToken(searchingCustomer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Xylophone"))
                .andExpect(jsonPath("$.content[0].lastName").value("Zenith"));
    }


    @Test
    void searchTargets_byName_returnsSafeFieldsOnly() throws Exception {
        User searchingCustomer = createCustomer("ac-lookup-searcher@test.inholland.nl");
        User matchingCustomer = createCustomer("ac-lookup-match@test.inholland.nl");

        matchingCustomer.setFirstName("Recipient");
        matchingCustomer.setLastName("Lookup");
        userRepository.save(matchingCustomer);

        createAccount(searchingCustomer, "RHINOLOOKUPOWN");
        createAccount(matchingCustomer, "RHINOLOOKUPCHK", AccountType.CHECKING, AccountStatus.ACTIVE);
        createAccount(matchingCustomer, "RHINOLOOKUPSAV", AccountType.SAVINGS, AccountStatus.ACTIVE);

        mockMvc.perform(get("/accounts/transfer-targets?name=Recipient&size=100")
                        .header("Authorization", bearerToken(searchingCustomer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].iban").value("RHINOLOOKUPCHK"))
                .andExpect(jsonPath("$.content[0].firstName").value("Recipient"))
                .andExpect(jsonPath("$.content[0].lastName").value("Lookup"))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].balance").doesNotExist())
                .andExpect(jsonPath("$.content[0].absoluteTransferLimit").doesNotExist())
                .andExpect(jsonPath("$.content[0].dailyTransferLimit").doesNotExist());
    }



    @Test
    void searchTargets_hidesInactiveAccounts() throws Exception {
        User searchingCustomer = createCustomer("ac-lookup-inactive-searcher@test.inholland.nl");
        User matchingCustomer = createCustomer("ac-lookup-inactive-match@test.inholland.nl");

        matchingCustomer.setFirstName("Inactive");
        matchingCustomer.setLastName("Lookup");
        userRepository.save(matchingCustomer);

        createAccount(matchingCustomer, "RHINOINACTIVECHK", AccountType.CHECKING, AccountStatus.CLOSED);

        mockMvc.perform(get("/accounts/transfer-targets?name=Inactive&size=100")
                        .header("Authorization", bearerToken(searchingCustomer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }



    @Test
    void searchTargets_ignoresPrivateFilters() throws Exception {
        User searchingCustomer = createCustomer("ac-lookup-filter-searcher@test.inholland.nl");
        User matchingCustomer = createCustomer("ac-lookup-filter-match@test.inholland.nl");

        matchingCustomer.setFirstName("Filtered");
        matchingCustomer.setLastName("Lookup");
        userRepository.save(matchingCustomer);

        createAccount(searchingCustomer, "RHINOFILTEROWN");
        createAccount(matchingCustomer, "RHINOFILTERCHK", AccountType.CHECKING, AccountStatus.ACTIVE);

        mockMvc.perform(get("/accounts/transfer-targets?name=Filtered&userId=" + searchingCustomer.getId() + "&type=SAVINGS&status=CLOSED&size=100")
                        .header("Authorization", bearerToken(searchingCustomer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].iban").value("RHINOFILTERCHK"))
                .andExpect(jsonPath("$.content[0].firstName").value("Filtered"))
                .andExpect(jsonPath("$.content[0].lastName").value("Lookup"))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].balance").doesNotExist());
    }



    @Test
    void searchTargets_whenClosed_returns403() throws Exception {
        User customer = createCustomer("ac-closed@test.inholland.nl");
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.CLOSED);
        customerProfileRepository.save(profile);

        createAccount(customer, "RHINOCLOSED01");

        mockMvc.perform(get("/accounts/transfer-targets?name=Test")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }


    }
    @Nested
    @DisplayName("PATCH /accounts/{iban}")
    class PatchAccount {

    @Test
    void patch_asEmployee_updatesLimitsAndStatus() throws Exception {
        User customer = createCustomer("ac-patch-customer@test.inholland.nl");
        User employee = createEmployee("ac-patch-employee@test.inholland.nl");
        Account account = createAccount(customer, "RHINOPATCH01");

        // must be zero before closing
        account.setBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        Map<String, Object> request = new HashMap<>();
        request.put("absoluteTransferLimit", "200.00");
        request.put("dailyTransferLimit", "3000.00");
        request.put("status", "CLOSED");

        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(account.getIban()))
                .andExpect(jsonPath("$.absoluteTransferLimit").value(200.00))
                .andExpect(jsonPath("$.dailyTransferLimit").value(3000.00))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }



    @Test
    void patch_closeWithBalance_returns400() throws Exception {
        User customer = createCustomer("ac-close-balance@test.inholland.nl");
        User employee = createEmployee("ac-close-balance-emp@test.inholland.nl");
        Account account = createAccount(customer, "RHINOCLOSEBAL01");

        Map<String, Object> request = new HashMap<>();
        request.put("status", "CLOSED");

        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void patch_asCustomer_returns403() throws Exception {
        User customer = createCustomer("ac-forbidden@test.inholland.nl");
        Account account = createAccount(customer, "RHINOFORBID01");

        Map<String, Object> request = new HashMap<>();
        request.put("status", "CLOSED");

        // only employees can update accounts
        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }



    @Test
    void patch_emptyBody_returns400() throws Exception {
        User customer = createCustomer("ac-empty@test.inholland.nl");
        User employee = createEmployee("ac-empty-emp@test.inholland.nl");
        Account account = createAccount(customer, "RHINOEMPTY01");

        // empty body should fail validation
        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }



    @Test
    void patch_negativeLimit_returns400() throws Exception {
        User customer = createCustomer("ac-neglimit@test.inholland.nl");
        User employee = createEmployee("ac-neglimit-emp@test.inholland.nl");
        Account account = createAccount(customer, "RHINONEGLIM01");

        Map<String, Object> request = new HashMap<>();
        request.put("absoluteTransferLimit", -1.00);

        // negative limits are not allowed
        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void patch_unknownIban_returns404() throws Exception {
        User employee = createEmployee("ac-notfound-emp@test.inholland.nl");

        Map<String, Object> request = new HashMap<>();
        request.put("absoluteTransferLimit", "500.00");

        // unknown IBAN should be 404
        mockMvc.perform(patch("/accounts/NL99XXXX0000000000")
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    }
}
