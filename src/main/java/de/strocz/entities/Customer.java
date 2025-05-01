package de.strocz.entities;

import java.util.List;

public class Customer {
    private String name;
    private String email;
    private List<String> coWorkers;
    private double height;
    private int age;

    public Customer(String name, String email, List<String> coWorkers, int age, double height) {
        this.name = name;
        this.email = email;
        this.coWorkers = coWorkers;
        this.age = age;
        this.height = height;
    }
    public Customer() {
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public List<String> getCoWorkers() {
        return coWorkers;
    }
    public void setCoWorkers(List<String> coWorkers) {
        this.coWorkers = coWorkers;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public double getHeight() {
        return height;
    }
    public void setHeight(double height) {
        this.height = height;
    }

    
}
