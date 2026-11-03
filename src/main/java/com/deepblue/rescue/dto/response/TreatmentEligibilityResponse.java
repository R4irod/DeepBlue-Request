package com.deepblue.rescue.dto.response;

public record TreatmentEligibilityResponse(
        String animalCode,
        boolean eligible
) {
}