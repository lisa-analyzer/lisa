package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Map;

/**
 * An environment for a {@link NonRelationalDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <M> the concrete type of environment
 * @param <E> the type of expressions that this domain can evaluate
 * @param <T> the concrete instance of the {@link NonRelationalDomain} whose
 *                instances are mapped in this environment
 */
public abstract class Environment<M extends Environment<M, E, T>,
		E extends SymbolicExpression,
		T extends NonRelationalDomain<T, E, M>>
		extends VariableLift<M, E, T> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public Environment(T domain) {
		super(domain);
	}

	/**
	 * Builds an environment containing the given mapping. If function is
	 * {@code null}, the new environment is the top environment if
	 * {@code lattice.isTop()} holds, and it is the bottom environment if
	 * {@code lattice.isBottom()} holds.
	 * 
	 * @param domain   a singleton instance to be used during semantic
	 *                     operations to retrieve top and bottom values
	 * @param function the function representing the mapping contained in the
	 *                     new environment; can be {@code null}
	 */
	public Environment(T domain, Map<Identifier, T> function) {
		super(domain, function);
	}

	@Override
	@SuppressWarnings("unchecked")
	public M assign(Identifier id, E expression, ProgramPoint pp) throws SemanticException {
		if (isBottom())
			return (M) this;

		// If id cannot be tracked by the underlying
		// lattice, return this
		if (!lattice.canProcess(expression) || !lattice.tracksIdentifiers(id))
			return (M) this;

		Map<Identifier, T> func = mkNewFunction(function, false);
		T value = lattice.eval(expression, (M) this, pp);
		T v = lattice.variable(id, pp);
		if (!v.isBottom())
			// some domains might provide fixed representations
			// for some variables
			value = v;
		if (id.isWeak() && function != null && function.containsKey(id))
			// if we have a weak identifier for which we already have
			// information, we we perform a weak assignment
			value = value.lub(getState(id));
		func.put(id, value);
		return mk(lattice, func);
	}

	@Override
	@SuppressWarnings("unchecked")
	public M smallStepSemantics(E expression, ProgramPoint pp) throws SemanticException {
		// environments do not change without assignments
		return (M) this;
	}

	/**
	 * Evaluates the given expression to an abstract value.
	 * 
	 * @param expression the expression to evaluate
	 * @param pp         the program point where the evaluation happens
	 * 
	 * @return the abstract result of the evaluation
	 * 
	 * @throws SemanticException if an error happens during the evaluation
	 */
	@SuppressWarnings("unchecked")
	public T eval(E expression, ProgramPoint pp) throws SemanticException {
		return lattice.eval(expression, (M) this, pp);
	}

	@Override
	@SuppressWarnings("unchecked")
	public M assume(E expression, ProgramPoint src, ProgramPoint dest) throws SemanticException {
		if (isBottom())
			return (M) this;
		return lattice.assume((M) this, expression, src, dest);
	}
}
