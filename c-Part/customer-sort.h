#ifndef CUSTOMER_SORT_H
#define CUSTOMER_SORT_H

struct customer {
    char* name;
    char* email;
    void* coWorkers;
    double height;
    int age;
};

void printCustomer(struct customer* c);
void printCoworkerList(void* list);
void sortCoWorkers(struct customer* c);

#endif
