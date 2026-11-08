package dev.hrutvik.billing.adjustment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WaiveAdjustmentRequest(@NotBlank @Size(max=500) String reason) {}
