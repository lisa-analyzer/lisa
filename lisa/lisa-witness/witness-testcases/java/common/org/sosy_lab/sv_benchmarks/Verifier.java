// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: Contributed by Peter Schrammel
// SPDX-FileCopyrightText: 2011-2025 The SV-Benchmarks Community
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.sv_benchmarks;

import java.util.Random;

/**
 * This class provides an interface between the benchmarks and the verification tools for
 *
 * <ul>
 *   <li/>defining <i>assumptions</i>
 *   <li/>generating <i>nondeterministic values</i>
 * </ul>
 *
 * The verification semantics of the methods is defined in the method documentations below. The
 * bodies of the methods are only approximate to allow execution, but do NOT define the verification
 * semantics.
 *
 * <p>Also see java/README.md
 */
public final class Verifier {
  /**
   * Assume that the given condition holds true. Any execution that does not satisfy the condition
   * cannot proceed.
   *
   * @param condition The assumption
   */
  public static void assume(boolean condition) {
    if (!condition) {
      Runtime.getRuntime().halt(1);
    }
  }

  /** @return A nondeterministically chosen Boolean value (true or false) */
  public static boolean nondetBoolean() {
    return new Random().nextBoolean();
  }

  /** @return A nondeterministicly chosen 8-bit signed integer value */
  public static byte nondetByte() {
    return (byte) (new Random().nextInt());
  }

  /** @return A nondeterministicly chosen 16-bit unsigned integer value */
  public static char nondetChar() {
    return (char) (new Random().nextInt());
  }

  /** @return A nondeterministicly chosen 16-bit signed integer value */
  public static short nondetShort() {
    return (short) (new Random().nextInt());
  }

  /** @return A nondeterministicly chosen 32-bit signed integer value */
  public static int nondetInt() {
    return new Random().nextInt();
  }

  /** @return A nondeterministicly chosen 64-bit signed integer value */
  public static long nondetLong() {
    return new Random().nextLong();
  }

  /** @return A nondeterministicly chosen 32-bit IEEE 754 single-precision floating point value */
  public static float nondetFloat() {
    return new Random().nextFloat();
  }

  /** @return A nondeterministicly chosen 64-bit IEEE 754 double-precision floating point value */
  public static double nondetDouble() {
    return new Random().nextDouble();
  }

  /**
   * @return A non-null character string of nondeterministically chosen length (in range
   *     [0,2147483647]) and consisting of nondeterministically chosen characters.
   */
  public static String nondetString() {
    Random random = new Random();
    int size = random.nextInt();
    assume(size >= 0);
    byte[] bytes = new byte[size];
    random.nextBytes(bytes);
    return new String(bytes);
  }
}
