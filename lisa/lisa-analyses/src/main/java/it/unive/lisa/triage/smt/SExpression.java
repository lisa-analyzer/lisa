package it.unive.lisa.triage.smt;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An s-expression, as produced by SMT-LIB solvers in their responses. An
 * s-expression is either an atom (a symbol, a numeral, a string literal, ...)
 * or a list of s-expressions. Each s-expression also retains its textual
 * representation, normalized to use single spaces between elements.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class SExpression {

	private final String atom;

	private final List<SExpression> children;

	private SExpression(
			String atom,
			List<SExpression> children) {
		this.atom = atom;
		this.children = children;
	}

	/**
	 * Yields {@code true} if this s-expression is an atom.
	 *
	 * @return whether or not this s-expression is an atom
	 */
	public boolean isAtom() {
		return atom != null;
	}

	/**
	 * Yields the text of this atom.
	 *
	 * @return the text, or {@code null} if this s-expression is a list
	 */
	public String getAtom() {
		return atom;
	}

	/**
	 * Yields the elements of this list.
	 *
	 * @return the elements (empty if this s-expression is an atom)
	 */
	public List<SExpression> getChildren() {
		return children;
	}

	@Override
	public String toString() {
		if (atom != null)
			return atom;
		StringBuilder sb = new StringBuilder("(");
		for (int i = 0; i < children.size(); i++) {
			if (i > 0)
				sb.append(" ");
			sb.append(children.get(i));
		}
		return sb.append(")").toString();
	}

	/**
	 * Parses a single s-expression from the given text.
	 *
	 * @param text the text
	 *
	 * @return the parsed s-expression
	 *
	 * @throws SmtException if the text does not contain a well-formed
	 *                          s-expression
	 */
	public static SExpression parse(
			String text) {
		try {
			SExpression res = read(new StringReader(text));
			if (res == null)
				throw new SmtException("Empty s-expression");
			return res;
		} catch (IOException e) {
			throw new SmtException("Cannot parse s-expression: " + text, e);
		}
	}

	/**
	 * Reads the next s-expression from the given reader, skipping whitespaces
	 * and comments before it.
	 *
	 * @param reader the reader
	 *
	 * @return the s-expression, or {@code null} if the reader ends before an
	 *             s-expression starts
	 *
	 * @throws IOException  if reading fails
	 * @throws SmtException if the reader ends in the middle of an s-expression
	 */
	public static SExpression read(
			Reader reader)
			throws IOException {
		int c = skip(reader);
		if (c == -1)
			return null;
		return read(reader, c);
	}

	private static int skip(
			Reader reader)
			throws IOException {
		int c;
		while ((c = reader.read()) != -1)
			if (c == ';')
				while ((c = reader.read()) != -1 && c != '\n')
					;
			else if (!Character.isWhitespace(c))
				return c;
		return -1;
	}

	private static SExpression read(
			Reader reader,
			int first)
			throws IOException {
		if (first == ')')
			throw new SmtException("Unexpected ')'");

		if (first == '(') {
			List<SExpression> children = new ArrayList<>();
			int c;
			while ((c = skip(reader)) != ')') {
				if (c == -1)
					throw new SmtException("Unterminated s-expression");
				children.add(read(reader, c));
			}
			return new SExpression(null, Collections.unmodifiableList(children));
		}

		StringBuilder sb = new StringBuilder();
		sb.appendCodePoint(first);
		if (first == '"' || first == '|') {
			// string literal (with "" as escaped quote) or quoted symbol
			int c;
			while (true) {
				c = reader.read();
				if (c == -1)
					throw new SmtException("Unterminated literal: " + sb);
				sb.appendCodePoint(c);
				if (c == first) {
					if (first == '|')
						break;
					reader.mark(1);
					int next = reader.read();
					if (next == '"')
						sb.appendCodePoint(next);
					else {
						if (next != -1)
							reader.reset();
						break;
					}
				}
			}
			return new SExpression(sb.toString(), Collections.emptyList());
		}

		while (true) {
			reader.mark(1);
			int c = reader.read();
			if (c == -1)
				break;
			if (Character.isWhitespace(c) || c == '(' || c == ')' || c == ';') {
				reader.reset();
				break;
			}
			sb.appendCodePoint(c);
		}
		return new SExpression(sb.toString(), Collections.emptyList());
	}
}
