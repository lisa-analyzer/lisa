# This file is part of sv-witnesses repository: https://github.com/sosy-lab/sv-witnesses
#
# SPDX-FileCopyrightText: 2026 Raphaël Monat/Inria
#
# SPDX-License-Identifier: Apache-2.0

import random

def average(l):
    s = 0
    for i in range(len(l)):
        s += l[i]
    return s / len(l)

if __name__ == "__main__":
    ll = random.randint(0, 100)
    if(ll > 0):
        l = [random.randint(-10, 10) for i in range(ll)]
        r = average(l)
