package it.unive.lisa.triage.smt;

import java.math.BigInteger;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An immutable term of the SMT-LIB language, together with its {@link Sort}.
 * Terms are identified by their SMT-LIB representation. Constants are created
 * by {@link SmtSolver#declare(String, Sort)}, while all other terms are built
 * through the static factories of this class.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Term {

	/**
	 * The {@code true} literal.
	 */
	public static final Term TRUE = new Term("true", Sort.BOOL);

	/**
	 * The {@code false} literal.
	 */
	public static final Term FALSE = new Term("false", Sort.BOOL);

	private static final Pattern SIMPLE_SYMBOL = Pattern.compile("[a-zA-Z~!@$%^&*_+=<>.?/\\-][a-zA-Z0-9~!@$%^&*_+=<>.?/\\-]*");

	private final String smtlib;

	private final Sort sort;

	private Term(
			String smtlib,
			Sort sort) {
		this.smtlib = smtlib;
		this.sort = sort;
	}

	/**
	 * Yields the SMT-LIB representation of this term.
	 *
	 * @return the representation
	 */
	public String toSmtLib() {
		return smtlib;
	}

	/**
	 * Yields the sort of this term.
	 *
	 * @return the sort
	 */
	public Sort getSort() {
		return sort;
	}

	/**
	 * Yields a valid SMT-LIB symbol for the given name, that is, the name
	 * itself if it is a simple symbol, or the name quoted between {@code |}
	 * otherwise. Characters that cannot appear in quoted symbols ({@code |}
	 * and {@code \}) are replaced with {@code _}.
	 *
	 * @param name the name
	 *
	 * @return the symbol
	 */
	public static String symbol(
			String name) {
		if (SIMPLE_SYMBOL.matcher(name).matches())
			return name;
		return "|" + name.replace('|', '_').replace('\\', '_') + "|";
	}

	/**
	 * Builds a constant with the given name and sort. Constants must be
	 * declared before being used: use {@link SmtSolver#declare(String, Sort)}
	 * to obtain declared constants.
	 *
	 * @param name the name of the constant
	 * @param sort the sort of the constant
	 *
	 * @return the constant
	 */
	static Term constant(
			String name,
			Sort sort) {
		return new Term(symbol(name), sort);
	}

	/**
	 * Builds a boolean literal.
	 *
	 * @param value the value of the literal
	 *
	 * @return the literal
	 */
	public static Term bool(
			boolean value) {
		return value ? TRUE : FALSE;
	}

	/**
	 * Builds an integer literal.
	 *
	 * @param value the value of the literal
	 *
	 * @return the literal
	 */
	public static Term integer(
			long value) {
		return integer(BigInteger.valueOf(value));
	}

	/**
	 * Builds an integer literal.
	 *
	 * @param value the value of the literal
	 *
	 * @return the literal
	 */
	public static Term integer(
			BigInteger value) {
		if (value.signum() < 0)
			return new Term("(- " + value.negate() + ")", Sort.INT);
		return new Term(value.toString(), Sort.INT);
	}

	/**
	 * Builds a bit-vector literal of the given width, whose bits are the
	 * {@code width} least significant bits of the two's complement
	 * representation of {@code value}.
	 *
	 * @param value the value of the literal
	 * @param width the width of the literal
	 *
	 * @return the literal
	 */
	public static Term bitVector(
			long value,
			int width) {
		Sort sort = Sort.bitVector(width);
		BigInteger unsigned = BigInteger.valueOf(value).and(BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE));
		return new Term("(_ bv" + unsigned + " " + width + ")", sort);
	}

	/**
	 * Builds a string literal.
	 *
	 * @param value the value of the literal
	 *
	 * @return the literal
	 */
	public static Term string(
			String value) {
		StringBuilder sb = new StringBuilder("\"");
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '"')
				sb.append("\"\"");
			else if (c < 32 || c > 126 || c == '\\')
				// unicode escape, as defined by the SMT-LIB strings theory
				sb.append(String.format("\\u{%x}", (int) c));
			else
				sb.append(c);
		}
		return new Term(sb.append("\"").toString(), Sort.STRING);
	}

	/**
	 * Builds a term from a value of the given sort, in SMT-LIB syntax, as
	 * printed by a solver in a model (e.g., {@code (- 3)} or
	 * {@code (Value.Int 5)}).
	 *
	 * @param smtlib the value
	 * @param sort   the sort of the value
	 *
	 * @return the term
	 */
	public static Term value(
			String smtlib,
			Sort sort) {
		return new Term(smtlib, sort);
	}

	/**
	 * Builds the application of the given function to the given arguments.
	 *
	 * @param function the function symbol (or indexed identifier, such as
	 *                     {@code (_ extract 7 0)})
	 * @param sort     the sort of the result of the application
	 * @param args     the arguments
	 *
	 * @return the application
	 */
	public static Term app(
			String function,
			Sort sort,
			Term... args) {
		if (args.length == 0)
			return new Term(function, sort);
		StringBuilder sb = new StringBuilder("(").append(function);
		for (Term arg : args)
			sb.append(" ").append(arg.smtlib);
		return new Term(sb.append(")").toString(), sort);
	}

	/**
	 * Builds the negation of the given boolean term.
	 *
	 * @param arg the term
	 *
	 * @return the negation
	 */
	public static Term not(
			Term arg) {
		return app("not", Sort.BOOL, arg);
	}

	/**
	 * Builds the conjunction of the given boolean terms.
	 *
	 * @param args the terms
	 *
	 * @return the conjunction ({@link #TRUE} if no terms are given)
	 */
	public static Term and(
			Term... args) {
		if (args.length == 0)
			return TRUE;
		if (args.length == 1)
			return args[0];
		return app("and", Sort.BOOL, args);
	}

	/**
	 * Builds the disjunction of the given boolean terms.
	 *
	 * @param args the terms
	 *
	 * @return the disjunction ({@link #FALSE} if no terms are given)
	 */
	public static Term or(
			Term... args) {
		if (args.length == 0)
			return FALSE;
		if (args.length == 1)
			return args[0];
		return app("or", Sort.BOOL, args);
	}

	/**
	 * Builds the implication between the given boolean terms.
	 *
	 * @param left  the premise
	 * @param right the conclusion
	 *
	 * @return the implication
	 */
	public static Term implies(
			Term left,
			Term right) {
		return app("=>", Sort.BOOL, left, right);
	}

	/**
	 * Builds the equality between the given terms.
	 *
	 * @param left  the first term
	 * @param right the second term
	 *
	 * @return the equality
	 */
	public static Term eq(
			Term left,
			Term right) {
		return app("=", Sort.BOOL, left, right);
	}

	/**
	 * Builds the conditional term {@code if cond then left else right}.
	 *
	 * @param cond  the condition
	 * @param left  the term yielded if the condition holds
	 * @param right the term yielded otherwise
	 *
	 * @return the conditional term
	 */
	public static Term ite(
			Term cond,
			Term left,
			Term right) {
		return app("ite", left.sort, cond, left, right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(smtlib, sort);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Term other = (Term) obj;
		return Objects.equals(smtlib, other.smtlib) && Objects.equals(sort, other.sort);
	}

	@Override
	public String toString() {
		return smtlib;
	}
}
