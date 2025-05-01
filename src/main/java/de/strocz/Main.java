package de.strocz;

import java.lang.foreign.Arena;
import java.util.List;


public class Main {
    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {

        //example customer
        Customer customer = new Customer("John Doe", "a@b.de", "Main Street 1", "1234567890", 30, List.of("aaksjdlaksjdlkajsdlkajsl", "b", "calhsdlkjahsdkj", "huh?"), List.of(42.0f, 6.9f, 2.1f, 3.14f));
        
        //example point
        Point point = new Point(10, 20);

        try(Arena arena = Arena.ofConfined()) {
            MemoryStruct<Customer> memoryConvert = new MemoryStruct<>(arena, customer);

            System.out.println("Name: " + memoryConvert.getFieldValue("name"));
            System.out.println("Email: " + memoryConvert.getFieldValue("email"));
            System.out.println("Address: " + memoryConvert.getFieldValue("address"));
            System.out.println("Phone: " + memoryConvert.getFieldValue("phoneNumber"));
            System.out.println("Age: " + memoryConvert.getFieldValue("age")); 
            System.out.println("orders: " + memoryConvert.getFieldValue("orders"));
            System.out.println("numbers: " + memoryConvert.getFieldValue("numbers"));  
            
            Customer backToCustomer = memoryConvert.convertBackToEntity();

            System.out.println("Name: " + backToCustomer.getName());
            System.out.println("Email: " + backToCustomer.getEmail());
            System.out.println("Address: " + backToCustomer.getAddress());
            System.out.println("Phone: " + backToCustomer.getPhoneNumber());
            System.out.println("Age: " + backToCustomer.getAge());
            System.out.println("Orders: " + backToCustomer.getOrders());
            System.out.println("Numbers: " + backToCustomer.getNumbers());
            
            
            //Point

            MemoryStruct<Point> memoryConvertPoint = new MemoryStruct<>(arena, point);
            System.out.println("X: " + memoryConvertPoint.getFieldValue("x"));
            System.out.println("Y: " + memoryConvertPoint.getFieldValue("y"));

            Point backToPoint = memoryConvertPoint.convertBackToEntity();
            System.out.println("X: " + backToPoint.getX());
            System.out.println("Y: " + backToPoint.getY());
        }
    }
}