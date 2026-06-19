package com.example.backend.repositories;

import com.example.backend.entities.CustomerProfile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerProfileRepository extends CrudRepository<CustomerProfile, Integer> {

    CustomerProfile findByUser_Id(int userId);
}


