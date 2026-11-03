package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.dto.response.RescueCaseResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RescueCaseMapper {

    @Mapping(target = "centerCode", source = "rescueCenter.code")
    @Mapping(target = "animalCode", source = "animal.animalCode")
    RescueCaseResponse toResponse(RescueCase rescueCase);
}