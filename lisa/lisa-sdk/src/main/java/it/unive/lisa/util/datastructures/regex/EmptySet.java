package it.unive.lisa.util.datastructures.regex;

import java.util.Collections;
import java.util.Set;

import it.unive.lisa.util.datastructures.automaton.AutomataFactory;
import it.unive.lisa.util.datastructures.automaton.Automaton;
import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;

/**
 * A {@link RegularExpression} representing the empty set of strings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class EmptySet extends RegularExpression {

	/**
	 * The singleton instance.
	 */
	public static final EmptySet INSTANCE = new EmptySet();

	private EmptySet() {
	}

	@Override
	public RegularExpression simplify() {
		return this;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EmptySet;
	}

	@Override
	public String toString() {
		return "∅";
	}

	@Override
	public <A extends Automaton<A, T>,
			T extends TransitionSymbol<T>> A toAutomaton(AutomataFactory<A, T> factory) {
		return factory.emptyLanguage();
	}

	@Override
	protected Set<PartialSubstring> substringAux(int charsToSkip, int missingChars) {
		return Collections.emptySet();
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean is(String str) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int maxLength() {
		return 0;
	}

	@Override
	public int minLength() {
		return 0;
	}

	@Override
	public boolean mayContain(String s) {
		return false;
	}

	@Override
	public boolean contains(String s) {
		return false;
	}

	@Override
	public boolean mayStartWith(String s) {
		return false;
	}

	@Override
	public boolean startsWith(String s) {
		return false;
	}

	@Override
	public boolean mayEndWith(String s) {
		return false;
	}

	@Override
	public boolean endsWith(String s) {
		return false;
	}

	@Override
	protected RegularExpression unrollStarToFixedLength(int length) {
		return this;
	}

	@Override
	public RegularExpression reverse() {
		return this;
	}

	@Override
	protected RegularExpression topAsEmptyString() {
		return this;
	}

	@Override
	protected RegularExpression topAsSingleChar() {
		return this;
	}

	@Override
	public RegularExpression[] explode() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int compareToAux(RegularExpression other) {
		return 0;
	}

	@Override
	public RegularExpression repeat(long n) {
		return this;
	}
}
