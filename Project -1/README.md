# Student Management System (CLI-Based) — Project 1

A console-based Java application to manage student records — **add, view, search, update, delete, sort and report** — built with core Java, OOP, the Collections Framework and file handling. Records are saved to a CSV file automatically after every change, so data survives restarts.

It comes with **two interfaces** over the same Java backend:

- **Web UI** (HTML + CSS + JavaScript) — dashboard with summary tiles, grade chart, course breakdown, searchable/sortable table and add/edit/delete dialogs.
- **Console UI** — the original menu-driven CLI.

**Tags:** Java · OOPs · Collection Framework · File Handling

---

## Features

| # | Menu option | What it does |
|---|-------------|--------------|
| 1 | Add Student | Validated input for ID, name, age, email, phone, course, marks; rejects duplicate IDs |
| 2 | View All Students | Formatted table with auto-calculated grade |
| 3 | Search Student | By ID (exact, case-insensitive), by name (partial match) or by course |
| 4 | Update Student | Shows current values; press Enter to keep a field unchanged |
| 5 | Delete Student | Shows the record and asks for confirmation |
| 6 | Sort Students | By ID, name, marks or age, ascending or descending |
| 7 | Reports & Statistics | Average / highest / lowest marks, pass %, topper, students per course, grade distribution |
| 8 | Export / Backup | Writes a timestamped CSV copy to `backup/` (or any path you choose) |
| 0 | Save & Exit | Data is already saved after each change |

Grades: A+ (≥90), A (≥80), B (≥70), C (≥60), D (≥50), E (≥40), F (<40). Pass mark is 40.

---

## Requirements

- JDK 8 or later (`java -version` / `javac -version`)

## How to run — Web UI

**Windows:** double-click `run-web.bat` (it compiles, starts the server and opens the browser), or:

```bat
run-web.bat
```

**Git Bash / Linux / macOS:**

```bash
sh run-web.sh
```

Then open **http://localhost:8080**. Stop the server with `Ctrl+C`.

Optional arguments: `run-web.bat [port] [data-file]`, e.g. `run-web.bat 9090`.

**Web UI features**

- Summary tiles: total students, average marks (with range), pass rate, topper
- Grade distribution chart (hover a bar for the exact count) and students-per-course list (click a course to filter)
- Live search by name or ID, course filter, sort by ID / name / marks / age (also by clicking column headers)
- Add / Edit dialog with inline validation (checked in the browser **and** again on the server)
- Delete with a confirmation dialog
- Export all records as a CSV download
- Responsive layout (works on mobile); keyboard shortcuts: `/` focuses search, `n` opens "Add student"

### REST API (used by the web UI)

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/students?q=&course=&sort=id\|name\|marks\|age&order=asc\|desc` | List / search / filter / sort |
| GET | `/api/students/{id}` | One student |
| POST | `/api/students` | Add (form-encoded body) — `400` on invalid fields, `409` on duplicate ID |
| PUT | `/api/students/{id}` | Update (form-encoded body) |
| DELETE | `/api/students/{id}` | Delete |
| GET | `/api/stats` | Totals, average, pass rate, topper, per-course and per-grade counts |
| GET | `/api/export` | Download all records as CSV |

## How to run — Console UI

**Windows (double-click or Command Prompt):**

```bat
run.bat
```

**Git Bash / Linux / macOS:**

```bash
sh run.sh
```

**Manually:**

```bash
javac -encoding UTF-8 -d out -sourcepath src src/com/sms/Main.java
java -cp out com.sms.Main
```

By default data is stored in `data/students.csv` (5 sample students included). To use a different file, pass its path:

```bash
java -cp out com.sms.Main path/to/my_students.csv
```

---

## Project structure

```
Project -1/
├── data/students.csv                      # data file (CSV, auto-saved)
├── run-web.bat / run-web.sh               # build + start the web UI
├── run.bat / run.sh                       # build + start the console UI
├── web/                                   # front end
│   ├── index.html                         # page structure, dialogs
│   ├── styles.css                         # layout, components, responsive rules
│   └── app.js                             # fetch API calls, rendering, validation
└── src/com/sms/
    ├── Main.java                          # CLI menu and screens
    ├── web/
    │   ├── WebServer.java                 # HTTP server: REST API + static files
    │   └── Json.java                      # minimal JSON writer
    ├── model/
    │   ├── Person.java                    # abstract base class
    │   └── Student.java                   # extends Person, implements Comparable
    ├── service/
    │   └── StudentService.java            # business logic (CRUD, search, sort, reports)
    ├── repository/
    │   ├── StudentRepository.java         # storage interface
    │   └── FileStudentRepository.java     # CSV file implementation
    ├── exception/
    │   ├── StudentNotFoundException.java
    │   └── DuplicateStudentException.java
    └── util/
        ├── Validator.java                 # validation rules shared by CLI and web
        ├── InputHelper.java               # validated Scanner input
        └── TablePrinter.java              # console table output
```

## Concepts used

**OOP**
- *Encapsulation* — private fields with getters/setters in `Person` and `Student`.
- *Inheritance* — `Student extends Person`.
- *Abstraction* — abstract class `Person`, interface `StudentRepository`.
- *Polymorphism* — `getRole()` override; the service works with any `StudentRepository` implementation.
- Overridden `equals`, `hashCode`, `toString`, `compareTo`.

**Collections Framework**
- `LinkedHashMap<String, Student>` — O(1) lookup by ID while keeping insertion order.
- `ArrayList`, `Collections.unmodifiableList`, `List.sort` with `Comparator` (`comparing`, `reversed`).
- `TreeMap` (course counts sorted by name), `Optional`, `DoubleSummaryStatistics`.
- Java 8 Streams: `filter`, `map`, `groupingBy`, `counting`.

**File handling**
- `BufferedReader` / `BufferedWriter` via `java.nio.file.Files`, with try-with-resources.
- Safe writes: data goes to a `.tmp` file first, which is then moved over the real file.
- CSV quoting so values containing commas or quotes are stored correctly.
- Bad lines in the CSV are skipped with a warning instead of crashing the app.

**Web (HTML / CSS / JS + Java HTTP server)**
- `com.sun.net.httpserver.HttpServer` (built into the JDK — no external libraries).
- REST endpoints returning JSON with proper status codes (200 / 201 / 400 / 404 / 409).
- Static file serving with a path-traversal guard (requests can't escape `web/`).
- Front end in plain JavaScript: `fetch`, `async/await`, DOM rendering with HTML escaping, `<dialog>` modals, debounced search.
- CSS Grid / Flexbox layout with responsive breakpoints.

**Exception handling**
- Custom checked exceptions for "not found" and "duplicate ID".
- Input validation loops (numbers, ranges, email and phone formats).
- Clean exit when input is closed (Ctrl+Z / Ctrl+D).

---

## Sample session

```
-------------------- MAIN MENU ----------------------
  1. Add Student
  ...
Enter your choice: 7

== Reports & Statistics ==
  Total students : 5
  Average marks  : 72.25
  Highest marks  : 92.00
  Lowest marks   : 38.50
  Passed / Failed: 4 / 1  (pass % = 80.0)
  Topper         : Priya Patel (S102) - 92.00
```
