package com.example.backend.repositories;

import com.example.backend.dtos.AccountQuery;
import com.example.backend.entities.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByIban(String iban);

    boolean existsByIban(String iban);

    Page<Account> findByUser_Id(int userId, Pageable pageable);

    default Page<Account> findAllFiltered(AccountQuery query, Pageable pageable) {
        return findAll(AccountSpecification.fromQuery(query), pageable);
    }
}
