// This file is part of the SV-Witnesses repository as regression test:
// https://gitlab.com/sosy-lab/benchmarking/sv-witnesses
//
// SPDX-FileCopyrightText: 2025 The SV-Witnesses Community
//
// SPDX-License-Identifier: Apache-2.0

// Concurrent no-overflow violation: the writer thread stores INT_MAX into x
// before main reads it, so `x + 1` overflows in main. x is atomic, so the
// program is race-free and the signed-integer overflow is the only violation.

#include <pthread.h>

_Atomic int x = 0;
int y = 0;

void* writer(void* arg) {
    x = 2147483647;   /* INT_MAX for ILP32 */
    return NULL;
}

int main(void) {
    pthread_t t;
    pthread_create(&t, NULL, writer, NULL);
    y = x + 1;
    pthread_join(t, NULL);
    return 0;
}
