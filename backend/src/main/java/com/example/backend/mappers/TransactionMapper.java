package com.example.backend.mappers;

import com.example.backend.dtos.TransactionCreateRequest;
import com.example.backend.dtos.TransactionResponse;
import com.example.backend.entities.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "initiatedBy.id", target = "initiatedByUserId")
    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "initiatedBy", ignore = true)
    @Mapping(target = "timestamp",   ignore = true)
    Transaction toEntity(TransactionCreateRequest request);
}
