package com.workforce.batch;

/** Thrown for a bad CSV row; the step skips the row and records the reason. */
public class RowValidationException extends RuntimeException {
    public RowValidationException(String message) { super(message); }
}
