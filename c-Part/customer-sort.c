#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "customer-sort.h"


void printCustomer(struct customer* c) {
    printf("Name: %s\n", c->name);
    printf("Email: %s\n", c->email);
    printf("Age: %d\n", c->age);
    printf("Height: %.2f\n", c->height);
    printCoworkerList(c->coWorkers);
}

void printCoworkerList(void*  list) {
    long size = *(long*)list;
    for (int i = 0; i < size; i++) {
        char* coworker = *(char**)(list + sizeof(long) + i * sizeof(char*));
        printf("%s\n", coworker);
    }
}

void sortCoWorkers(struct customer* c) {
    long size = *(long*)c->coWorkers;
    char** coworkers = (char**)(c->coWorkers + sizeof(long));
    for (int i = 0; i < size - 1; i++) {
        for (int j = 0; j < size - i - 1; j++) {
            if (strcmp(coworkers[j], coworkers[j + 1]) > 0) {
                char* temp = coworkers[j];
                coworkers[j] = coworkers[j + 1];
                coworkers[j + 1] = temp;
            }
        }
    }
}