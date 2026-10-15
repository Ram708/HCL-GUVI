package com.sms.util;

import java.util.regex.Pattern;

/**
 * Validation rules shared by the console and web interfaces. Each check
 * returns {@code null} when the value is valid, otherwise an error message.
 */
public final class Validator {

    public static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    public static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");
    public static final Pattern STUDENT_ID = Pattern.compile("^[A-Za-z0-9-]{2,15}$");
    public static final Pattern NAME = Pattern.compile("^[A-Za-z .'-]{2,50}$");

    public static final int MIN_AGE = 5;
    public static final int MAX_AGE = 100;
    public static final double MIN_MARKS = 0;
    public static final double MAX_MARKS = 100;

    private Validator() {
    }

    public static String checkStudentId(String v) {
        return v != null && STUDENT_ID.matcher(v.trim()).matches()
                ? null : "ID must be 2-15 letters, digits or '-' (e.g. S101).";
    }

    public static String checkName(String v) {
        return v != null && NAME.matcher(v.trim()).matches()
                ? null : "Name must be 2-50 letters (spaces, . ' - allowed).";
    }

    public static String checkEmail(String v) {
        return v != null && EMAIL.matcher(v.trim()).matches()
                ? null : "Enter a valid email (e.g. name@example.com).";
    }

    public static String checkPhone(String v) {
        return v != null && PHONE.matcher(v.trim()).matches()
                ? null : "Enter a valid 10-digit mobile number starting with 6-9.";
    }

    public static String checkCourse(String v) {
        return v != null && !v.trim().isEmpty() && v.trim().length() <= 40
                ? null : "Course is required (max 40 characters).";
    }

    public static String checkAge(String v) {
        try {
            int n = Integer.parseInt(v.trim());
            if (n >= MIN_AGE && n <= MAX_AGE) return null;
        } catch (RuntimeException ignored) {
            // fall through to the error message
        }
        return "Age must be a whole number between " + MIN_AGE + " and " + MAX_AGE + ".";
    }

    public static String checkMarks(String v) {
        try {
            double d = Double.parseDouble(v.trim());
            if (d >= MIN_MARKS && d <= MAX_MARKS) return null;
        } catch (RuntimeException ignored) {
            // fall through to the error message
        }
        return "Marks must be a number between 0 and 100.";
    }
}
