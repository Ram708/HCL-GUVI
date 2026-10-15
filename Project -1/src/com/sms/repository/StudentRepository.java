package com.sms.repository;

import com.sms.model.Student;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/** Storage abstraction so the service does not depend on a specific format. */
public interface StudentRepository {

    List<Student> loadAll() throws IOException;

    void saveAll(Collection<Student> students) throws IOException;
}
