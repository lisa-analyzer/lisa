package it.unive.lisa.triage.pdr;

import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.ModelLocation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A proof obligation of property-directed reachability: the states of a cube at
 * a location must be shown unreachable within a given number of steps (the
 * level), or a predecessor must be found. Each obligation, except the one at
 * the bad location, records the obligation it is a predecessor of (its parent)
 * and the transition leading there (or no transition, if the obligation
 * restricts the cube of its parent to initial states), together with the values
 * of the inputs taken by that transition and the exactness of its encoding (as
 * the reasons why it is not exact, if any), so
 * that a chain of obligations reaching the initial states yields a
 * counterexample.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Obligation
		implements
		Comparable<Obligation> {

	private final ModelLocation location;

	private final Cube cube;

	private final int level;

	private final Obligation parent;

	private final EdgeTransition edge;

	private final Map<InputValue, String> inputs;

	private final Set<String> imprecisions;

	private final Map<Identifier, String> point;

	private final long order;

	/**
	 * Builds the obligation.
	 *
	 * @param location the location
	 * @param cube     the cube
	 * @param level    the level
	 * @param parent   the obligation this is a predecessor of, or {@code null}
	 *                     for the bad location
	 * @param edge     the transition from {@code location} to the location of
	 *                     the parent (for the bad location, {@code null})
	 * @param inputs   the values of the inputs taken by the transition (for the
	 *                     bad location, by the bad case)
	 * @param imprecisions the reasons why the transition (or the bad case) is
	 *                         not encoded exactly (empty if it is), as
	 *                         {@link it.unive.lisa.triage.bmc.Unrolling#getImprecisions(String, it.unive.lisa.triage.smt.SmtSolver)}
	 *                         yields them
	 * @param point    the values of the identifiers in the model that produced
	 *                     this obligation (a state of the cube)
	 * @param order    the creation order of the obligation, used to break ties
	 *                     in the queue
	 */
	public Obligation(
			ModelLocation location,
			Cube cube,
			int level,
			Obligation parent,
			EdgeTransition edge,
			Map<InputValue, String> inputs,
			Set<String> imprecisions,
			Map<Identifier, String> point,
			long order) {
		this.location = location;
		this.cube = cube;
		this.level = level;
		this.parent = parent;
		this.edge = edge;
		this.inputs = Collections.unmodifiableMap(inputs);
		this.imprecisions = Collections.unmodifiableSet(new LinkedHashSet<>(imprecisions));
		this.point = Collections.unmodifiableMap(point);
		this.order = order;
	}

	/**
	 * Yields the location.
	 *
	 * @return the location
	 */
	public ModelLocation getLocation() {
		return location;
	}

	/**
	 * Yields the cube.
	 *
	 * @return the cube
	 */
	public Cube getCube() {
		return cube;
	}

	/**
	 * Yields the level.
	 *
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * Yields the obligation this is a predecessor of.
	 *
	 * @return the parent, or {@code null}
	 */
	public Obligation getParent() {
		return parent;
	}

	/**
	 * Yields the transition leading to the location of the parent.
	 *
	 * @return the transition, or {@code null}
	 */
	public EdgeTransition getEdge() {
		return edge;
	}

	/**
	 * Yields the values of the inputs taken by the transition leading to the
	 * parent (or, for the bad location, by the bad case).
	 *
	 * @return the values, in SMT-LIB syntax
	 */
	public Map<InputValue, String> getInputs() {
		return inputs;
	}

	/**
	 * Yields whether or not the transition leading to the parent (or, for the
	 * bad location, the bad case) is encoded exactly.
	 *
	 * @return whether or not the encoding is exact
	 */
	public boolean isExact() {
		return imprecisions.isEmpty();
	}

	/**
	 * Yields the reasons why the transition leading to the parent (or, for the
	 * bad location, the bad case) is not encoded exactly.
	 *
	 * @return the reasons (empty if the encoding is exact)
	 */
	public Set<String> getImprecisions() {
		return imprecisions;
	}

	/**
	 * Yields the values of the identifiers in the model that produced this
	 * obligation: a state of the cube, that (for obligations at level
	 * {@code 0}) is an initial state.
	 *
	 * @return the values, in SMT-LIB syntax
	 */
	public Map<Identifier, String> getPoint() {
		return point;
	}

	/**
	 * Obligations with lower levels come first; among the ones with the same
	 * level, the most recent comes first (depth-first).
	 */
	@Override
	public int compareTo(
			Obligation o) {
		int cmp = Integer.compare(level, o.level);
		return cmp != 0 ? cmp : Long.compare(o.order, order);
	}

	@Override
	public String toString() {
		return "(" + location + ", " + cube + ", " + level + ")";
	}
}
