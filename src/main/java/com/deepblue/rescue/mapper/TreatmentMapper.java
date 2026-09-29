package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.response.TreatmentResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TreatmentMapper {

    @Mapping(target = "animalCode", source = "animal.animalCode")
    @Mapping(target = "specialistCode", source = "specialist.professionalCode")
    TreatmentResponse toResponse(Treatment treatment);
}