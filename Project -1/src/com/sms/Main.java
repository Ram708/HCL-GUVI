package com.sms;

import com.sms.exception.DuplicateStudentException;
import com.sms.exception.StudentNotFoundException;
import com.sms.model.Student;
import com.sms.repository.FileStudentRepository;
import com.sms.service.StudentService;
import com.sms.service.StudentService.SortField;
import com.sms.util.InputHelper;
import com.sms.util.InputHelper.InputClosedException;
import com.sms.util.TablePrinter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Entry point of the Student Management System (console application).
 *
 * Usage: java -cp out com.sms.Main [path/to/students.csv]
 */
public class Main {

    private static final String DEFAULT_DATA_FILE = "data/students.csv";

    private final StudentService service;
    private final FileStudentRepository repository;
    private final InputHelper in;

    private Main(StudentService service, FileStudentRepository repository, InputHelper in) {
        this.service = service;
        this.repository = repository;
        this.in = in;
    }

    public static void main(String[] args) {
        Path dataFile = Paths.get(args.length > 0 ? args[0] : DEFAULT_DATA_FILE);
        FileStudentRepository repository = new FileStudentRepository(dataFile);

        StudentService service;
        try {
            service = new StudentService(repository);
        } catch (IOException e) {
            System.err.println("Could not read data file " + dataFile.toAbsolutePath() + ": " + e.getMessage());
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            new Main(service, repository, new InputHelper(scanner)).run();
        } catch (InputClosedException e) {
            System.out.println();
            System.out.println("Input closed. All data is already saved. Goodbye!");
        }
    }

    private void run() {
        printBanner();
        boolean running = true;
        while (running) {
            printMenu();
            int choice = in.readInt("Enter your choice: ", 0, 8);
            System.out.println();
            try {
                switch (choice) {
                    case 1: addStudent(); break;
                    case 2: viewAll(); break;
                    case 3: searchMenu(); break;
                    case 4: updateStudent(); break;
                    case 5: deleteStudent(); break;
                    case 6: sortMenu(); break;
                    case 7: showReports(); break;
                    case 8: exportBackup(); break;
                    case 0: running = false; break;
                    default: break;
                }
            } catch (IOException e) {
                System.out.println("  ! Could not save to file: " + e.getMessage());
            }
        }
        System.out.println("All data saved to " + repository.getFile().toAbsolutePath());
        System.out.println("Thank you for using the Student Management System. Goodbye!");
    }

    private void printBanner() {
        System.out.println("=====================================================");
        System.out.println("        STUDENT MANAGEMENT SYSTEM  (Console)         ");
        System.out.println("=====================================================");
        System.out.println("Data file : " + repository.getFile().toAbsolutePath());
        System.out.println("Loaded    : " + service.count() + " student record(s)");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("-------------------- MAIN MENU ----------------------");
        System.out.println("  1. Add Student");
        System.out.println("  2. View All Students");
        System.out.println("  3. Search Student");
        System.out.println("  4. Update Student");
        System.out.println("  5. Delete Student");
        System.out.println("  6. Sort Students");
        System.out.println("  7. Reports & Statistics");
        System.out.println("  8. Export / Backup Records");
        System.out.println("  0. Save & Exit");
        System.out.println("-----------------------------------------------------");
    }

    // ---------- 1. Add ----------

    private void addStudent() throws IOException {
        System.out.println("== Add New Student ==");
        String id = in.readStudentId("Student ID (e.g. S101): ");
        if (service.exists(id)) {
            System.out.println("  ! A student with ID " + id + " already exists.");
            return;
        }
        String name = in.readName("Full name      : ");
        int age = in.readInt("Age (5-100)    : ", 5, 100);
        String email = in.readEmail("Email          : ");
        String phone = in.readPhone("Phone (10 dig) : ");
        String course = in.readNonEmpty("Course         : ");
        double marks = in.readDouble("Marks (0-100)  : ", 0, 100);

        try {
            service.addStudent(new Student(id, name, age, email, phone, course, marks));
            System.out.println("  Student " + id + " added successfully.");
        } catch (DuplicateStudentException e) {
            System.out.println("  ! " + e.getMessage());
        }
    }

    // ---------- 2. View ----------

    private void viewAll() {
        System.out.println("== All Students ==");
        TablePrinter.print(service.getAllStudents());
    }

    // ---------- 3. Search ----------

    private void searchMenu() {
        System.out.println("== Search Student ==");
        System.out.println("  1. By Student ID");
        System.out.println("  2. By Name (partial match)");
        System.out.println("  3. By Course");
        int choice = in.readInt("Choose search type: ", 1, 3);
        switch (choice) {
            case 1:
                String id = in.readNonEmpty("Enter Student ID: ");
                try {
                    TablePrinter.printDetails(service.getStudent(id));
                } catch (StudentNotFoundException e) {
                    System.out.println("  ! " + e.getMessage());
                }
                break;
            case 2:
                TablePrinter.print(service.searchByName(in.readNonEmpty("Enter name to search: ")));
                break;
            case 3:
                TablePrinter.print(service.filterByCourse(in.readNonEmpty("Enter course: ")));
                break;
            default:
                break;
        }
    }

    // ---------- 4. Update ----------

    private void updateStudent() throws IOException {
        System.out.println("== Update Student ==");
        String id = in.readNonEmpty("Enter Student ID to update: ");
        try {
            Student s = service.getStudent(id);
            TablePrinter.printDetails(s);
            System.out.println("Enter new values (press Enter to keep the current value):");
            s.setName(in.readNameOrKeep("Name  ", s.getName()));
            s.setAge(in.readIntOrKeep("Age   ", s.getAge(), 5, 100));
            s.setEmail(in.readEmailOrKeep("Email ", s.getEmail()));
            s.setPhone(in.readPhoneOrKeep("Phone ", s.getPhone()));
            s.setCourse(in.readTextOrKeep("Course", s.getCourse()));
            s.setMarks(in.readDoubleOrKeep("Marks ", s.getMarks(), 0, 100));
            service.updateStudent(s);
            System.out.println("  Student " + s.getStudentId() + " updated successfully.");
            TablePrinter.printDetails(s);
        } catch (StudentNotFoundException e) {
            System.out.println("  ! " + e.getMessage());
        }
    }

    // ---------- 5. Delete ----------

    private void deleteStudent() throws IOException {
        System.out.println("== Delete Student ==");
        String id = in.readNonEmpty("Enter Student ID to delete: ");
        try {
            Student s = service.getStudent(id);
            TablePrinter.printDetails(s);
            if (in.readYesNo("Are you sure you want to delete " + s.getName() + "?")) {
                service.deleteStudent(id);
                System.out.println("  Student " + s.getStudentId() + " deleted.");
            } else {
                System.out.println("  Delete cancelled.");
            }
        } catch (StudentNotFoundException e) {
            System.out.println("  ! " + e.getMessage());
        }
    }

    // ---------- 6. Sort ----------

    private void sortMenu() {
        System.out.println("== Sort Students ==");
        System.out.println("  1. By Student ID");
        System.out.println("  2. By Name");
        System.out.println("  3. By Marks");
        System.out.println("  4. By Age");
        int choice = in.readInt("Sort by: ", 1, 4);
        boolean descending = in.readYesNo("Descending order?");
        SortField field = SortField.values()[choice - 1];
        TablePrinter.print(service.getSorted(field, descending));
    }

    // ---------- 7. Reports ----------

    private void showReports() {
        System.out.println("== Reports & Statistics ==");
        if (service.count() == 0) {
            System.out.println("  No students yet.");
            return;
        }
        DoubleSummaryStatistics stats = service.marksStatistics();
        long passed = service.passedCount();
        System.out.println("  Total students : " + stats.getCount());
        System.out.printf("  Average marks  : %.2f%n", stats.getAverage());
        System.out.printf("  Highest marks  : %.2f%n", stats.getMax());
        System.out.printf("  Lowest marks   : %.2f%n", stats.getMin());
        System.out.printf("  Passed / Failed: %d / %d  (pass %% = %.1f)%n",
                passed, stats.getCount() - passed, 100.0 * passed / stats.getCount());
        service.topper().ifPresent(t ->
                System.out.printf("  Topper         : %s (%s) - %.2f%n", t.getName(), t.getStudentId(), t.getMarks()));

        System.out.println();
        System.out.println("  Students per course:");
        for (Map.Entry<String, Long> e : service.countByCourse().entrySet()) {
            System.out.printf("    %-15s %d%n", e.getKey(), e.getValue());
        }
        System.out.println();
        System.out.println("  Grade distribution:");
        for (Map.Entry<String, Long> e : service.countByGrade().entrySet()) {
            System.out.printf("    %-3s %3d  %s%n", e.getKey(), e.getValue(), bar(e.getValue()));
        }
    }

    private static String bar(long n) {
        StringBuilder sb = new StringBuilder();
        for (long i = 0; i < n; i++) sb.append('#');
        return sb.toString();
    }

    // ---------- 8. Export ----------

    private void exportBackup() throws IOException {
        System.out.println("== Export / Backup ==");
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String defaultName = "backup/students_" + stamp + ".csv";
        String name = in.readLine("Export file path [" + defaultName + "]: ");
        Path target = Paths.get(name.isEmpty() ? defaultName : name);
        List<Student> all = service.getAllStudents();
        new FileStudentRepository(target).saveAll(all);
        System.out.println("  Exported " + all.size() + " record(s) to " + target.toAbsolutePath());
    }
}
