package com.deepblue.rescue.dto.request;

import com.deepblue.rescue.domain.TreatmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTreatmentRequest(
        @NotBlank(message = "Animal code is required")
        String animalCode,

        @NotBlank(message = "Specialist code is required")
        String specialistCode,

        @NotNull(message = "Treatment date is required")
        @PastOrPresent(message = "Treatment date cannot be in the future")
        LocalDateTime performedAt,

        @NotNull(message = "Treatment type is required")
        TreatmentType type,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 500,
              message = "Description must contain between 10 and 500 characters")
        String description
) {
}