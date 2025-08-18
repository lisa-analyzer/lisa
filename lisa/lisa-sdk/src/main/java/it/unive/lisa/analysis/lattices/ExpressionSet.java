package it.unive.lisa.analysis.lattices;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.ScopedObject;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticExceptionWrapper;
import it.unive.lisa.program.cfg.ProgramPoint;
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
 * A set lattice containing a set of symbolic expressions.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class ExpressionSet extends SetLattice<ExpressionSet, SymbolicExpression>
		implements
		ScopedObject<ExpressionSet> {

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
	public ExpressionSet(
			SymbolicExpression exp) {
		this(Collections.singleton(exp), false);
	}

	/**
	 * Builds a set lattice element.
	 * 
	 * @param set the set of expression
	 */
	public ExpressionSet(
			Set<SymbolicExpression> set) {
		this(set, false);
	}

	private ExpressionSet(
			boolean isTop) {
		this(Collections.emptySet(), isTop);
	}

	private ExpressionSet(
			Set<SymbolicExpression> set,
			boolean isTop) {
		super(set, isTop);
	}

	@Override
	public ExpressionSet top() {
		return new ExpressionSet(true);
	}

	@Override
	public ExpressionSet bottom() {
		return new ExpressionSet();
	}

	@Override
	public ExpressionSet mk(
			Set<SymbolicExpression> set) {
		return new ExpressionSet(set);
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
		ExpressionSet other = (ExpressionSet) obj;
		if (isTop != other.isTop)
			return false;
		return true;
	}

	@Override
	public ExpressionSet lubAux(
			ExpressionSet other)
			throws SemanticException {
		Set<SymbolicExpression> lub = new HashSet<>();

		// all non-identifiers expressions are part of the lub
		elements.stream().filter(Predicate.not(Identifier.class::isInstance)).forEach(lub::add);
		// common ones will be overwritten
		other.elements.stream().filter(Predicate.not(Identifier.class::isInstance)).forEach(lub::add);

		// identifiers are added after lubbing the ones with the same name
		Set<Identifier> idlub = new HashSet<>();
		CollectionUtilities.join(
			onlyIds(),
			other.onlyIds(),
			idlub,
			(
					id1,
					id2
			) -> id1.getName().equals(id2.getName()),
			ExpressionSet::wrapper);
		idlub.forEach(lub::add);

		return new ExpressionSet(lub);
	}

	private static Identifier wrapper(
			Identifier id1,
			Identifier id2) {
		try {
			return id1.lub(id2);
		} catch (SemanticException e) {
			throw new SemanticExceptionWrapper(e);
		}
	}

	private Collection<Identifier> onlyIds() {
		return elements.stream()
			.filter(Identifier.class::isInstance)
			.map(Identifier.class::cast)
			.collect(Collectors.toSet());
	}

	@Override
	public ExpressionSet pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		Set<SymbolicExpression> mapped = new HashSet<>();
		for (SymbolicExpression exp : elements)
			mapped.add(exp.pushScope(token, pp));
		return new ExpressionSet(mapped);
	}

	@Override
	public ExpressionSet popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		Set<SymbolicExpression> mapped = new HashSet<>();
		for (SymbolicExpression exp : elements)
			mapped.add(exp.popScope(token, pp));
		return new ExpressionSet(mapped);
	}

}
