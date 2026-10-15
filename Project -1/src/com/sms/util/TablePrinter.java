package com.sms.util;

import com.sms.model.Student;

import java.util.List;

/** Prints students as a fixed-width table on the console. */
public final class TablePrinter {

    private static final String ROW = "| %-8s | %-20s | %3s | %-26s | %-10s | %-12s | %6s | %-5s |%n";
    private static final String LINE =
            "+----------+----------------------+-----+----------------------------+------------+--------------+--------+-------+";

    private TablePrinter() {
    }

    public static void print(List<Student> students) {
        if (students.isEmpty()) {
            System.out.println("  (no students to show)");
            return;
        }
        System.out.println(LINE);
        System.out.printf(ROW, "ID", "Name", "Age", "Email", "Phone", "Course", "Marks", "Grade");
        System.out.println(LINE);
        for (Student s : students) {
            System.out.printf(ROW,
                    cut(s.getStudentId(), 8), cut(s.getName(), 20), s.getAge(),
                    cut(s.getEmail(), 26), cut(s.getPhone(), 10), cut(s.getCourse(), 12),
                    String.format("%.2f", s.getMarks()), s.getGrade());
        }
        System.out.println(LINE);
        System.out.println("  Total: " + students.size() + " student(s)");
    }

    public static void printDetails(Student s) {
        System.out.println("  ----------------------------------");
        System.out.println("  Student ID : " + s.getStudentId());
        System.out.println("  Name       : " + s.getName());
        System.out.println("  Age        : " + s.getAge());
        System.out.println("  Email      : " + s.getEmail());
        System.out.println("  Phone      : " + s.getPhone());
        System.out.println("  Course     : " + s.getCourse());
        System.out.printf("  Marks      : %.2f%n", s.getMarks());
        System.out.println("  Grade      : " + s.getGrade() + (s.isPassed() ? " (Pass)" : " (Fail)"));
        System.out.println("  ----------------------------------");
    }

    private static String cut(String value, int width) {
        return value.length() <= width ? value : value.substring(0, width - 1) + "~";
    }
}
