package com.rutina.rutinabackend.domain.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 50) String job,
        @Size(max = 10) String age,
        @Min(0) @Max(1) Integer gender
) {}
