package com.example.backend.mappers;

import com.example.backend.dtos.EmployeeAccountResponse;
import com.example.backend.dtos.OwnAccountResponse;
import com.example.backend.dtos.TransferTargetResponse;
import com.example.backend.entities.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    EmployeeAccountResponse toEmployeeResponse(Account account);

    OwnAccountResponse toOwnResponse(Account account);

    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    TransferTargetResponse toTransferTargetResponse(Account account);
}
