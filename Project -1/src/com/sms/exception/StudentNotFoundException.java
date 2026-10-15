package com.sms.exception;

/** Thrown when no student exists for the given ID. */
public class StudentNotFoundException extends Exception {

    public StudentNotFoundException(String studentId) {
        super("No student found with ID: " + studentId);
    }
}
