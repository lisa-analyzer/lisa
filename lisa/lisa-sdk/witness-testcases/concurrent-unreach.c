// This file is part of the SV-Witnesses repository as regression test:
// https://gitlab.com/sosy-lab/benchmarking/sv-witnesses
//
// SPDX-FileCopyrightText: 2025 The SV-Witnesses Community
//
// SPDX-License-Identifier: Apache-2.0

// Concurrent violation: writer thread writes x=1 before main reads x,
// so 'if (x == 1)' is taken and reach_error() is called.
// x is atomic, so the program is race-free and the witness may constrain it.

#include <pthread.h>

void reach_error() {}

_Atomic int x = 0;

void* writer(void* arg) {
    x = 1;
    return NULL;
}

int main(void) {
    pthread_t t;
    pthread_create(&t, NULL, writer, NULL);
    if (x == 1) reach_error();
    pthread_join(t, NULL);
    return 0;
}
