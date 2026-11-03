package com.deepblue.rescue.dto.response;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;

public record AnimalResponse(
        Long id,
        String animalCode,
        String commonName,
        String scientificName,
        AnimalSex sex,
        String caseCode,
        RescueStatus rescueStatus
) {
}