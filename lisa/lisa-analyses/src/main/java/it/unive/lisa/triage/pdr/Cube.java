package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A cube of property-directed reachability: a conjunction of boolean
 * symbolic expressions (literals) over the identifiers of an
 * {@link it.unive.lisa.triage.model.AlarmModel}, possibly containing fresh
 * values (existentially quantified). Cubes describe sets of states that must
 * be blocked; once blocked at a location, the cube yields the lemma
 * {@link #negate()}. Generalization drops literals through
 * {@link #without(SymbolicExpression)}. The empty cube is {@code true}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Cube {

	private final Set<SymbolicExpression> literals;

	/**
	 * Builds the cube. Conjunctions are split into their conjuncts, and
	 * {@code true} literals are removed.
	 *
	 * @param literals the literals
	 */
	public Cube(
			Collection<SymbolicExpression> literals) {
		this.literals = Collections.unmodifiableSet(Projection.conjuncts(literals));
	}

	/**
	 * Yields the literals of this cube.
	 *
	 * @return the literals
	 */
	public Set<SymbolicExpression> getLiterals() {
		return literals;
	}

	/**
	 * Yields the cube obtained by dropping the given literal, that represents
	 * a superset of the states of this one.
	 *
	 * @param literal the literal
	 *
	 * @return the cube
	 */
	public Cube without(
			SymbolicExpression literal) {
		Set<SymbolicExpression> result = new LinkedHashSet<>(literals);
		result.remove(literal);
		return new Cube(result);
	}

	/**
	 * Yields the conjunction of the literals of this cube, as a single
	 * expression.
	 *
	 * @return the conjunction
	 */
	public SymbolicExpression toExpression() {
		SymbolicExpression result = null;
		for (SymbolicExpression lit : literals)
			result = result == null ? lit
					: new BinaryExpression(Untyped.INSTANCE, result, lit, LogicalAnd.INSTANCE,
							SyntheticLocation.INSTANCE);
		return result == null ? new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE) : result;
	}

	/**
	 * Yields the formula of this cube, over the values of identifiers at the
	 * given step of the given unrolling. Fresh values are encoded through
	 * {@link Unrolling#condition(SymbolicExpression, int, String)} with the
	 * given tag, and the exactness of the encoding is recorded under key
	 * {@code "cond@" + step + tag}.
	 *
	 * @param unrolling the unrolling
	 * @param step      the step
	 * @param tag       the tag for fresh values, starting with {@code #}
	 *
	 * @return the formula
	 *
	 * @throws SemanticException if the cube cannot be encoded
	 */
	public Term at(
			Unrolling unrolling,
			int step,
			String tag)
			throws SemanticException {
		return unrolling.condition(toExpression(), step, tag);
	}

	/**
	 * Yields the lemma excluding the states of this cube.
	 *
	 * @return the lemma
	 */
	public Lemma negate() {
		return new Blocked(this);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(literals);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		return Objects.equals(literals, ((Cube) obj).literals);
	}

	@Override
	public String toString() {
		return literals.isEmpty() ? "true" : literals.toString();
	}

	/**
	 * The lemma excluding the states of a cube. Fresh values of the cube are
	 * encoded with tag {@code #lemma}.
	 */
	public static final class Blocked
			implements
			Lemma {

		private final Cube cube;

		private Blocked(
				Cube cube) {
			this.cube = cube;
		}

		/**
		 * Yields the cube excluded by this lemma.
		 *
		 * @return the cube
		 */
		public Cube getCube() {
			return cube;
		}

		@Override
		public Term at(
				Unrolling unrolling,
				int step)
				throws SemanticException {
			return Term.not(cube.at(unrolling, step, "#lemma"));
		}

		@Override
		public int hashCode() {
			return Objects.hash(cube);
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			return Objects.equals(cube, ((Blocked) obj).cube);
		}

		@Override
		public String toString() {
			return "!" + cube;
		}
	}
}
