package com.sms.web;

import com.sms.model.Student;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/** Minimal JSON writer (the project uses no external libraries). */
final class Json {

    private Json() {
    }

    static String string(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c == '<' || c == '>') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    static String number(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    static String student(Student s) {
        return "{\"studentId\":" + string(s.getStudentId())
                + ",\"name\":" + string(s.getName())
                + ",\"age\":" + s.getAge()
                + ",\"email\":" + string(s.getEmail())
                + ",\"phone\":" + string(s.getPhone())
                + ",\"course\":" + string(s.getCourse())
                + ",\"marks\":" + number(s.getMarks())
                + ",\"grade\":" + string(s.getGrade())
                + ",\"passed\":" + s.isPassed() + "}";
    }

    static String students(Collection<Student> list) {
        StringBuilder sb = new StringBuilder("[");
        Iterator<Student> it = list.iterator();
        while (it.hasNext()) {
            sb.append(student(it.next()));
            if (it.hasNext()) sb.append(',');
        }
        return sb.append(']').toString();
    }

    static String counts(Map<String, Long> map) {
        StringBuilder sb = new StringBuilder("[");
        Iterator<Map.Entry<String, Long>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            sb.append("{\"label\":").append(string(e.getKey()))
              .append(",\"count\":").append(e.getValue()).append('}');
            if (it.hasNext()) sb.append(',');
        }
        return sb.append(']').toString();
    }

    static String error(String message) {
        return "{\"error\":" + string(message) + "}";
    }

    static String errors(Map<String, String> fieldErrors) {
        StringBuilder sb = new StringBuilder("{\"error\":\"Please fix the highlighted fields.\",\"fields\":{");
        Iterator<Map.Entry<String, String>> it = fieldErrors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            sb.append(string(e.getKey())).append(':').append(string(e.getValue()));
            if (it.hasNext()) sb.append(',');
        }
        return sb.append("}}").toString();
    }
}
