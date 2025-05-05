package de.strocz;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.List;

import de.strocz.entities.Customer;
import de.strocz.memoryconverter.MemoryStruct;

public class Main {
    public static void main(String[] args) throws Throwable {

        // example customer
        List<String> coWorkers = List.of("Zigbert", "Strocz", "Anna", "Berta", "Charlie");
        Customer customer = new Customer("John Doe", "john@stroz.com", coWorkers, 30, 1.75);

        try (Arena arena = Arena.ofShared()) {
            MemoryStruct<Customer> customerStruct = new MemoryStruct<>(arena, customer);

            Linker linker = Linker.nativeLinker();

            SymbolLookup loaderLookup = SymbolLookup
                    .libraryLookup("src/main/resources/customer-sort.so", arena);

            // Sort coworkers of customer in c function
            MethodHandle sortCoworkerHandle = linker.downcallHandle(
                    loaderLookup.find("sortCoWorkers").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

            sortCoworkerHandle.invoke(customerStruct.getSegment());

            Customer backToCustomer = customerStruct.convertBackToEntity();

            System.out.println("---back to java---");
            System.out.println("Name: " + backToCustomer.getName());
            System.out.println("Email: " + backToCustomer.getEmail());
            System.out.println("Co-Workers: " + backToCustomer.getCoWorkers());
            System.out.println("Age: " + backToCustomer.getAge());
            System.out.println("Height: " + backToCustomer.getHeight());
        }
    }
}