package com.workforce.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmployeeRowValidatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    private EmployeeCsvRow row(String name, String email, String salary, String date) {
        EmployeeCsvRow r = new EmployeeCsvRow();
        r.setName(name);
        r.setEmail(email);
        r.setDepartment("Sales");
        r.setRole("Area Sales Manager");
        r.setSalary(salary);
        r.setJoiningDate(date);
        return r;
    }

    @Test
    void validRowHasNoErrors() {
        assertThat(EmployeeRowValidator.validate(row("Uriah Bridges", "uriah@bilearner.com", "52000", "2019-09-20"), TODAY)).isEmpty();
    }

    @Test
    void rejectsInvalidEmail() {
        assertThat(EmployeeRowValidator.validate(row("A", "broken-email", "1", "2020-01-01"), TODAY))
                .anyMatch(e -> e.contains("email"));
    }

    @Test
    void rejectsNegativeSalary() {
        assertThat(EmployeeRowValidator.validate(row("A", "a@b.com", "-500", "2020-01-01"), TODAY))
                .contains("salary must not be negative");
    }

    @Test
    void rejectsBadDateFormat() {
        assertThat(EmployeeRowValidator.validate(row("A", "a@b.com", "100", "31/02/2020"), TODAY))
                .anyMatch(e -> e.contains("YYYY-MM-DD"));
    }

    @Test
    void rejectsMissingName() {
        assertThat(EmployeeRowValidator.validate(row("", "a@b.com", "100", "2020-01-01"), TODAY))
                .contains("name is required");
    }
}
