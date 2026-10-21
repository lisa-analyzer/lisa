# This file is part of sv-witnesses repository: https://github.com/sosy-lab/sv-witnesses
#
# SPDX-FileCopyrightText: 2026 The SV-Witnesses Community
#
# SPDX-License-Identifier: Apache-2.0

import random


def modify(n):
    n += 3
    n *= 2
    return n


if __name__ == "__main__":
    n = random.randint(-100, 100)
    res = modify(n)
    assert res == 2 * (n + 3)
    assert res // (n + 3) == 2
