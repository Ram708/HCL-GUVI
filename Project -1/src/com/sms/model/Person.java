package com.sms.model;

/**
 * Base class holding the details common to any person in the system.
 * Demonstrates abstraction and encapsulation (private fields + getters/setters).
 */
public abstract class Person {

    private String name;
    private int age;
    private String email;
    private String phone;

    protected Person(String name, int age, String email, String phone) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
    }

    /** Each subclass describes its own role (polymorphism). */
    public abstract String getRole();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
