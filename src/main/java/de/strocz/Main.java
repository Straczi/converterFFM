package de.strocz;

import java.lang.foreign.Arena;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException {
        Customer customer = new Customer("John Doe", "a@b.de", "Main Street 1", "1234567890", 30);
        Point point = new Point(10, 20);
        try(Arena arena = Arena.ofConfined()) {
            MemoryStruct<Customer> memoryConvert = new MemoryStruct<>(arena, customer);
            System.out.println("Name: " + memoryConvert.getField("name"));
            System.out.println("Email: " + memoryConvert.getField("email"));
            System.out.println("Address: " + memoryConvert.getField("address"));
            System.out.println("Phone: " + memoryConvert.getField("phoneNumber"));
            System.out.println("Age: " + memoryConvert.getField("age")); 

            Customer backToCustomer = memoryConvert.convertBackToEntity();
            System.out.println("Name: " + backToCustomer.getName());
            System.out.println("Email: " + backToCustomer.getEmail());
            System.out.println("Address: " + backToCustomer.getAddress());
            System.out.println("Phone: " + backToCustomer.getPhoneNumber());
            System.out.println("Age: " + backToCustomer.getAge());
            
            
            //Point

            MemoryStruct<Point> memoryConvertPoint = new MemoryStruct<>(arena, point);
            System.out.println("X: " + memoryConvertPoint.getField("x"));
            System.out.println("Y: " + memoryConvertPoint.getField("y"));

            Point backToPoint = memoryConvertPoint.convertBackToEntity();
            System.out.println("X: " + backToPoint.getX());
            System.out.println("Y: " + backToPoint.getY());
        }
    }
}