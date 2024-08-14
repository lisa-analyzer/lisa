package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.ScopedObject;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticExceptionWrapper;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An inverse set lattice containing a set of symbolic expressions.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class ExpressionInverseSet extends InverseSetLattice<ExpressionInverseSet, SymbolicExpression>
		implements
		ScopedObject<ExpressionInverseSet> {

	/**
	 * Builds the empty set lattice element.
	 */
	public ExpressionInverseSet() {
		this(Collections.emptySet(), false);
	}

	/**
	 * Builds a singleton set lattice element.
	 * 
	 * @param exp the expression
	 */
	public ExpressionInverseSet(
			SymbolicExpression exp) {
		this(Collections.singleton(exp), false);
	}

	/**
	 * Builds a set lattice element.
	 * 
	 * @param set the set of expression
	 */
	public ExpressionInverseSet(
			Set<SymbolicExpression> set) {
		this(Collections.unmodifiableSet(set), false);
	}

	private ExpressionInverseSet(
			boolean isTop) {
		this(Collections.emptySet(), isTop);
	}

	private ExpressionInverseSet(
			Set<SymbolicExpression> set,
			boolean isTop) {
		super(set, isTop);
	}

	@Override
	public ExpressionInverseSet top() {
		return new ExpressionInverseSet(true);
	}

	@Override
	public ExpressionInverseSet bottom() {
		return new ExpressionInverseSet();
	}

	@Override
	public ExpressionInverseSet mk(
			Set<SymbolicExpression> set) {
		return new ExpressionInverseSet(set);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isTop ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionInverseSet other = (ExpressionInverseSet) obj;
		if (isTop != other.isTop)
			return false;
		return true;
	}

	@Override
	public ExpressionInverseSet lubAux(
			ExpressionInverseSet other)
			throws SemanticException {
		Set<SymbolicExpression> lub = new HashSet<>(exceptIds());
		lub.retainAll(other.exceptIds());

		Set<Identifier> idlub = new HashSet<>();
		CollectionUtilities.meet(onlyIds(), other.onlyIds(), idlub, (
				id1,
				id2) -> id1.getName().equals(id2.getName()),
				ExpressionInverseSet::wrapper);
		idlub.forEach(lub::add);

		if (lub.isEmpty())
			return top();

		return new ExpressionInverseSet(lub);
	}

	private static Identifier wrapper(
			Identifier id1,
			Identifier id2) {
		try {
			// we keep using the lub here as it is basically an equality
			// operator, distinguishing only between weak and strong identifiers
			return id1.lub(id2);
		} catch (SemanticException e) {
			throw new SemanticExceptionWrapper(e);
		}
	}

	private Collection<Identifier> onlyIds() {
		return elements.stream().filter(Identifier.class::isInstance).map(Identifier.class::cast)
				.collect(Collectors.toSet());
	}

	private Collection<SymbolicExpression> exceptIds() {
		return elements.stream().filter(Predicate.not(Identifier.class::isInstance))
				.collect(Collectors.toSet());
	}

	@Override
	public ExpressionInverseSet pushScope(
			ScopeToken token)
			throws SemanticException {
		Set<SymbolicExpression> mapped = new HashSet<>();
		for (SymbolicExpression exp : elements)
			mapped.add(exp.pushScope(token));
		return new ExpressionInverseSet(mapped);
	}

	@Override
	public ExpressionInverseSet popScope(
			ScopeToken token)
			throws SemanticException {
		Set<SymbolicExpression> mapped = new HashSet<>();
		for (SymbolicExpression exp : elements)
			mapped.add(exp.popScope(token));
		return new ExpressionInverseSet(mapped);
	}
}
