package it.unive.lisa.analysis.lattices;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A set lattice containing a set of symbolic expressions.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 *
 * @param <T> the type of the tracked symbolic expressions
 */
public class ExpressionSet<T extends SymbolicExpression> extends SetLattice<ExpressionSet<T>, T> {

	private final boolean isTop;

	/**
	 * Builds the empty set lattice element.
	 */
	public ExpressionSet() {
		this(Collections.emptySet(), false);
	}

	/**
	 * Builds a singleton set lattice element.
	 * 
	 * @param exp the expression
	 */
	public ExpressionSet(T exp) {
		this(Collections.singleton(exp), false);
	}

	/**
	 * Builds a set lattice element.
	 * 
	 * @param set the set of expression
	 */
	public ExpressionSet(Set<T> set) {
		this(set, false);
	}

	private ExpressionSet(boolean isTop) {
		this(Collections.emptySet(), isTop);
	}

	private ExpressionSet(Set<T> set, boolean isTop) {
		super(set);
		this.isTop = isTop;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public ExpressionSet<T> top() {
		return new ExpressionSet<T>(true);
	}

	@Override
	public boolean isBottom() {
		return !isTop && elements.isEmpty();
	}

	@Override
	public ExpressionSet<T> bottom() {
		return new ExpressionSet<T>();
	}

	@Override
	protected ExpressionSet<T> mk(Set<T> set) {
		return new ExpressionSet<T>(set);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ExpressionSet<T> lubAux(ExpressionSet<T> other) throws SemanticException {
		Set<T> lub = new HashSet<>();

		// all non-identifiers expressions are part of the lub
		elements.stream().filter(Predicate.not(Identifier.class::isInstance)).forEach(lub::add);
		// common ones will be overwritten
		other.elements.stream().filter(Predicate.not(Identifier.class::isInstance)).forEach(lub::add);

		// identifiers are added after lubbing the ones with the same name
		
		// the following keeps track of the unmatched nodes in second
		Collection<Identifier> copy = new HashSet<>(other.onlyIds());
		boolean found;
		for (Identifier id1 : onlyIds()) {
			found = false;
			for (Identifier id2 : other.onlyIds())
				if (copy.contains(id2) && id1.getName().equals(id2.getName())) {
					copy.remove(id2);
					lub.add((T) id1.lub(id2));
					found = true;
					break;
				}

			if (!found)
				// no match found
				lub.add((T) id1);
		}

		// unmatched in other
		copy.forEach(id -> lub.add((T) id));

		return new ExpressionSet<>(lub);
	}

	private Collection<Identifier> onlyIds() {
		return elements.stream().filter(Identifier.class::isInstance).map(Identifier.class::cast)
				.collect(Collectors.toSet());
	}
}
