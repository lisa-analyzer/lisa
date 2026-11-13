// This file is part of the SV-Witnesses repository as regression test:
// https://gitlab.com/sosy-lab/benchmarking/sv-witnesses
//
// SPDX-FileCopyrightText: 2025 The SV-Witnesses Community
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>

void reach_error(){}

pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;
unsigned int used = 0;

void* t1_func(void* arg) {
    pthread_mutex_lock(&m);
    used += 42;
    used -= 42;
    pthread_mutex_unlock(&m);
    return NULL;
}

int main(void) {
    pthread_t t1;
    pthread_create(&t1, NULL, t1_func, NULL);

    pthread_mutex_lock(&m);
    if (used != 0) reach_error();
    pthread_mutex_unlock(&m);
    return 0;
}
