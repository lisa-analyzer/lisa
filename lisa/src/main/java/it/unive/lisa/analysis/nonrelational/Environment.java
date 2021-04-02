package it.unive.lisa.analysis.nonrelational;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;

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
		extends FunctionalLattice<M, Identifier, T> implements SemanticDomain<M, E, Identifier> {

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	protected Environment(T domain) {
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
	protected Environment(T domain, Map<Identifier, T> function) {
		super(domain, function);
	}

	/**
	 * Copies this environment. The function of the returned environment
	 * <b>must</b> be a (shallow) copy of the one of the given environment.
	 * 
	 * @return a copy of the given environment
	 */
	protected abstract M copy();

	@Override
	@SuppressWarnings("unchecked")
	public final M assign(Identifier id, E value, ProgramPoint pp) throws SemanticException {
		// If id cannot be tracked by the underlying
		// lattice, return this
		if (!lattice.canProcess(value) || !lattice.tracksIdentifiers(id))
			return (M) this;

		// the mkNewFunction will return an empty function if the
		// given one is null
		Map<Identifier, T> func = mkNewFunction(function);
		T eval = lattice.eval(value, (M) this, pp);
		if (id.isWeak())
			eval = eval.lub(getState(id));
		func.put(id, eval);
		return assignAux(id, value, func, eval, pp);
	}

	/**
	 * Auxiliary function of
	 * {@link #assign(Identifier, SymbolicExpression, ProgramPoint)} that is
	 * invoked after the evaluation of the expression.
	 * 
	 * @param id       the identifier that has been assigned
	 * @param value    the expression that has been evaluated and assigned
	 * @param function a copy of the current function, where the {@code id} has
	 *                     been assigned to {@code eval}
	 * @param eval     the abstract value that is the result of the evaluation
	 *                     of {@code value}
	 * @param pp       the program point that where this operation is being
	 *                     evaluated
	 * 
	 * @return a new instance of this environment containing the given function,
	 *             obtained by assigning {@code id} to {@code eval}
	 */
	protected abstract M assignAux(Identifier id, E value, Map<Identifier, T> function, T eval, ProgramPoint pp);

	@Override
	@SuppressWarnings("unchecked")
	public M assume(E expression, ProgramPoint pp) throws SemanticException {
		if (lattice.satisfies(expression, (M) this, pp) == Satisfiability.NOT_SATISFIED)
			return bottom();
		else if (lattice.satisfies(expression, (M) this, pp) == Satisfiability.SATISFIED)
			return (M) this;
		else
			return assumeAux(expression, pp);
	}

	/**
	 * Auxiliary version of {@link #assume(SymbolicExpression, ProgramPoint)}
	 * where the cases where the expression is never satisfied
	 * ({@code lattice.satisfies(expression, this) == Satisfiability.NOT_SATISFIED})
	 * and is always satisfied
	 * ({@code lattice.satisfies(expression, this) == Satisfiability.SATISFIED})
	 * have already been handled. The given expression thus holds sometimes.
	 * 
	 * @param expression the expression to assume to hold.
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return the (optionally) modified copy of this domain
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected M assumeAux(E expression, ProgramPoint pp) throws SemanticException {
		// TODO: a more precise filtering is needed when satisfiability of
		// expression is unknown
		// subclasses might add some logic
		return copy();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Satisfiability satisfies(E expression, ProgramPoint pp) throws SemanticException {
		return lattice.satisfies(expression, (M) this, pp);
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * An environment is the top environment if the underlying lattice's
	 * {@code isTop()} holds and its function is {@code null}.
	 */
	@Override
	public final boolean isTop() {
		return lattice.isTop() && function == null;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * An environment is the bottom environment if the underlying lattice's
	 * {@code isBottom()} holds and its function is {@code null}.
	 */
	@Override
	public final boolean isBottom() {
		return lattice.isBottom() && function == null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final M forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop() || isBottom())
			return (M) this;

		M result = copy();
		if (result.function.containsKey(id))
			result.function.remove(id);

		return result;
	}

	@Override
	public final String toString() {
		return representation();
	}

	@Override
	public String representation() {
		if (isTop())
			return Lattice.TOP_STRING;

		if (isBottom())
			return Lattice.BOTTOM_STRING;

		SortedSet<String> res = new TreeSet<>();
		for (Entry<Identifier, T> entry : function.entrySet())
			res.add(entry.getKey() + ": " + entry.getValue().representation());

		return StringUtils.join(res, '\n');
	}
	
	@Override
	protected Set<Identifier> functionalLiftKeys(M other) {
		Set<Identifier> keys = new HashSet<>();
		CollectionsDiffBuilder<Identifier> builder = new CollectionsDiffBuilder<>(Identifier.class, function.keySet(), other.function.keySet());
		builder.compute(Comparator.comparing(Identifier::getName));
		keys.addAll(builder.getOnlyFirst());
		keys.addAll(builder.getOnlySecond());
		for (Pair<Identifier, Identifier> pair : builder.getCommons())
			try {
				keys.add(pair.getLeft().lub(pair.getRight()));
			} catch (SemanticException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return keys;
	}
}