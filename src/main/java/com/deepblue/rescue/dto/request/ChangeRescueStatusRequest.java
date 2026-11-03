package com.deepblue.rescue.dto.request;

import com.deepblue.rescue.domain.RescueStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeRescueStatusRequest(
        @NotNull(message = "Status is required")
        RescueStatus status
) {
}