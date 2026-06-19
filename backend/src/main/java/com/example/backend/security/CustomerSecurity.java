package com.example.backend.security;

import com.example.backend.entities.CustomerProfile;
import com.example.backend.entities.User;
import com.example.backend.entities.enums.CustomerStatus;
import com.example.backend.entities.enums.UserRole;
import com.example.backend.repositories.CustomerProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("customerSecurity")
public class CustomerSecurity {

    private final CustomerProfileRepository customerProfileRepository;

    public CustomerSecurity(CustomerProfileRepository customerProfileRepository) {
        this.customerProfileRepository = customerProfileRepository;
    }

    public boolean isActiveCustomer(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        if (user.getRole() != UserRole.CUSTOMER) {
            return false;
        }

        CustomerProfile profile = customerProfileRepository.findByUser_Id(user.getId());
        return profile != null && profile.getStatus() == CustomerStatus.ACTIVE;
    }
}
