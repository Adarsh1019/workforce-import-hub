package com.workforce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 160) String email,
        @NotNull Long departmentId,
        @NotBlank @Size(max = 120) String role,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal salary,
        @NotNull @PastOrPresent LocalDate joiningDate) {}
