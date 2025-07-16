package it.unive.lisa.analysis.nonInterference;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
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
 * instances of {@link NI} as values, and uses {@link Identifier}s as keys.
 * Instances of this class also contain a reference to the non interference
 * levels of all the guards protecting the program point where this object was
 * created. These are used to determine the execution state, which can be
 * retrieved via {@link #getExecutionState()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NIEnv
		extends
		FunctionalLattice<NIEnv, Identifier, NIEnv.NI>
		implements
		ValueLattice<NIEnv> {

	/**
	 * The non interference levels of the guards protecting the program point
	 * where this object was created.
	 */
	public final GenericMapLattice<ProgramPoint, NI> guards;

	/**
	 * Builds a new instance of the non interference environment.
	 */
	public NIEnv() {
		super(NI.HIGH_LOW);
		this.guards = new GenericMapLattice<ProgramPoint, NI>(NI.LOW_HIGH).top();
	}

	/**
	 * Builds a new instance of the non interference environment, with the given
	 * {@link NI} as the lattice, the given map of {@link Identifier}s to
	 * {@link NI} as the function, and the given guards.
	 * 
	 * @param domain   the lattice to use for the non interference levels
	 * @param function the map of {@link Identifier}s to {@link NI} to use as
	 *                     the function
	 * @param guards   the non interference levels of the guards protecting the
	 *                     program point where this object was created
	 */
	NIEnv(
			NI domain,
			Map<Identifier, NI> function,
			GenericMapLattice<ProgramPoint, NI> guards) {
		super(domain, function);
		this.guards = guards;
	}

	@Override
	public NIEnv pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		NIEnv result = liftIdentifiers(id -> {
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
	public NIEnv popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		AtomicReference<SemanticException> holder = new AtomicReference<>();

		NIEnv result = liftIdentifiers(id -> {
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

	private NIEnv liftIdentifiers(
			UnaryOperator<Identifier> lifter)
			throws SemanticException {
		if (isBottom() || isTop())
			return this;

		Map<Identifier, NI> function = mkNewFunction(null, false);
		for (Identifier id : getKeys()) {
			Identifier lifted = lifter.apply(id);
			if (lifted != null)
				if (!function.containsKey(lifted))
					function.put(lifted, getState(id));
				else
					function.put(lifted, getState(id).lub(function.get(lifted)));

		}

		return new NIEnv(lattice, function, guards);
	}

	@Override
	public NIEnv forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<Identifier, NI> result = mkNewFunction(function, false);
		result.remove(id);

		return new NIEnv(lattice, result, guards);
	}

	@Override
	public NIEnv forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<Identifier, NI> result = mkNewFunction(function, false);
		for (Identifier id : ids)
			result.remove(id);

		return new NIEnv(lattice, result, guards);
	}

	@Override
	public NIEnv forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<Identifier, NI> result = mkNewFunction(function, false);
		Set<Identifier> keys = result.keySet().stream().filter(test::test).collect(Collectors.toSet());
		keys.forEach(result::remove);

		return new NIEnv(lattice, result, guards);
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
	public NI stateOfUnknown(
			Identifier key) {
		return lattice.unknownValue(key);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return getKeys().contains(id);
	}

	@Override
	public NIEnv top() {
		return isTop() ? this : new NIEnv(lattice.top(), null, guards.top());
	}

	@Override
	public boolean isTop() {
		return super.isTop() && guards.isTop();
	}

	@Override
	public NIEnv bottom() {
		return isBottom() ? this : new NIEnv(lattice.bottom(), null, guards.bottom());
	}

	@Override
	public boolean isBottom() {
		return super.isBottom() && guards.isBottom();
	}

	@Override
	public NIEnv mk(
			NI lattice,
			Map<Identifier, NI> function) {
		return new NIEnv(lattice, function, guards);
	}

	/**
	 * Yields the state in which the execution is. This corresponds to the lub
	 * of the {@link NI} instances of all the guards protecting the program
	 * point where this object was created.
	 * 
	 * @return the execution state
	 */
	public NI getExecutionState() {
		if (guards.function == null || guards.function.isEmpty())
			// we return LH since that is the lowest non-error state
			return NI.LOW_HIGH;

		// we start at LH since that is the lowest non-error state
		try {
			NI res = NI.LOW_HIGH;
			for (Entry<ProgramPoint, NI> guard : guards)
				res = res.lub(guard.getValue());
			return res;
		} catch (SemanticException e) {
			return lattice.bottom();
		}
	}

	@Override
	public boolean lessOrEqualAux(
			NIEnv other)
			throws SemanticException {
		return super.lessOrEqualAux(other) && guards.lessOrEqual(other.guards);
	}

	@Override
	public NIEnv lubAux(
			NIEnv other)
			throws SemanticException {
		NIEnv lub = super.lubAux(other);
		return new NIEnv(lub.lattice, lub.function, guards.lub(other.guards));
	}

	@Override
	public NIEnv glbAux(
			NIEnv other)
			throws SemanticException {
		NIEnv glb = super.glbAux(other);
		return new NIEnv(glb.lattice, glb.function, guards.glb(other.guards));
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
		NIEnv other = (NIEnv) obj;
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
	public NIEnv store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		if (isTop() || isBottom() || function == null || !function.containsKey(source))
			return this;
		return putState(target, getState(source));
	}

	/**
	 * A pair of integrity and confidentiality bits for the
	 * {@link NonInterference} analysis.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class NI
			implements
			BaseLattice<NI> {

		/**
		 * The value to use for bottom non interference levels.
		 */
		public static final byte NI_BOTTOM = 0;

		/**
		 * The value to use for low non interference levels.
		 */
		public static final byte NI_LOW = -1;

		/**
		 * The value to use for high non interference levels.
		 */
		public static final byte NI_HIGH = 1;

		/**
		 * The non interference level for low confidentiality and high
		 * integrity.
		 */
		public static NI LOW_HIGH = new NI(NI_LOW, NI_HIGH);

		/**
		 * The non interference level for low confidentiality and low integrity.
		 */
		public static NI LOW_LOW = new NI(NI_LOW, NI_LOW);

		/**
		 * The non interference level for high confidentiality and high
		 * integrity.
		 */
		public static NI HIGH_HIGH = new NI(NI_HIGH, NI_HIGH);

		/**
		 * The non interference level for high confidentiality and low
		 * integrity, corresponding to the top of the lattice.
		 */
		public static NI HIGH_LOW = new NI(NI_HIGH, NI_LOW);

		/**
		 * The bottom abstract element.
		 */
		public static NI BOTTOM = new NI(NI_BOTTOM, NI_BOTTOM);

		private final byte confidentiality;

		private final byte integrity;

		/**
		 * Builds the abstract value for the given confidentiality and integrity
		 * values. Each of those can be either 0 for bottom
		 * ({@link #NI_BOTTOM}), 1 for low ({@link #NI_LOW}), or 2 for high
		 * ({@link #NI_HIGH}).
		 * 
		 * @param confidentiality the confidentiality value
		 * @param integrity       the integrity value
		 */
		private NI(
				byte confidentiality,
				byte integrity) {
			this.confidentiality = confidentiality;
			this.integrity = integrity;
		}

		@Override
		public NI top() {
			return HIGH_LOW;
		}

		@Override
		public NI bottom() {
			return BOTTOM;
		}

		/**
		 * Yields {@code true} if and only if this instance represents a
		 * {@code high} value for the confidentiality non interference analysis.
		 * 
		 * @return {@code true} if this is a high confidentiality element
		 */
		public boolean isHighConfidentiality() {
			return confidentiality == NI_HIGH;
		}

		/**
		 * Yields {@code true} if and only if this instance represents a
		 * {@code low} value for the confidentiality non interference analysis.
		 * 
		 * @return {@code true} if this is a low confidentiality element
		 */
		public boolean isLowConfidentiality() {
			return confidentiality == NI_LOW;
		}

		/**
		 * Yields {@code true} if and only if this instance represents a
		 * {@code high} value for the integrity non interference analysis.
		 * 
		 * @return {@code true} if this is a high integrity element
		 */
		public boolean isHighIntegrity() {
			return integrity == NI_HIGH;
		}

		/**
		 * Yields {@code true} if and only if this instance represents a
		 * {@code low} value for the integrity non interference analysis.
		 * 
		 * @return {@code true} if this is a low integrity element
		 */
		public boolean isLowIntegrity() {
			return integrity == NI_LOW;
		}

		@Override
		public NI lubAux(
				NI other)
				throws SemanticException {
			// HL
			// | \
			// HH LL
			// | /
			// LH
			// |
			// BB
			byte confidentiality = isHighConfidentiality() || other.isHighConfidentiality() ? NI_HIGH : NI_LOW;
			byte integrity = isLowIntegrity() || other.isLowIntegrity() ? NI_LOW : NI_HIGH;
			if (confidentiality == NI_LOW && integrity == NI_LOW)
				return LOW_LOW;
			if (confidentiality == NI_HIGH && integrity == NI_HIGH)
				return HIGH_HIGH;
			if (confidentiality == NI_HIGH && integrity == NI_LOW)
				return HIGH_LOW;
			// confidentiality == NI_LOW && integrity == NI_HIGH
			return LOW_HIGH;
		}

		@Override
		public boolean lessOrEqualAux(
				NI other)
				throws SemanticException {
			// HL
			// | \
			// HH LL
			// | /
			// LH
			// |
			// BB
			boolean confidentiality = isLowConfidentiality() || this.confidentiality == other.confidentiality;
			boolean integrity = isHighIntegrity() || this.integrity == other.integrity;
			return confidentiality && integrity;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + confidentiality;
			result = prime * result + integrity;
			return result;
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NI other = (NI) obj;
			if (confidentiality != other.confidentiality)
				return false;
			if (integrity != other.integrity)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		@Override
		public StructuredRepresentation representation() {
			if (isBottom())
				return Lattice.bottomRepresentation();
			return new StringRepresentation((isHighConfidentiality() ? "H" : "L") + (isHighIntegrity() ? "H" : "L"));
		}

	}

}
