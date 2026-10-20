package com.workforce.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Pure validation rules for an imported row (easy to unit test). */
public final class EmployeeRowValidator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private EmployeeRowValidator() {}

    public static List<String> validate(EmployeeCsvRow r, LocalDate today) {
        List<String> errors = new ArrayList<>();
        if (blank(r.getName())) errors.add("name is required");
        else if (r.getName().trim().length() > 120) errors.add("name is longer than 120 characters");
        if (blank(r.getEmail())) errors.add("email is required");
        else if (!EMAIL.matcher(r.getEmail().trim()).matches()) errors.add("email '" + r.getEmail() + "' is invalid");
        if (blank(r.getDepartment())) errors.add("department is required");
        if (blank(r.getRole())) errors.add("role is required");
        if (blank(r.getSalary())) errors.add("salary is required");
        else {
            try {
                if (new BigDecimal(r.getSalary().trim()).signum() < 0) errors.add("salary must not be negative");
            } catch (NumberFormatException e) {
                errors.add("salary '" + r.getSalary() + "' is not a number");
            }
        }
        if (blank(r.getJoiningDate())) errors.add("joiningDate is required");
        else {
            try {
                if (LocalDate.parse(r.getJoiningDate().trim()).isAfter(today)) errors.add("joiningDate is in the future");
            } catch (DateTimeParseException e) {
                errors.add("joiningDate '" + r.getJoiningDate() + "' must be YYYY-MM-DD");
            }
        }
        return errors;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
