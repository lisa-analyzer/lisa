package it.unive.lisa.analysis.inference;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.inference.InferredValue.InferredPair;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An inference system that model standard derivation systems (e.g., types
 * systems, small step semantics, big step semantics, ...). An inference system
 * is an {@link Environment} that work on {@link InferredValue}s, and that
 * exposes the last inferred value ({@link #getInferredValue()}) and the
 * execution state ({@link #getExecutionState()}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of {@link InferredValue} in this inference system
 */
public class InferenceSystem<T extends InferredValue<T>> extends FunctionalLattice<InferenceSystem<T>, Identifier, T>
		implements ValueDomain<InferenceSystem<T>> {

	private final InferredPair<T> inferred;

	/**
	 * Builds an empty inference system.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public InferenceSystem(T domain) {
		super(domain);
		inferred = new InferredPair<>(domain.bottom(), domain.bottom(), domain.bottom());
	}

	/**
	 * Builds an inference system identical to the given one, except for the
	 * execution state that will be set to the given one.
	 * 
	 * @param other the inference system to copy
	 * @param state the new execution state
	 */
	public InferenceSystem(InferenceSystem<T> other, T state) {
		this(other.lattice, other.function, new InferredPair<>(other.lattice, other.inferred.getInferred(), state));
	}

	private InferenceSystem(T domain, Map<Identifier, T> function, InferredPair<T> inferred) {
		super(domain, function);
		this.inferred = inferred;
	}

	/**
	 * Yields the execution state (also called program counter), that might
	 * change when evaluating an expression.
	 * 
	 * @return the execution state
	 */
	public T getExecutionState() {
		return inferred.getState();
	}

	/**
	 * Yields the inferred value of the last {@link SymbolicExpression} handled
	 * by this domain, either through
	 * {@link #assign(Identifier, ValueExpression, ProgramPoint)} or
	 * {@link #smallStepSemantics(ValueExpression, ProgramPoint)}.
	 * 
	 * @return the value inferred for the last expression
	 */
	public T getInferredValue() {
		return inferred.getInferred();
	}

	@Override
	public InferenceSystem<T> assign(Identifier id, ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		// If id cannot be tracked by the underlying
		// lattice, return this
		if (!lattice.canProcess(expression) || !lattice.tracksIdentifiers(id))
			return this;

		// the mkNewFunction will return an empty function if the
		// given one is null
		Map<Identifier, T> func = mkNewFunction(function);
		InferredPair<T> eval = lattice.eval(expression, this, pp);
		T v = lattice.variable(id, pp);
		if (!v.isBottom())
			eval = eval.lub(new InferredPair<>(lattice, v, eval.getState()));
		if (id.isWeak())
			eval = eval.lub(new InferredPair<>(lattice, getState(id), eval.getState()));
		func.put(id, eval.getInferred());
		return new InferenceSystem<>(lattice, func, eval);
	}

	@Override
	public InferenceSystem<T> smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		// we update the inferred value
		return new InferenceSystem<>(lattice, function, lattice.eval(expression, this, pp));
	}

	@Override
	public InferenceSystem<T> top() {
		return new InferenceSystem<T>(lattice.top(), null, inferred.top());
	}

	@Override
	public final boolean isTop() {
		// we ignore inferred since we can infer a non-top value even with a
		// top environment
		return lattice.isTop() && function == null;
	}

	@Override
	public InferenceSystem<T> bottom() {
		return new InferenceSystem<T>(lattice.bottom(), null, inferred.bottom());
	}

	@Override
	public final boolean isBottom() {
		// we ignore inferred since we can infer a non-bottom value even with a
		// bottom environment
		return lattice.isBottom() && function == null;
	}

	@Override
	public InferenceSystem<T> lubAux(InferenceSystem<T> other) throws SemanticException {
		InferenceSystem<T> lub = super.lubAux(other);
		if (lub.isTop() || lub.isBottom())
			return lub;
		return new InferenceSystem<>(lub.lattice, lub.function, inferred.lub(other.inferred));
	}

	@Override
	protected Set<Identifier> lubKeys(Set<Identifier> k1, Set<Identifier> k2) throws SemanticException {
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

	@Override
	public InferenceSystem<T> wideningAux(InferenceSystem<T> other) throws SemanticException {
		InferenceSystem<T> widen = super.wideningAux(other);
		if (widen.isTop() || widen.isBottom())
			return widen;
		return new InferenceSystem<>(widen.lattice, widen.function, inferred.widening(other.inferred));
	}

	@Override
	public boolean lessOrEqualAux(InferenceSystem<T> other) throws SemanticException {
		if (!super.lessOrEqualAux(other))
			return false;

		return inferred.lessOrEqual(other.inferred);
	}

	@Override
	public String representation() {
		return toString();
	}

	@Override
	public String toString() {
		if (isBottom() || isTop())
			return super.toString();

		SortedSet<String> res = new TreeSet<>();
		for (Entry<Identifier, T> entry : function.entrySet())
			res.add(entry.getKey() + ": " + entry.getValue().representation());

		return StringUtils.join(res, '\n') + "\n[" + inferred + "]";
	}

	@Override
	public InferenceSystem<T> assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		if (lattice.satisfies(expression, this, pp) == Satisfiability.NOT_SATISFIED)
			return bottom();
		else if (lattice.satisfies(expression, this, pp) == Satisfiability.SATISFIED)
			return this;
		else
			return glb(lattice.assume(this, expression, pp));
	}

	/**
	 * Performs the greatest lower bound between this environment and
	 * {@code other}.
	 * 
	 * @param other the other environment
	 * 
	 * @return the greatest lower bound between this environment and
	 *             {@code other}
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public InferenceSystem<T> glb(InferenceSystem<T> other) throws SemanticException {
		// we always keep the execution state of other since it is the one that
		// has assumed the condition
		if (other == null || this.isBottom() || other.isTop() || this == other || this.equals(other)
				|| this.lessOrEqual(other))
			return new InferenceSystem<>(lattice, function,
					new InferredPair<>(lattice, inferred.getInferred(), other.getExecutionState()));

		if (other.isBottom() || this.isTop() || other.lessOrEqual(this))
			return other;

		InferenceSystem<
				T> lift = functionalLift(other, (k1, k2) -> glbKeys(k1, k2), (o1, o2) -> o1 == null ? o2 : o1.glb(o2));
		return new InferenceSystem<>(lift.lattice, lift.function,
				new InferredPair<>(lift.lattice, inferred.getInferred(), other.getExecutionState()));
	}

	@Override
	public final InferenceSystem<T> forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop() || isBottom())
			return this;

		InferenceSystem<T> result = new InferenceSystem<>(lattice, function, inferred);
		if (result.function.containsKey(id))
			result.function.remove(id);

		return result;
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return lattice.satisfies(expression, this, pp);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((inferred == null) ? 0 : inferred.hashCode());
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
		InferenceSystem<?> other = (InferenceSystem<?>) obj;
		if (inferred == null) {
			if (other.inferred != null)
				return false;
		} else if (!inferred.equals(other.inferred))
			return false;
		return true;
	}
}