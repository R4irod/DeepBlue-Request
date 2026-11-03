package com.deepblue.rescue.dto.response;

import com.deepblue.rescue.domain.RescueStatus;

import java.time.LocalDate;

public record RescueCaseResponse(
        Long id,
        String caseCode,
        LocalDate rescueDate,
        String rescueLocation,
        RescueStatus status,
        String centerCode,
        String animalCode
) {
}
