package it.unive.lisa.triage.pdr;

import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Term;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A cube of property-directed reachability: a conjunction of literals
 * {@code id = value}, where each value is a term in SMT-LIB syntax as printed
 * by a solver in a model. Cubes describe (sets of) states that must be
 * blocked; once blocked at a location, the cube yields the lemma
 * {@link #negate()}. Generalization drops literals through
 * {@link #without(Identifier)}. The empty cube is {@code true}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Cube {

	private final Map<Identifier, String> literals;

	/**
	 * Builds the cube.
	 *
	 * @param literals the value of each identifier, in SMT-LIB syntax
	 */
	public Cube(
			Map<Identifier, String> literals) {
		this.literals = Collections.unmodifiableMap(new LinkedHashMap<>(literals));
	}

	/**
	 * Reads a cube from the current model of the given solver, assigning to
	 * each of the given identifiers its value at the given step of the given
	 * unrolling. The solver must have just answered
	 * {@link it.unive.lisa.triage.smt.SmtResult#SAT}.
	 *
	 * @param unrolling   the unrolling
	 * @param step        the step
	 * @param identifiers the identifiers
	 * @param solver      the solver
	 *
	 * @return the cube
	 */
	public static Cube fromModel(
			Unrolling unrolling,
			int step,
			Collection<Identifier> identifiers,
			SmtSolver solver) {
		Map<Term, Identifier> terms = new LinkedHashMap<>();
		for (Identifier id : identifiers)
			terms.put(unrolling.var(id, step), id);
		Map<Identifier, String> literals = new LinkedHashMap<>();
		if (!terms.isEmpty()) {
			Map<Term, String> values = solver.getValues(new ArrayList<>(terms.keySet()));
			for (Map.Entry<Term, Identifier> e : terms.entrySet())
				literals.put(e.getValue(), values.get(e.getKey()));
		}
		return new Cube(literals);
	}

	/**
	 * Yields the literals of this cube.
	 *
	 * @return the value of each identifier, in SMT-LIB syntax
	 */
	public Map<Identifier, String> getLiterals() {
		return literals;
	}

	/**
	 * Yields the cube obtained by dropping the literal of the given
	 * identifier, that represents a superset of the states of this one.
	 *
	 * @param id the identifier
	 *
	 * @return the cube
	 */
	public Cube without(
			Identifier id) {
		Map<Identifier, String> result = new LinkedHashMap<>(literals);
		result.remove(id);
		return new Cube(result);
	}

	/**
	 * Yields the formula of this cube, over the values of identifiers at the
	 * given step of the given unrolling.
	 *
	 * @param unrolling the unrolling
	 * @param step      the step
	 *
	 * @return the formula
	 */
	public Term at(
			Unrolling unrolling,
			int step) {
		List<Term> conj = new ArrayList<>();
		for (Map.Entry<Identifier, String> e : literals.entrySet()) {
			Term var = unrolling.var(e.getKey(), step);
			conj.add(Term.eq(var, Term.value(e.getValue(), var.getSort())));
		}
		return Term.and(conj.toArray(Term[]::new));
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
	 * The lemma excluding the states of a cube.
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
				int step) {
			return Term.not(cube.at(unrolling, step));
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
