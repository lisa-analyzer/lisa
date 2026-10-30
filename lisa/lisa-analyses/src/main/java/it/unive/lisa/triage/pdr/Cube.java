package it.unive.lisa.triage.pdr;

import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.encoding.ValueSort;
import it.unive.lisa.triage.smt.SExpression;
import it.unive.lisa.triage.smt.SmtException;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Term;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A cube of property-directed reachability: a conjunction of literals, one
 * per identifier, each being either {@code id = value}, where the value is a
 * term in SMT-LIB syntax as printed by a solver in a model, or (for
 * identifiers encoded as {@link ValueSort values}) a test on the constructor
 * of {@code id}. Cubes describe (sets of) states that must be blocked; once
 * blocked at a location, the cube yields the lemma {@link #negate()}.
 * Generalization drops literals through {@link #without(Identifier)}, and
 * weakens equalities to constructor tests through
 * {@link #weaken(Identifier)}. The empty cube is {@code true}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Cube {

	private final Map<Identifier, String> literals;

	private final Map<Identifier, String> constructors;

	/**
	 * Builds a cube made of equalities.
	 *
	 * @param literals the value of each identifier, in SMT-LIB syntax
	 */
	public Cube(
			Map<Identifier, String> literals) {
		this(literals, Map.of());
	}

	/**
	 * Builds the cube.
	 *
	 * @param literals     the value of each identifier constrained by an
	 *                         equality, in SMT-LIB syntax
	 * @param constructors the constructor of each identifier constrained by a
	 *                         constructor test (that must not be constrained
	 *                         by an equality)
	 */
	public Cube(
			Map<Identifier, String> literals,
			Map<Identifier, String> constructors) {
		this.literals = Collections.unmodifiableMap(new LinkedHashMap<>(literals));
		this.constructors = Collections.unmodifiableMap(new LinkedHashMap<>(constructors));
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
	 * Yields the equalities of this cube.
	 *
	 * @return the value of each identifier, in SMT-LIB syntax
	 */
	public Map<Identifier, String> getLiterals() {
		return literals;
	}

	/**
	 * Yields the constructor tests of this cube.
	 *
	 * @return the constructor of each identifier
	 */
	public Map<Identifier, String> getConstructors() {
		return constructors;
	}

	/**
	 * Yields the identifiers constrained by this cube.
	 *
	 * @return the identifiers
	 */
	public Set<Identifier> getIdentifiers() {
		Set<Identifier> result = new LinkedHashSet<>(literals.keySet());
		result.addAll(constructors.keySet());
		return result;
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
		Map<Identifier, String> lits = new LinkedHashMap<>(literals);
		lits.remove(id);
		Map<Identifier, String> cons = new LinkedHashMap<>(constructors);
		cons.remove(id);
		return new Cube(lits, cons);
	}

	/**
	 * Yields the cube obtained by replacing the equality of the given
	 * identifier with a test on the constructor of its value, that represents
	 * a superset of the states of this one. This is possible only if the
	 * value is a non-nullary constructor application of {@link ValueSort}
	 * (e.g., {@code (Value.String "a")} is weakened to the test for
	 * {@code Value.String}).
	 *
	 * @param id the identifier
	 *
	 * @return the cube, or {@code null} if the equality cannot be weakened
	 */
	public Cube weaken(
			Identifier id) {
		String value = literals.get(id);
		if (value == null)
			return null;
		SExpression parsed;
		try {
			parsed = SExpression.parse(value);
		} catch (SmtException e) {
			return null;
		}
		if (parsed.isAtom() || parsed.getChildren().isEmpty() || !parsed.getChildren().get(0).isAtom())
			return null;
		String head = parsed.getChildren().get(0).getAtom();
		if (!ValueSort.DATATYPE.getConstructors().containsKey(head))
			return null;
		Map<Identifier, String> lits = new LinkedHashMap<>(literals);
		lits.remove(id);
		Map<Identifier, String> cons = new LinkedHashMap<>(constructors);
		cons.put(id, head);
		return new Cube(lits, cons);
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
		for (Map.Entry<Identifier, String> e : constructors.entrySet())
			conj.add(ValueSort.DATATYPE.test(e.getValue(), unrolling.var(e.getKey(), step)));
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
		return Objects.hash(literals, constructors);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Cube other = (Cube) obj;
		return Objects.equals(literals, other.literals) && Objects.equals(constructors, other.constructors);
	}

	@Override
	public String toString() {
		if (literals.isEmpty() && constructors.isEmpty())
			return "true";
		StringBuilder sb = new StringBuilder("{");
		for (Map.Entry<Identifier, String> e : literals.entrySet())
			sb.append(sb.length() > 1 ? ", " : "").append(e.getKey()).append("=").append(e.getValue());
		for (Map.Entry<Identifier, String> e : constructors.entrySet())
			sb.append(sb.length() > 1 ? ", " : "").append(e.getKey()).append(" is ").append(e.getValue());
		return sb.append("}").toString();
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
