package com.sms.service;

import com.sms.exception.DuplicateStudentException;
import com.sms.exception.StudentNotFoundException;
import com.sms.model.Student;
import com.sms.repository.StudentRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic for managing students. Records are kept in memory in a
 * {@link LinkedHashMap} (keyed by upper-cased ID for O(1) lookup while keeping
 * insertion order) and persisted through a {@link StudentRepository} after
 * every change.
 */
public class StudentService {

    public enum SortField { ID, NAME, MARKS, AGE }

    private final Map<String, Student> students = new LinkedHashMap<>();
    private final StudentRepository repository;

    public StudentService(StudentRepository repository) throws IOException {
        this.repository = repository;
        for (Student s : repository.loadAll()) {
            students.put(key(s.getStudentId()), s);
        }
    }

    private static String key(String studentId) {
        return studentId.trim().toUpperCase();
    }

    private void persist() throws IOException {
        repository.saveAll(students.values());
    }

    // ---------- CRUD ----------

    public void addStudent(Student student) throws DuplicateStudentException, IOException {
        String k = key(student.getStudentId());
        if (students.containsKey(k)) {
            throw new DuplicateStudentException(student.getStudentId());
        }
        students.put(k, student);
        persist();
    }

    public Student getStudent(String studentId) throws StudentNotFoundException {
        Student s = students.get(key(studentId));
        if (s == null) {
            throw new StudentNotFoundException(studentId);
        }
        return s;
    }

    public boolean exists(String studentId) {
        return students.containsKey(key(studentId));
    }

    /** Persists changes made to a student obtained via {@link #getStudent}. */
    public void updateStudent(Student student) throws StudentNotFoundException, IOException {
        String k = key(student.getStudentId());
        if (!students.containsKey(k)) {
            throw new StudentNotFoundException(student.getStudentId());
        }
        students.put(k, student);
        persist();
    }

    public Student deleteStudent(String studentId) throws StudentNotFoundException, IOException {
        Student removed = students.remove(key(studentId));
        if (removed == null) {
            throw new StudentNotFoundException(studentId);
        }
        persist();
        return removed;
    }

    public List<Student> getAllStudents() {
        return Collections.unmodifiableList(new ArrayList<>(students.values()));
    }

    public int count() {
        return students.size();
    }

    // ---------- Search & sort ----------

    public List<Student> searchByName(String text) {
        String needle = text.trim().toLowerCase();
        return students.values().stream()
                .filter(s -> s.getName().toLowerCase().contains(needle))
                .collect(Collectors.toList());
    }

    public List<Student> filterByCourse(String course) {
        return students.values().stream()
                .filter(s -> s.getCourse().equalsIgnoreCase(course.trim()))
                .collect(Collectors.toList());
    }

    public List<Student> getSorted(SortField field, boolean descending) {
        Comparator<Student> comparator;
        switch (field) {
            case NAME:
                comparator = Comparator.comparing(Student::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case MARKS:
                comparator = Comparator.comparingDouble(Student::getMarks);
                break;
            case AGE:
                comparator = Comparator.comparingInt(Student::getAge);
                break;
            case ID:
            default:
                comparator = Comparator.naturalOrder();
        }
        if (descending) {
            comparator = comparator.reversed();
        }
        List<Student> list = new ArrayList<>(students.values());
        list.sort(comparator);
        return list;
    }

    // ---------- Reports ----------

    public DoubleSummaryStatistics marksStatistics() {
        return students.values().stream()
                .mapToDouble(Student::getMarks)
                .summaryStatistics();
    }

    public Optional<Student> topper() {
        return students.values().stream().max(Comparator.comparingDouble(Student::getMarks));
    }

    public long passedCount() {
        return students.values().stream().filter(Student::isPassed).count();
    }

    /** Number of students per course, sorted alphabetically by course. */
    public Map<String, Long> countByCourse() {
        return students.values().stream()
                .collect(Collectors.groupingBy(Student::getCourse, TreeMap::new, Collectors.counting()));
    }

    /** Number of students per grade, in grade order (A+ first). */
    public Map<String, Long> countByGrade() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String g : new String[]{"A+", "A", "B", "C", "D", "E", "F"}) {
            result.put(g, 0L);
        }
        for (Student s : students.values()) {
            result.merge(s.getGrade(), 1L, Long::sum);
        }
        return result;
    }
}
