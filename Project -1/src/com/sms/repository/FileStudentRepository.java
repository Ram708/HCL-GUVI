package com.sms.repository;

import com.sms.model.Student;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Stores students in a CSV file. Writes go to a temporary file first and are
 * then moved over the real file, so a crash mid-write never corrupts the data.
 */
public class FileStudentRepository implements StudentRepository {

    private static final String HEADER = "studentId,name,age,email,phone,course,marks";

    private final Path file;

    public FileStudentRepository(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    @Override
    public List<Student> loadAll() throws IOException {
        List<Student> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.startsWith("studentId")) continue;
                if (line.trim().isEmpty()) continue;
                try {
                    result.add(parse(line));
                } catch (RuntimeException e) {
                    System.err.println("Skipping bad record on line " + lineNo + ": " + e.getMessage());
                }
            }
        }
        return result;
    }

    @Override
    public void saveAll(Collection<Student> students) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            for (Student s : students) {
                writer.write(format(s));
                writer.newLine();
            }
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    // ---------- CSV helpers ----------

    private static String format(Student s) {
        return String.join(",",
                escape(s.getStudentId()),
                escape(s.getName()),
                String.valueOf(s.getAge()),
                escape(s.getEmail()),
                escape(s.getPhone()),
                escape(s.getCourse()),
                String.valueOf(s.getMarks()));
    }

    private static Student parse(String line) {
        List<String> f = splitCsv(line);
        if (f.size() != 7) {
            throw new IllegalArgumentException("expected 7 fields but found " + f.size());
        }
        return new Student(f.get(0), f.get(1), Integer.parseInt(f.get(2).trim()),
                f.get(3), f.get(4), f.get(5), Double.parseDouble(f.get(6).trim()));
    }

    /** Quotes a value if it contains a comma, quote or newline. */
    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields;
    }
}
