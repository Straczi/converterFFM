package de.strocz;

import java.util.List;

public class Customer {
    private String name;
    private String email;
    private String address;
    private List<String> orders;
    private int age;
    private String phoneNumber;

    public Customer(String name, String email, String address, String phoneNumber, int age, List<String> orders) {
        this.name = name;
        this.email = email;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.age = age;
        this.orders = orders;
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
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    
    public List<String> getOrders() {
        return orders;
    }
    public void setOrders(List<String> orders) {
        this.orders = orders;
    }
    
}
