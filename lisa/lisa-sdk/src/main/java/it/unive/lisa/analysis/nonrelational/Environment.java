package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

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
		extends
		FunctionalLattice<M, Identifier, T>
		implements
		SemanticDomain<M, E, Identifier> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public Environment(
			T domain) {
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
	public Environment(
			T domain,
			Map<Identifier, T> function) {
		super(domain, function);
	}

	@Override
	@SuppressWarnings("unchecked")
	public M assign(
			Identifier id,
			E expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (isBottom())
			return (M) this;

		if (!lattice.canProcess(expression, pp, oracle))
			return (M) this;

		Map<Identifier, T> func = mkNewFunction(function, false);
		T value = lattice.eval(expression, (M) this, pp, oracle);
		T v = lattice.fixedVariable(id, pp, oracle);
		if (!v.isBottom())
			// some domains might provide fixed representations
			// for some variables
			value = v;
		else if (id.isWeak() && function != null && function.containsKey(id))
			// if we have a weak identifier for which we already have
			// information, we we perform a weak assignment
			value = value.lub(getState(id));
		func.put(id, value);
		return mk(lattice, func);
	}

	@Override
	@SuppressWarnings("unchecked")
	public M smallStepSemantics(
			E expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// environments do not change without assignments
		return (M) this;
	}

	/**
	 * Evaluates the given expression to an abstract value.
	 * 
	 * @param expression the expression to evaluate
	 * @param pp         the program point where the evaluation happens
	 * @param oracle     the oracle for inter-domain communication
	 * 
	 * @return the abstract result of the evaluation
	 * 
	 * @throws SemanticException if an error happens during the evaluation
	 */
	@SuppressWarnings("unchecked")
	public T eval(
			E expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return lattice.eval(expression, (M) this, pp, oracle);
	}

	@Override
	@SuppressWarnings("unchecked")
	public M assume(
			E expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (isBottom())
			return (M) this;
		return lattice.assume((M) this, expression, src, dest, oracle);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Satisfiability satisfies(
			E expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (isBottom())
			return Satisfiability.BOTTOM;

		return lattice.satisfies(expression, (M) this, pp, oracle);
	}

	@Override
	public M pushScope(
			ScopeToken scope)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		M result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.pushScope(scope);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException("Pushing the scope '" + scope + "' raised an error", holder.get());

		return result;
	}

	@Override
	public M popScope(
			ScopeToken scope)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		M result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.popScope(scope);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException("Popping the scope '" + scope + "' raised an error", holder.get());

		return result;
	}

	@SuppressWarnings("unchecked")
	private M liftIdentifiers(
			UnaryOperator<Identifier> lifter)
			throws SemanticException {
		if (isBottom() || isTop())
			return (M) this;

		Map<Identifier, T> function = mkNewFunction(null, false);
		for (Identifier id : getKeys()) {
			Identifier lifted = lifter.apply(id);
			if (lifted != null)
				if (!function.containsKey(lifted))
					function.put(lifted, getState(id));
				else
					function.put(lifted, getState(id).lub(function.get(lifted)));

		}

		return mk(lattice, function);
	}

	@Override
	@SuppressWarnings("unchecked")
	public M forgetIdentifier(
			Identifier id)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return (M) this;

		Map<Identifier, T> result = mkNewFunction(function, false);
		if (result.containsKey(id))
			result.remove(id);

		return mk(lattice, result);
	}

	@Override
	@SuppressWarnings("unchecked")
	public M forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return (M) this;

		Map<Identifier, T> result = mkNewFunction(function, false);
		Set<Identifier> keys = result.keySet().stream().filter(test::test).collect(Collectors.toSet());
		keys.forEach(result::remove);

		return mk(lattice, result);
	}

	@Override
	public Set<Identifier> lubKeys(
			Set<Identifier> k1,
			Set<Identifier> k2)
			throws SemanticException {
		Set<Identifier> keys = new HashSet<>();
		CollectionsDiffBuilder<Identifier> builder = new CollectionsDiffBuilder<>(Identifier.class, k1,
				k2);
		// this is needed for a name-only comparison
		builder.compute(Comparator.comparing(Identifier::getName));
		keys.addAll(builder.getOnlyFirst());
		keys.addAll(builder.getOnlySecond());
		for (Pair<Identifier, Identifier> pair : builder.getCommons())
			try {
				keys.add(pair.getLeft().lub(pair.getRight()));
			} catch (SemanticException e) {
				throw new SemanticException("Unable to lub " + pair.getLeft() + " and " + pair.getRight(), e);
			}
		return keys;
	}

	@Override
	public T stateOfUnknown(
			Identifier key) {
		return lattice.unknownVariable(key);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return getKeys().contains(id);
	}
}
