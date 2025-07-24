package it.unive.lisa.lattices.informationFlow;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link FunctionalLattice} for the non interference analysis, which contains
 * instances of {@link NonInterferenceValue} as values, and uses
 * {@link Identifier}s as keys. Instances of this class also contain a reference
 * to the non interference levels of all the guards protecting the program point
 * where this object was created. These are used to determine the execution
 * state, which can be retrieved via {@link #getExecutionState()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NonInterferenceEnvironment
		extends
		FunctionalLattice<NonInterferenceEnvironment, Identifier, NonInterferenceValue>
		implements
		ValueLattice<NonInterferenceEnvironment> {

	/**
	 * The non interference levels of the guards protecting the program point
	 * where this object was created.
	 */
	public final GenericMapLattice<ProgramPoint, NonInterferenceValue> guards;

	/**
	 * Builds a new instance of the non interference environment.
	 */
	public NonInterferenceEnvironment() {
		super(NonInterferenceValue.HIGH_LOW);
		this.guards = new GenericMapLattice<ProgramPoint, NonInterferenceValue>(NonInterferenceValue.LOW_HIGH).top();
	}

	/**
	 * Builds a new instance of the non interference environment, with the given
	 * {@link NonInterferenceValue} as the lattice, the given map of
	 * {@link Identifier}s to {@link NonInterferenceValue} as the function, and
	 * the given guards.
	 * 
	 * @param domain   the lattice to use for the non interference levels
	 * @param function the map of {@link Identifier}s to
	 *                     {@link NonInterferenceValue} to use as the function
	 * @param guards   the non interference levels of the guards protecting the
	 *                     program point where this object was created
	 */
	public NonInterferenceEnvironment(
			NonInterferenceValue domain,
			Map<Identifier, NonInterferenceValue> function,
			GenericMapLattice<ProgramPoint, NonInterferenceValue> guards) {
		super(domain, function);
		this.guards = guards;
	}

	@Override
	public NonInterferenceEnvironment pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		NonInterferenceEnvironment result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.pushScope(scope, pp);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException(
					"Pushing the scope '" + scope + "' raised an error",
					holder.get());

		return result;
	}

	@Override
	public NonInterferenceEnvironment popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		NonInterferenceEnvironment result = liftIdentifiers(id -> {
			try {
				return (Identifier) id.popScope(scope, pp);
			} catch (SemanticException e) {
				holder.set(e);
			}
			return null;
		});

		if (holder.get() != null)
			throw new SemanticException(
					"Popping the scope '" + scope + "' raised an error",
					holder.get());

		return result;
	}

	private NonInterferenceEnvironment liftIdentifiers(
			UnaryOperator<Identifier> lifter)
			throws SemanticException {
		if (isBottom() || isTop())
			return this;

		Map<Identifier, NonInterferenceValue> function = mkNewFunction(null, false);
		for (Identifier id : getKeys()) {
			Identifier lifted = lifter.apply(id);
			if (lifted != null)
				if (!function.containsKey(lifted))
					function.put(lifted, getState(id));
				else
					function.put(lifted, getState(id).lub(function.get(lifted)));

		}

		return new NonInterferenceEnvironment(lattice, function, guards);
	}

	@Override
	public NonInterferenceEnvironment forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<Identifier, NonInterferenceValue> result = mkNewFunction(function, false);
		result.remove(id);

		return new NonInterferenceEnvironment(lattice, result, guards);
	}

	@Override
	public NonInterferenceEnvironment forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<Identifier, NonInterferenceValue> result = mkNewFunction(function, false);
		for (Identifier id : ids)
			result.remove(id);

		return new NonInterferenceEnvironment(lattice, result, guards);
	}

	@Override
	public NonInterferenceEnvironment forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<Identifier, NonInterferenceValue> result = mkNewFunction(function, false);
		Set<Identifier> keys = result.keySet().stream().filter(test::test).collect(Collectors.toSet());
		keys.forEach(result::remove);

		return new NonInterferenceEnvironment(lattice, result, guards);
	}

	@Override
	public Set<Identifier> lubKeys(
			Set<Identifier> k1,
			Set<Identifier> k2)
			throws SemanticException {
		Set<Identifier> keys = new HashSet<>();
		CollectionsDiffBuilder<Identifier> builder = new CollectionsDiffBuilder<>(Identifier.class, k1, k2);
		// this is needed for a name-only comparison
		builder.compute(Comparator.comparing(Identifier::getName));
		keys.addAll(builder.getOnlyFirst());
		keys.addAll(builder.getOnlySecond());
		for (Pair<Identifier, Identifier> pair : builder.getCommons())
			try {
				keys.add(pair.getLeft().lub(pair.getRight()));
			} catch (SemanticException e) {
				throw new SemanticException(
						"Unable to lub " + pair.getLeft() + " and " + pair.getRight(),
						e);
			}
		return keys;
	}

	@Override
	public NonInterferenceValue stateOfUnknown(
			Identifier key) {
		return lattice.unknownValue(key);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return getKeys().contains(id);
	}

	@Override
	public NonInterferenceEnvironment top() {
		return isTop() ? this : new NonInterferenceEnvironment(lattice.top(), null, guards.top());
	}

	@Override
	public boolean isTop() {
		return super.isTop() && guards.isTop();
	}

	@Override
	public NonInterferenceEnvironment bottom() {
		return isBottom() ? this : new NonInterferenceEnvironment(lattice.bottom(), null, guards.bottom());
	}

	@Override
	public boolean isBottom() {
		return super.isBottom() && guards.isBottom();
	}

	@Override
	public NonInterferenceEnvironment mk(
			NonInterferenceValue lattice,
			Map<Identifier, NonInterferenceValue> function) {
		return new NonInterferenceEnvironment(lattice, function, guards);
	}

	/**
	 * Yields the state in which the execution is. This corresponds to the lub
	 * of the {@link NonInterferenceValue} instances of all the guards
	 * protecting the program point where this object was created.
	 * 
	 * @return the execution state
	 */
	public NonInterferenceValue getExecutionState() {
		if (guards.function == null || guards.function.isEmpty())
			// we return LH since that is the lowest non-error state
			return NonInterferenceValue.LOW_HIGH;

		// we start at LH since that is the lowest non-error state
		try {
			NonInterferenceValue res = NonInterferenceValue.LOW_HIGH;
			for (Entry<ProgramPoint, NonInterferenceValue> guard : guards)
				res = res.lub(guard.getValue());
			return res;
		} catch (SemanticException e) {
			return lattice.bottom();
		}
	}

	@Override
	public boolean lessOrEqualAux(
			NonInterferenceEnvironment other)
			throws SemanticException {
		return super.lessOrEqualAux(other) && guards.lessOrEqual(other.guards);
	}

	@Override
	public NonInterferenceEnvironment lubAux(
			NonInterferenceEnvironment other)
			throws SemanticException {
		NonInterferenceEnvironment lub = super.lubAux(other);
		return new NonInterferenceEnvironment(lub.lattice, lub.function, guards.lub(other.guards));
	}

	@Override
	public NonInterferenceEnvironment glbAux(
			NonInterferenceEnvironment other)
			throws SemanticException {
		NonInterferenceEnvironment glb = super.glbAux(other);
		return new NonInterferenceEnvironment(glb.lattice, glb.function, guards.glb(other.guards));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(guards);
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
		NonInterferenceEnvironment other = (NonInterferenceEnvironment) obj;
		return Objects.equals(guards, other.guards);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom() || isTop())
			return super.representation();

		return new ObjectRepresentation(
				Map.of("map", super.representation(), "state", getExecutionState().representation()));
	}

	@Override
	public NonInterferenceEnvironment store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		if (isTop() || isBottom() || function == null || !function.containsKey(source))
			return this;
		return putState(target, getState(source));
	}

}
