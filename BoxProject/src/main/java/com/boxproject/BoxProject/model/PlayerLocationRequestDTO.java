package com.boxproject.BoxProject.model;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record PlayerLocationRequestDTO(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotNull @DecimalMin("0") @DecimalMax("1000") Double accuracyMeters,
        @NotNull Instant capturedAt) {
}
