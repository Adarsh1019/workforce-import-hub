package com.workforce.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.workforce.dto.EmployeeRequest;
import com.workforce.exception.ConflictException;
import com.workforce.exception.NotFoundException;
import com.workforce.repository.DepartmentRepository;
import com.workforce.repository.EmployeeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Mock EmployeeRepository employees;
    @Mock DepartmentRepository departments;
    @InjectMocks EmployeeService service;

    private final EmployeeRequest req = new EmployeeRequest("Paula Small", "paula.small@bilearner.com", 1L,
            "Production Technician I", new BigDecimal("44000"), LocalDate.of(2023, 2, 11));

    @Test
    void createRejectsDuplicateEmail() {
        when(employees.existsByEmailIgnoreCase("paula.small@bilearner.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(ConflictException.class);
        verify(employees, never()).save(any());
    }

    @Test
    void createRejectsUnknownDepartment() {
        when(employees.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(departments.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteMissingEmployeeThrowsNotFound() {
        when(employees.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(NotFoundException.class);
    }
}
