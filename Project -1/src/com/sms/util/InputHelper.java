package com.sms.util;

import java.util.NoSuchElementException;
import java.util.Scanner;

import static com.sms.util.Validator.EMAIL;
import static com.sms.util.Validator.NAME;
import static com.sms.util.Validator.PHONE;
import static com.sms.util.Validator.STUDENT_ID;

/**
 * Reads and validates console input. Every method keeps asking until the user
 * enters a valid value.
 */
public class InputHelper {

    /** Thrown when the input stream is closed (e.g. Ctrl+Z / Ctrl+D). */
    public static class InputClosedException extends RuntimeException {
        public InputClosedException() {
            super("Input stream closed");
        }
    }

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            throw new InputClosedException();
        }
    }

    public String readNonEmpty(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) return value;
            System.out.println("  ! This field cannot be empty.");
        }
    }

    public String readName(String prompt) {
        while (true) {
            String value = readNonEmpty(prompt);
            if (NAME.matcher(value).matches()) return value;
            System.out.println("  ! Name must be 2-50 letters (spaces, . ' - allowed).");
        }
    }

    public String readStudentId(String prompt) {
        while (true) {
            String value = readNonEmpty(prompt);
            if (STUDENT_ID.matcher(value).matches()) return value.toUpperCase();
            System.out.println("  ! ID must be 2-15 letters, digits or '-' (e.g. S101).");
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String value = readLine(prompt);
            try {
                int n = Integer.parseInt(value);
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException ignored) {
                // fall through to the error message
            }
            System.out.printf("  ! Enter a whole number between %d and %d.%n", min, max);
        }
    }

    public double readDouble(String prompt, double min, double max) {
        while (true) {
            String value = readLine(prompt);
            try {
                double d = Double.parseDouble(value);
                if (d >= min && d <= max) return d;
            } catch (NumberFormatException ignored) {
                // fall through to the error message
            }
            System.out.printf("  ! Enter a number between %.0f and %.0f.%n", min, max);
        }
    }

    public String readEmail(String prompt) {
        while (true) {
            String value = readNonEmpty(prompt);
            if (EMAIL.matcher(value).matches()) return value.toLowerCase();
            System.out.println("  ! Enter a valid email (e.g. name@example.com).");
        }
    }

    public String readPhone(String prompt) {
        while (true) {
            String value = readNonEmpty(prompt);
            if (PHONE.matcher(value).matches()) return value;
            System.out.println("  ! Enter a valid 10-digit mobile number starting with 6-9.");
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String value = readLine(prompt + " (y/n): ").toLowerCase();
            if (value.equals("y") || value.equals("yes")) return true;
            if (value.equals("n") || value.equals("no")) return false;
            System.out.println("  ! Please type y or n.");
        }
    }

    // ---------- "keep current value" variants used by the update screen ----------

    public String readNameOrKeep(String label, String current) {
        while (true) {
            String value = readLine(String.format("%s [%s]: ", label, current));
            if (value.isEmpty()) return current;
            if (NAME.matcher(value).matches()) return value;
            System.out.println("  ! Name must be 2-50 letters (spaces, . ' - allowed).");
        }
    }

    public String readTextOrKeep(String label, String current) {
        String value = readLine(String.format("%s [%s]: ", label, current));
        return value.isEmpty() ? current : value;
    }

    public int readIntOrKeep(String label, int current, int min, int max) {
        while (true) {
            String value = readLine(String.format("%s [%d]: ", label, current));
            if (value.isEmpty()) return current;
            try {
                int n = Integer.parseInt(value);
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException ignored) {
                // fall through to the error message
            }
            System.out.printf("  ! Enter a whole number between %d and %d.%n", min, max);
        }
    }

    public double readDoubleOrKeep(String label, double current, double min, double max) {
        while (true) {
            String value = readLine(String.format("%s [%.2f]: ", label, current));
            if (value.isEmpty()) return current;
            try {
                double d = Double.parseDouble(value);
                if (d >= min && d <= max) return d;
            } catch (NumberFormatException ignored) {
                // fall through to the error message
            }
            System.out.printf("  ! Enter a number between %.0f and %.0f.%n", min, max);
        }
    }

    public String readEmailOrKeep(String label, String current) {
        while (true) {
            String value = readLine(String.format("%s [%s]: ", label, current));
            if (value.isEmpty()) return current;
            if (EMAIL.matcher(value).matches()) return value.toLowerCase();
            System.out.println("  ! Enter a valid email (e.g. name@example.com).");
        }
    }

    public String readPhoneOrKeep(String label, String current) {
        while (true) {
            String value = readLine(String.format("%s [%s]: ", label, current));
            if (value.isEmpty()) return current;
            if (PHONE.matcher(value).matches()) return value;
            System.out.println("  ! Enter a valid 10-digit mobile number starting with 6-9.");
        }
    }
}
