package com.sms.exception;

/** Thrown when adding a student whose ID is already taken. */
public class DuplicateStudentException extends Exception {

    public DuplicateStudentException(String studentId) {
        super("A student with ID " + studentId + " already exists.");
    }
}
