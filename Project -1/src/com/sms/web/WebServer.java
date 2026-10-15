package com.sms.web;

import com.sms.exception.DuplicateStudentException;
import com.sms.exception.StudentNotFoundException;
import com.sms.model.Student;
import com.sms.repository.FileStudentRepository;
import com.sms.service.StudentService;
import com.sms.service.StudentService.SortField;
import com.sms.util.Validator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Web front end for the Student Management System. Serves the HTML/CSS/JS
 * files in {@code web/} and a JSON REST API backed by {@link StudentService}.
 *
 * <pre>
 * GET    /api/students?q=&amp;course=&amp;sort=id|name|marks|age&amp;order=asc|desc
 * GET    /api/students/{id}
 * POST   /api/students          (form-encoded body)
 * PUT    /api/students/{id}     (form-encoded body)
 * DELETE /api/students/{id}
 * GET    /api/stats
 * GET    /api/export            (CSV download)
 * </pre>
 *
 * Usage: java [-Dsms.home=projectDir] -cp out com.sms.web.WebServer [port] [path/to/students.csv]
 */
public class WebServer {

    private static final Map<String, String> MIME = new HashMap<>();

    static {
        MIME.put("html", "text/html; charset=utf-8");
        MIME.put("css", "text/css; charset=utf-8");
        MIME.put("js", "application/javascript; charset=utf-8");
        MIME.put("svg", "image/svg+xml");
        MIME.put("png", "image/png");
        MIME.put("ico", "image/x-icon");
    }

    private final StudentService service;
    private final FileStudentRepository repository;
    private final Path webRoot;

    public WebServer(StudentService service, FileStudentRepository repository, Path webRoot) {
        this.service = service;
        this.repository = repository;
        this.webRoot = webRoot.toAbsolutePath().normalize();
    }

    public static void main(String[] args) throws IOException {
        // Project folder; defaults to the current directory. Set -Dsms.home=... to run from elsewhere.
        Path home = Paths.get(System.getProperty("sms.home", "."));
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path dataFile = home.resolve(args.length > 1 ? args[1] : "data/students.csv");

        FileStudentRepository repository = new FileStudentRepository(dataFile);
        StudentService service = new StudentService(repository);
        WebServer app = new WebServer(service, repository, home.resolve("web"));

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/api/", app::handleApi);
        server.createContext("/", app::handleStatic);
        // A null executor handles requests one at a time on a single thread, so
        // the (non-thread-safe) service never sees concurrent modifications.
        server.setExecutor(null);
        server.start();

        System.out.println("Student Management System web UI running at http://localhost:" + port);
        System.out.println("Data file: " + dataFile.toAbsolutePath() + " (" + service.count() + " records)");
        System.out.println("Press Ctrl+C to stop.");
    }

    // ---------------------------------------------------------------- API

    private void handleApi(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod().toUpperCase(Locale.ROOT);

            if (path.equals("/api/students") || path.equals("/api/students/")) {
                if (method.equals("GET")) {
                    listStudents(ex);
                } else if (method.equals("POST")) {
                    createStudent(ex);
                } else {
                    sendJson(ex, 405, Json.error("Method not allowed"));
                }
            } else if (path.startsWith("/api/students/")) {
                String id = decode(path.substring("/api/students/".length()));
                if (method.equals("GET")) {
                    sendJson(ex, 200, Json.student(service.getStudent(id)));
                } else if (method.equals("PUT")) {
                    updateStudent(ex, id);
                } else if (method.equals("DELETE")) {
                    Student removed = service.deleteStudent(id);
                    sendJson(ex, 200, Json.student(removed));
                } else {
                    sendJson(ex, 405, Json.error("Method not allowed"));
                }
            } else if (path.equals("/api/stats") && method.equals("GET")) {
                sendJson(ex, 200, stats());
            } else if (path.equals("/api/export") && method.equals("GET")) {
                export(ex);
            } else {
                sendJson(ex, 404, Json.error("Unknown endpoint: " + path));
            }
        } catch (StudentNotFoundException e) {
            sendJson(ex, 404, Json.error(e.getMessage()));
        } catch (DuplicateStudentException e) {
            sendJson(ex, 409, Json.errors(singleton("studentId", e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, Json.error("Server error: " + e.getMessage()));
        }
    }

    private void listStudents(HttpExchange ex) throws IOException {
        Map<String, String> q = parseForm(ex.getRequestURI().getRawQuery());
        SortField field = parseSort(q.get("sort"));
        boolean desc = "desc".equalsIgnoreCase(q.get("order"));
        String text = q.getOrDefault("q", "").trim().toLowerCase(Locale.ROOT);
        String course = q.getOrDefault("course", "").trim();

        List<Student> list = service.getSorted(field, desc).stream()
                .filter(s -> text.isEmpty()
                        || s.getName().toLowerCase(Locale.ROOT).contains(text)
                        || s.getStudentId().toLowerCase(Locale.ROOT).contains(text))
                .filter(s -> course.isEmpty() || s.getCourse().equalsIgnoreCase(course))
                .collect(Collectors.toList());
        sendJson(ex, 200, Json.students(list));
    }

    private void createStudent(HttpExchange ex) throws IOException, DuplicateStudentException {
        Map<String, String> f = parseForm(readBody(ex));
        Map<String, String> errors = validate(f, true);
        if (!errors.isEmpty()) {
            sendJson(ex, 400, Json.errors(errors));
            return;
        }
        Student s = new Student(
                f.get("studentId").trim().toUpperCase(Locale.ROOT),
                f.get("name").trim(),
                Integer.parseInt(f.get("age").trim()),
                f.get("email").trim().toLowerCase(Locale.ROOT),
                f.get("phone").trim(),
                f.get("course").trim(),
                Double.parseDouble(f.get("marks").trim()));
        service.addStudent(s);
        sendJson(ex, 201, Json.student(s));
    }

    private void updateStudent(HttpExchange ex, String id) throws IOException, StudentNotFoundException {
        Student s = service.getStudent(id);
        Map<String, String> f = parseForm(readBody(ex));
        Map<String, String> errors = validate(f, false);
        if (!errors.isEmpty()) {
            sendJson(ex, 400, Json.errors(errors));
            return;
        }
        s.setName(f.get("name").trim());
        s.setAge(Integer.parseInt(f.get("age").trim()));
        s.setEmail(f.get("email").trim().toLowerCase(Locale.ROOT));
        s.setPhone(f.get("phone").trim());
        s.setCourse(f.get("course").trim());
        s.setMarks(Double.parseDouble(f.get("marks").trim()));
        service.updateStudent(s);
        sendJson(ex, 200, Json.student(s));
    }

    private static Map<String, String> validate(Map<String, String> f, boolean includeId) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (includeId) put(errors, "studentId", Validator.checkStudentId(f.get("studentId")));
        put(errors, "name", Validator.checkName(f.get("name")));
        put(errors, "age", Validator.checkAge(f.get("age")));
        put(errors, "email", Validator.checkEmail(f.get("email")));
        put(errors, "phone", Validator.checkPhone(f.get("phone")));
        put(errors, "course", Validator.checkCourse(f.get("course")));
        put(errors, "marks", Validator.checkMarks(f.get("marks")));
        return errors;
    }

    private static void put(Map<String, String> errors, String field, String message) {
        if (message != null) errors.put(field, message);
    }

    private String stats() {
        DoubleSummaryStatistics st = service.marksStatistics();
        long total = st.getCount();
        long passed = service.passedCount();
        Optional<Student> topper = service.topper();
        return "{\"total\":" + total
                + ",\"average\":" + Json.number(total == 0 ? 0 : st.getAverage())
                + ",\"highest\":" + Json.number(total == 0 ? 0 : st.getMax())
                + ",\"lowest\":" + Json.number(total == 0 ? 0 : st.getMin())
                + ",\"passed\":" + passed
                + ",\"failed\":" + (total - passed)
                + ",\"passRate\":" + Json.number(total == 0 ? 0 : 100.0 * passed / total)
                + ",\"topper\":" + (topper.isPresent() ? Json.student(topper.get()) : "null")
                + ",\"byCourse\":" + Json.counts(service.countByCourse())
                + ",\"byGrade\":" + Json.counts(service.countByGrade())
                + "}";
    }

    private void export(HttpExchange ex) throws IOException {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path tmp = Files.createTempFile("students_export", ".csv");
        try {
            new FileStudentRepository(tmp).saveAll(service.getAllStudents());
            byte[] bytes = Files.readAllBytes(tmp);
            ex.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
            ex.getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"students_" + stamp + ".csv\"");
            send(ex, 200, bytes);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static SortField parseSort(String s) {
        if (s == null) return SortField.ID;
        try {
            return SortField.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SortField.ID;
        }
    }

    // ------------------------------------------------------------- static

    private void handleStatic(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) {
            send(ex, 405, "Method not allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String path = decode(ex.getRequestURI().getPath());
        if (path.endsWith("/")) path += "index.html";
        Path file = webRoot.resolve(path.substring(1)).normalize();

        // Refuse anything outside web/ (e.g. /../data/students.csv).
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            send(ex, 404, "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String name = file.getFileName().toString();
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        ex.getResponseHeaders().set("Content-Type", MIME.getOrDefault(ext, "application/octet-stream"));
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        send(ex, 200, Files.readAllBytes(file));
    }

    // ------------------------------------------------------------ helpers

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(ex, status, json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, byte[] body) throws IOException {
        ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** Parses {@code a=1&b=2} (query strings and form bodies). */
    private static Map<String, String> parseForm(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = decode(eq < 0 ? pair : pair.substring(0, eq));
            String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
            map.put(key, value);
        }
        return map;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return s;
        }
    }

    private static Map<String, String> singleton(String key, String value) {
        Map<String, String> m = new HashMap<>();
        m.put(key, value);
        return m;
    }
}
