package it.unive.lisa.triage.smt;

import java.math.BigInteger;
import java.util.List;

/**
 * Conversions between Java floating point numbers and their representation in
 * the SMT-LIB theory of floating point numbers ({@link Sort#FLOAT32} and
 * {@link Sort#FLOAT64}), through their IEEE 754 encodings: literals are built
 * from the bits of the numbers, so that they denote exactly the same numbers,
 * and values in models are converted back to the numbers with the same bits.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class FloatingPoints {

	private FloatingPoints() {
	}

	/**
	 * Builds the literal denoting the given double precision number.
	 *
	 * @param value the number
	 *
	 * @return the literal, of sort {@link Sort#FLOAT64}
	 */
	public static Term literal(
			double value) {
		return literal(BigInteger.valueOf(Double.doubleToRawLongBits(value)), Sort.FLOAT64);
	}

	/**
	 * Builds the literal denoting the given single precision number.
	 *
	 * @param value the number
	 *
	 * @return the literal, of sort {@link Sort#FLOAT32}
	 */
	public static Term literal(
			float value) {
		return literal(BigInteger.valueOf(Float.floatToRawIntBits(value)), Sort.FLOAT32);
	}

	private static Term literal(
			BigInteger rawBits,
			Sort sort) {
		int eb = sort.getExponentBits();
		int sb = sort.getSignificandBits() - 1;
		int width = 1 + eb + sb;
		// the bits of the number, as an unsigned integer
		BigInteger bits = rawBits.signum() < 0 ? rawBits.add(BigInteger.ONE.shiftLeft(width)) : rawBits;
		String binary = String.format("%" + width + "s", bits.toString(2)).replace(' ', '0');
		return Term.value("(fp #b" + binary.charAt(0) + " #b" + binary.substring(1, 1 + eb) + " #b"
				+ binary.substring(1 + eb) + ")", sort);
	}

	/**
	 * Converts a floating point value, in SMT-LIB syntax as printed in models
	 * (e.g., {@code (fp #b0 #b10000000000 #x8000000000000)},
	 * {@code (_ NaN 11 53)}, {@code (_ -zero 8 24)}), to the Java number with
	 * the same bits.
	 *
	 * @param smtlib the value
	 * @param sort   the sort of the value
	 *
	 * @return the number ({@link Double} for {@link Sort#FLOAT64},
	 *             {@link Float} for {@link Sort#FLOAT32})
	 *
	 * @throws SmtException if the value cannot be parsed
	 */
	public static Number parse(
			String smtlib,
			Sort sort) {
		return parse(SExpression.parse(smtlib), sort);
	}

	/**
	 * Converts a floating point value, as an s-expression, to the Java number
	 * with the same bits (see {@link #parse(String, Sort)}).
	 *
	 * @param value the value
	 * @param sort  the sort of the value
	 *
	 * @return the number
	 *
	 * @throws SmtException if the value cannot be parsed
	 */
	public static Number parse(
			SExpression value,
			Sort sort) {
		boolean dbl = sort.equals(Sort.FLOAT64);
		if (!sort.isFloatingPoint() || value.isAtom())
			throw new SmtException("Not a floating point value: " + value);
		List<SExpression> children = value.getChildren();
		String head = children.get(0).isAtom() ? children.get(0).getAtom() : "";
		if (head.equals("_") && children.size() >= 2 && children.get(1).isAtom()) {
			switch (children.get(1).getAtom()) {
			case "NaN":
				return dbl ? (Number) Double.NaN : (Number) Float.NaN;
			case "+oo":
				return dbl ? (Number) Double.POSITIVE_INFINITY : (Number) Float.POSITIVE_INFINITY;
			case "-oo":
				return dbl ? (Number) Double.NEGATIVE_INFINITY : (Number) Float.NEGATIVE_INFINITY;
			case "+zero":
				return dbl ? (Number) 0.0d : (Number) 0.0f;
			case "-zero":
				return dbl ? (Number) (-0.0d) : (Number) (-0.0f);
			default:
				break;
			}
		}
		if (head.equals("fp") && children.size() == 4) {
			StringBuilder bits = new StringBuilder();
			for (int i = 1; i < 4; i++)
				bits.append(binary(children.get(i)));
			long raw = new BigInteger(bits.toString(), 2).longValue();
			return dbl ? (Number) Double.longBitsToDouble(raw) : (Number) Float.intBitsToFloat((int) raw);
		}
		throw new SmtException("Not a floating point value: " + value);
	}

	/**
	 * Yields the sort of the given floating point value, as printed in models,
	 * from the width of its exponent.
	 *
	 * @param value the value
	 *
	 * @return the sort, or {@code null} if the value is not a floating point
	 *             value
	 */
	public static Sort sortOf(
			SExpression value) {
		if (value.isAtom())
			return null;
		List<SExpression> children = value.getChildren();
		if (children.isEmpty() || !children.get(0).isAtom())
			return null;
		String head = children.get(0).getAtom();
		int eb = -1;
		if (head.equals("fp") && children.size() == 4 && children.get(2).isAtom()) {
			String exponent = children.get(2).getAtom();
			eb = exponent.startsWith("#b") ? exponent.length() - 2
					: exponent.startsWith("#x") ? (exponent.length() - 2) * 4 : -1;
		} else if (head.equals("_") && children.size() == 4 && children.get(2).isAtom())
			try {
				eb = Integer.parseInt(children.get(2).getAtom());
			} catch (NumberFormatException e) {
				return null;
			}
		return eb == Sort.FLOAT32.getExponentBits() ? Sort.FLOAT32
				: eb == Sort.FLOAT64.getExponentBits() ? Sort.FLOAT64 : null;
	}

	private static String binary(
			SExpression literal) {
		if (!literal.isAtom())
			throw new SmtException("Not a bit-vector literal: " + literal);
		String atom = literal.getAtom();
		if (atom.startsWith("#b"))
			return atom.substring(2);
		if (atom.startsWith("#x")) {
			String hex = atom.substring(2);
			String bin = new BigInteger(hex, 16).toString(2);
			return String.format("%" + (hex.length() * 4) + "s", bin).replace(' ', '0');
		}
		throw new SmtException("Not a bit-vector literal: " + literal);
	}
}
