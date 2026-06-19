package com.example.backend.controllers;

import com.example.backend.dtos.CustomerDetailResponse;
import com.example.backend.dtos.CustomerProfileResponse;
import com.example.backend.dtos.CustomerSummaryResponse;
import com.example.backend.dtos.CustomerUpdateRequest;
import com.example.backend.entities.CustomerProfile;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.CustomerStatus;
import com.example.backend.mappers.CustomerMapper;
import com.example.backend.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("users")
public class UserRestController {

    private final CustomerService customers;
    private final CustomerMapper toResponse;

    public UserRestController(CustomerService customers, CustomerMapper toResponse) {
        this.customers = customers;
        this.toResponse = toResponse;
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Page<CustomerSummaryResponse> listCustomers(
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable page) {
        return summaryPage(customers.getAllCustomers(status, search, page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public CustomerDetailResponse findCustomer(@PathVariable int id) {
        User user = customers.getCustomerUserById(id);
        return toResponse.toDetail(user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public CustomerProfileResponse applyUpdate(
            @PathVariable int id,
            @Valid @RequestBody CustomerUpdateRequest body) {
        CustomerProfile profile = customers.updateCustomer(id, body);
        return toResponse.toProfile(profile);
    }

    private Page<CustomerSummaryResponse> summaryPage(Page<User> page) {
        return page.map(toResponse::toSummary);
    }
}
