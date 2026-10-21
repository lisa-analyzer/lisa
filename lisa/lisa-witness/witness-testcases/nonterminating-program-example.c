// This file is part of sv-witnesses repository: https://github.com/sosy-lab/sv-witnesses
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef enum {false,true} bool;

extern int __VERIFIER_nondet_int(void);

int main() {
    int i;
    i = __VERIFIER_nondet_int();
    
    while (i != 0) {
        if (i < 0) {
            i = i+2;
            if (i < 0) {
                if (i <= -2147483648) return 0;
                i = i*(-1);
            }
        } else {
            i = i-2;
            if (i > 0) {
                i = i*(-1);
            }
        }
    }
    
    return 0;
}
