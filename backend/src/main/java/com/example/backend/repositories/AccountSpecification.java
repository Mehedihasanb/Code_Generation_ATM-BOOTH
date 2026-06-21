package com.example.backend.repositories;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import com.example.backend.dtos.AccountQuery;
import com.example.backend.entities.Account;
import com.example.backend.entities.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AccountSpecification {

    private AccountSpecification() {}

    public static Specification<Account> fromQuery(AccountQuery query) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), query.getUserId()));
            }
            if (query.getType() != null) {
                predicates.add(cb.equal(root.get("type"), query.getType()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getIban() != null) {
                predicates.add(cb.equal(root.get("iban"), query.getIban()));
            }
            if (query.getName() != null) {
                Join<Account, User> user = root.join("user");
                String search = "%" + query.getName().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(user.get("firstName")), search),
                        cb.like(cb.lower(user.get("lastName")), search),
                        cb.like(cb.lower(user.get("email")), search)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Account> forTransferTargetSearch(int excludeUserId, String searchTerm) {
        return (root, q, cb) -> {
            Join<Account, User> user = root.join("user");
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.notEqual(user.get("id"), excludeUserId));
            predicates.add(cb.equal(root.get("type"), com.example.backend.entities.enums.AccountType.CHECKING));
            predicates.add(cb.equal(root.get("status"), com.example.backend.entities.enums.AccountStatus.ACTIVE));

            String normalized = searchTerm.trim().replaceAll("\\s+", " ");
            if (normalized.isEmpty()) {
                return cb.disjunction();
            }

            var fullName = cb.lower(cb.concat(cb.concat(user.get("firstName"), " "), user.get("lastName")));
            String lowered = normalized.toLowerCase();
            String containsPattern = "%" + lowered + "%";

            List<Predicate> searchStrategies = new ArrayList<>();
            searchStrategies.add(cb.like(cb.lower(root.get("iban")), containsPattern));
            searchStrategies.add(cb.like(fullName, containsPattern));

            if (!normalized.contains(" ")) {
                searchStrategies.add(cb.or(
                        cb.like(cb.lower(user.get("firstName")), containsPattern),
                        cb.like(cb.lower(user.get("lastName")), containsPattern)
                ));
            } else {
                List<Predicate> tokenPredicates = new ArrayList<>();
                for (String token : normalized.split(" ")) {
                    if (token.isBlank()) {
                        continue;
                    }
                    String tokenPattern = "%" + token.toLowerCase() + "%";
                    tokenPredicates.add(cb.or(
                            cb.like(cb.lower(user.get("firstName")), tokenPattern),
                            cb.like(cb.lower(user.get("lastName")), tokenPattern),
                            cb.like(fullName, tokenPattern),
                            cb.like(cb.lower(root.get("iban")), tokenPattern)
                    ));
                }
                if (!tokenPredicates.isEmpty()) {
                    searchStrategies.add(cb.and(tokenPredicates.toArray(new Predicate[0])));
                }
            }

            predicates.add(cb.or(searchStrategies.toArray(new Predicate[0])));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
