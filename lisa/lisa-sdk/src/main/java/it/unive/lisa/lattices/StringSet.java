package it.unive.lisa.lattices;

import java.util.Collections;
import java.util.Set;

/**
 * A set lattice containing a set of strings.
 *
 * @author <a href="mailto:giacomo.boldini@unive.it">Giacomo Boldini</a>
 */
public class StringSet
		extends
		SetLattice<StringSet, String> {

	/**
	 * Builds the empty set lattice element.
	 */
	public StringSet() {
		this(Collections.emptySet(), false);
	}

	/**
	 * Builds a singleton set lattice element.
	 *
	 * @param string the string
	 */
	public StringSet(
			String string) {
		this(Collections.singleton(string), false);
	}

	/**
	 * Builds a set lattice element.
	 *
	 * @param set the set of strings
	 */
	public StringSet(
			Set<String> set) {
		this(set, false);
	}

	private StringSet(
			boolean isTop) {
		this(Collections.emptySet(), isTop);
	}

	private StringSet(
			Set<String> set,
			boolean isTop) {
		super(set, isTop);
	}

	@Override
	public StringSet top() {
		return new StringSet(true);
	}

	@Override
	public StringSet bottom() {
		return new StringSet();
	}

	@Override
	public StringSet mk(
			Set<String> set) {
		return new StringSet(set);
	}

}
