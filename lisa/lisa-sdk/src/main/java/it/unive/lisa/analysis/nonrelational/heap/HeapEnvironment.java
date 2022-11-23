package it.unive.lisa.analysis.nonrelational.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An environment for a {@link NonRelationalHeapDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete instance of the {@link NonRelationalHeapDomain} whose
 *                instances are mapped in this environment
 */
public class HeapEnvironment<T extends NonRelationalHeapDomain<T>>
		extends Environment<HeapEnvironment<T>, SymbolicExpression, T, T> implements HeapDomain<HeapEnvironment<T>> {

	/**
	 * The substitution
	 */
	private final List<HeapReplacement> substitution;

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public HeapEnvironment(T domain) {
		super(domain);
		substitution = Collections.emptyList();
	}

	/**
	 * Builds an empty environment from a given mapping.
	 * 
	 * @param domain   singleton instance to be used during semantic operations
	 *                     to retrieve top and bottom values
	 * @param function the initial mapping of this heap environment
	 */
	public HeapEnvironment(T domain, Map<Identifier, T> function) {
		this(domain, function, Collections.emptyList());
	}

	/**
	 * Builds an environment containing the given mapping. If function is
	 * {@code null}, the new environment is the top environment if
	 * {@code lattice.isTop()} holds, and it is the bottom environment if
	 * {@code lattice.isBottom()} holds.
	 * 
	 * @param domain       a singleton instance to be used during semantic
	 *                         operations to retrieve top and bottom values
	 * @param function     the function representing the mapping contained in
	 *                         the new environment; can be {@code null}
	 * @param substitution the list of substitutions that has been generated
	 *                         together with the fresh instance being built
	 */
	public HeapEnvironment(T domain, Map<Identifier, T> function, List<HeapReplacement> substitution) {
		super(domain, function);
		this.substitution = substitution;
	}

	@Override
	public HeapEnvironment<T> mk(T lattice, Map<Identifier, T> function) {
		return new HeapEnvironment<>(lattice, function);
	}

	@Override
	public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return lattice.rewrite(expression, this, pp);
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return substitution;
	}

	@Override
	public Pair<T, T> eval(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		T eval = lattice.eval(expression, this, pp);
		return Pair.of(eval, eval);
	}

	@Override
	public HeapEnvironment<T> assignAux(Identifier id, SymbolicExpression expression, Map<Identifier, T> function,
			T value, T eval, ProgramPoint pp) {
		return new HeapEnvironment<>(lattice, function, eval.getSubstitution());
	}

	@Override
	public HeapEnvironment<T> assumeSatisfied(T eval) {
		return new HeapEnvironment<>(lattice, function, eval.getSubstitution());
	}

	@Override
	public HeapEnvironment<T> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		if (isBottom())
			return this;
		T eval = lattice.eval(expression, this, pp);
		return new HeapEnvironment<>(lattice, function, eval.getSubstitution());
	}

	@Override
	public HeapEnvironment<T> top() {
		return isTop() ? this
				: new HeapEnvironment<>(lattice.top(), null, Collections.emptyList());
	}

	@Override
	public HeapEnvironment<T> bottom() {
		return isBottom() ? this
				: new HeapEnvironment<>(lattice.bottom(), null, Collections.emptyList());
	}

	@Override
	public boolean isTop() {
		return super.isTop() && substitution.isEmpty();
	}

	@Override
	public boolean isBottom() {
		return super.isBottom() && substitution.isEmpty();
	}

	// TODO how do we lub/widen/glb/narrow the substitutions?

	@Override
	public boolean lessOrEqualAux(HeapEnvironment<T> other) throws SemanticException {
		if (!super.lessOrEqualAux(other))
			return false;
		// TODO how do we check the substitutions?
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((substitution == null) ? 0 : substitution.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		HeapEnvironment<?> other = (HeapEnvironment<?>) obj;
		if (substitution == null) {
			if (other.substitution != null)
				return false;
		} else if (!substitution.equals(other.substitution))
			return false;
		return true;
	}
}
