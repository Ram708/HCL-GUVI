package com.sms.model;

import java.util.Objects;

/**
 * A student record. Inherits personal details from {@link Person} and adds
 * academic details. Natural ordering is by student ID.
 */
public class Student extends Person implements Comparable<Student> {

    private final String studentId;
    private String course;
    private double marks;

    public Student(String studentId, String name, int age, String email,
                   String phone, String course, double marks) {
        super(name, age, email, phone);
        this.studentId = studentId;
        this.course = course;
        this.marks = marks;
    }

    @Override
    public String getRole() {
        return "Student";
    }

    /** Grade derived from marks (out of 100). */
    public String getGrade() {
        if (marks >= 90) return "A+";
        if (marks >= 80) return "A";
        if (marks >= 70) return "B";
        if (marks >= 60) return "C";
        if (marks >= 50) return "D";
        if (marks >= 40) return "E";
        return "F";
    }

    public boolean isPassed() {
        return marks >= 40;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public double getMarks() {
        return marks;
    }

    public void setMarks(double marks) {
        this.marks = marks;
    }

    @Override
    public int compareTo(Student other) {
        return this.studentId.compareToIgnoreCase(other.studentId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return studentId.equalsIgnoreCase(((Student) o).studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId.toUpperCase());
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %d | %s | %s | %s | %.2f | %s",
                studentId, getName(), getAge(), getEmail(), getPhone(),
                course, marks, getGrade());
    }
}
