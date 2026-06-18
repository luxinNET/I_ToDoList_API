package com.example.itodo.user;

import com.example.itodo.user.dto.UserProfileResponse;
import com.example.itodo.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {

    UserProfileResponse toProfileResponse(User user);
}
