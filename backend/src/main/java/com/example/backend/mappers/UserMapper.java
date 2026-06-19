package com.example.backend.mappers;

import com.example.backend.dtos.UserResponse;
import com.example.backend.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
