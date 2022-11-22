package it.unive.lisa.analysis.nonrelational;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.MapRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
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
 * @param <T> the concrete instance of the {@link NonRelationalElement} whose
 *                instances are mapped in this environment
 * @param <V> the type of value returned by the eval function of objects of type
 *                {@code T}
 */
public abstract class Environment<M extends Environment<M, E, T, V>,
		E extends SymbolicExpression,
		T extends NonRelationalElement<T, E, M>,
		V extends Lattice<V>>
		extends FunctionalLattice<M, Identifier, T> implements SemanticDomain<M, E, Identifier> {

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
		Pair<T, V> eval = eval(expression, pp);
		T value = eval.getLeft();
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
		return assignAux(id, expression, func, value, eval.getRight(), pp);
	}

	/**
	 * Yields the evaluation of the given expression, happening at the given
	 * program point. The result of the evaluation is in the form of
	 * {@code <abstract element, evaluation result>}, where
	 * {@code evaluation result} is the true result of the evaluation, while
	 * {@code abstract element} is the element derived by the result that is to
	 * be stored inside the environment mapped to an identifier.
	 * 
	 * @param expression the expression to evaluate
	 * @param pp         the program point where the evaluation happens
	 * 
	 * @return the result of the evaluation
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	public abstract Pair<T, V> eval(E expression, ProgramPoint pp) throws SemanticException;

	/**
	 * Auxiliary function of
	 * {@link #assign(Identifier, SymbolicExpression, ProgramPoint)} that is
	 * invoked after the evaluation of the expression.
	 * 
	 * @param id         the identifier that has been assigned
	 * @param expression the expression that has been evaluated and assigned
	 * @param function   a copy of the current function, where the {@code id}
	 *                       has been assigned to {@code eval}
	 * @param value      the final value stored for {@code id}, after
	 *                       considering applying
	 *                       {@link NonRelationalElement#variable(Identifier, ProgramPoint)}
	 *                       and {@link Identifier#isWeak()}
	 * @param eval       the abstract value that is the result of the evaluation
	 *                       of {@code value}
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return a new instance of this environment containing the given function,
	 *             obtained by assigning {@code id} to {@code eval}
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public abstract M assignAux(Identifier id, E expression, Map<Identifier, T> function, T value, V eval,
			ProgramPoint pp) throws SemanticException;

	@Override
	@SuppressWarnings("unchecked")
	public M assume(E expression, ProgramPoint pp) throws SemanticException {
		if (isBottom())
			return (M) this;

		Satisfiability sat = lattice.satisfies(expression, (M) this, pp);
		if (sat == Satisfiability.NOT_SATISFIED)
			return bottom();

		if (sat == Satisfiability.SATISFIED)
			return assumeSatisfied(eval(expression, pp).getRight());

		return lattice.assume((M) this, expression, pp);
	}

	/**
	 * Assumes that an expression, that evaluated to {@code eval}, is always
	 * satisfied by this environment. This auxiliary method serves as a
	 * constructor for the final concrete instance of environment.
	 * 
	 * @param eval the result of the evaluation of the expression that is always
	 *                 satisfied
	 * 
	 * @return the (possibly) updated environment
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public abstract M assumeSatisfied(V eval) throws SemanticException;

	@Override
	@SuppressWarnings("unchecked")
	public Satisfiability satisfies(E expression, ProgramPoint pp) throws SemanticException {
		if (isBottom())
			return Satisfiability.BOTTOM;

		return lattice.satisfies(expression, (M) this, pp);
	}

	@Override
	public M pushScope(ScopeToken scope) throws SemanticException {
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
	public M popScope(ScopeToken scope) throws SemanticException {
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
	private M liftIdentifiers(UnaryOperator<Identifier> lifter) throws SemanticException {
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
	public M forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return (M) this;

		Map<Identifier, T> result = mkNewFunction(function, false);
		if (result.containsKey(id))
			result.remove(id);

		return mk(lattice, result);
	}

	@Override
	@SuppressWarnings("unchecked")
	public M forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return (M) this;

		Map<Identifier, T> result = mkNewFunction(function, false);
		Set<Identifier> keys = result.keySet().stream().filter(test::test).collect(Collectors.toSet());
		keys.forEach(result::remove);

		return mk(lattice, result);
	}

	@Override
	public DomainRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();

		if (isBottom())
			return Lattice.bottomRepresentation();

		if (function == null)
			return new StringRepresentation("empty");

		return new MapRepresentation(function, StringRepresentation::new, NonRelationalElement::representation);
	}

	@Override
	public Set<Identifier> lubKeys(Set<Identifier> k1, Set<Identifier> k2) throws SemanticException {
		Set<Identifier> keys = new HashSet<>();
		CollectionsDiffBuilder<Identifier> builder = new CollectionsDiffBuilder<>(Identifier.class, k1,
				k2);
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
}
