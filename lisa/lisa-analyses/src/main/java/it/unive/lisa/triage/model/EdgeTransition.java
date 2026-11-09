package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The symbolic transition from a location of an {@link AlarmModel} to one of
 * its successors. When the transition corresponds to an edge of a cfg, this is
 * the semantics of the source node, followed by the traversal of the edge
 * connecting it to its successor (e.g., assuming the condition of a conditional
 * edge, or catching an error). A transition can also be the composition of a
 * sequence of transitions (see {@link #then(EdgeTransition, long)}), passing
 * through intermediate locations.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class EdgeTransition {

	private final ModelLocation source;

	private final ModelLocation destination;

	private final Edge edge;

	private final SymbolicStore store;

	private final Statement thrower;

	/**
	 * The transitions this one is the composition of, in order (only this
	 * transition, if it is not a composition).
	 */
	private final List<EdgeTransition> components;

	/**
	 * The index of the component where each fresh value of the components
	 * occurs, computed when first needed.
	 */
	private Map<FreshValue, Integer> occurrences;

	/**
	 * Builds the transition.
	 *
	 * @param source      the location where the transition starts
	 * @param destination the location where the transition ends
	 * @param edge        the traversed edge (can be {@code null} if the
	 *                        transition does not correspond to an edge of a
	 *                        cfg)
	 * @param store       the symbolic transition
	 */
	public EdgeTransition(
			ModelLocation source,
			ModelLocation destination,
			Edge edge,
			SymbolicStore store) {
		this(source, destination, edge, store, null);
	}

	/**
	 * Builds the transition.
	 *
	 * @param source      the location where the transition starts
	 * @param destination the location where the transition ends
	 * @param edge        the traversed edge (can be {@code null} if the
	 *                        transition does not correspond to an edge of a
	 *                        cfg)
	 * @param store       the symbolic transition
	 * @param thrower     the statement raising the error that the transition
	 *                        moves to, if it leads to a raised error (can be
	 *                        {@code null})
	 */
	public EdgeTransition(
			ModelLocation source,
			ModelLocation destination,
			Edge edge,
			SymbolicStore store,
			Statement thrower) {
		this.source = source;
		this.destination = destination;
		this.edge = edge;
		this.store = store;
		this.thrower = thrower;
		this.components = Collections.singletonList(this);
	}

	private EdgeTransition(
			EdgeTransition first,
			EdgeTransition second,
			SymbolicStore store) {
		this.source = first.source;
		this.destination = second.destination;
		this.edge = second.edge;
		this.store = store;
		this.thrower = second.thrower;
		List<EdgeTransition> all = new ArrayList<>(first.components);
		all.addAll(second.components);
		this.components = Collections.unmodifiableList(all);
	}

	/**
	 * Yields the composition of this transition with the given one, starting
	 * where this transition ends: the composed transition goes from the source
	 * of this transition to the destination of the given one, and its store is
	 * the sequential composition of the stores (see
	 * {@link SymbolicStore#then(SymbolicStore, long)}). Yields {@code null} if
	 * the stores cannot be composed.
	 *
	 * @param next  the transition following this one
	 * @param bound the maximum size of the conditions and values of the
	 *                  composed cases
	 *
	 * @return the composition, or {@code null}
	 */
	public EdgeTransition then(
			EdgeTransition next,
			long bound) {
		SymbolicStore composed = store.then(next.store, bound);
		return composed == null ? null : new EdgeTransition(this, next, composed);
	}

	/**
	 * Yields the transitions this one is the composition of, in order (a list
	 * containing only this transition, if it is not a composition).
	 *
	 * @return the components
	 */
	public List<EdgeTransition> getComponents() {
		return components;
	}

	/**
	 * Yields the locations this transition passes through, between its source
	 * and its destination (empty if it is not a composition).
	 *
	 * @return the intermediate locations, in order
	 */
	public List<ModelLocation> getIntermediateLocations() {
		List<ModelLocation> result = new ArrayList<>();
		for (int i = 1; i < components.size(); i++)
			result.add(components.get(i).source);
		return result;
	}

	/**
	 * Yields the component of this transition producing the given fresh value
	 * (e.g., an input), that is, the first one whose cases mention it, or this
	 * transition itself if no component does (e.g., if the value has been
	 * introduced by the composition). Fresh values produced by earlier
	 * components are produced before the ones of later components.
	 *
	 * @param value the fresh value
	 *
	 * @return the component
	 */
	public synchronized EdgeTransition componentOf(
			FreshValue value) {
		if (components.size() == 1)
			return this;
		if (occurrences == null) {
			occurrences = new HashMap<>();
			for (int i = components.size() - 1; i >= 0; i--)
				for (SymbolicCase c : components.get(i).store.getCases()) {
					for (SymbolicExpression cond : c.getPathCondition())
						collect(cond, i, occurrences);
					for (SymbolicExpression v : c.getUpdates().values())
						collect(v, i, occurrences);
				}
		}
		Integer index = occurrences.get(value);
		return index == null ? this : components.get(index);
	}

	/**
	 * Yields the position of the component producing the given fresh value (see
	 * {@link #componentOf(FreshValue)}): fresh values produced by earlier
	 * components have smaller positions, while the ones introduced by the
	 * composition come last.
	 *
	 * @param value the fresh value
	 *
	 * @return the position
	 */
	public int positionOf(
			FreshValue value) {
		EdgeTransition component = componentOf(value);
		return component == this ? components.size() : components.indexOf(component);
	}

	private static void collect(
			SymbolicExpression e,
			int index,
			Map<FreshValue, Integer> occurrences) {
		if (e instanceof FreshValue)
			// components are visited from the last one
			occurrences.put((FreshValue) e, index);
		else if (e instanceof UnaryExpression)
			collect(((UnaryExpression) e).getExpression(), index, occurrences);
		else if (e instanceof BinaryExpression) {
			collect(((BinaryExpression) e).getLeft(), index, occurrences);
			collect(((BinaryExpression) e).getRight(), index, occurrences);
		} else if (e instanceof TernaryExpression) {
			collect(((TernaryExpression) e).getLeft(), index, occurrences);
			collect(((TernaryExpression) e).getMiddle(), index, occurrences);
			collect(((TernaryExpression) e).getRight(), index, occurrences);
		}
	}

	/**
	 * Yields the location where the transition starts.
	 *
	 * @return the source location
	 */
	public ModelLocation getSource() {
		return source;
	}

	/**
	 * Yields the location where the transition ends.
	 *
	 * @return the destination location
	 */
	public ModelLocation getDestination() {
		return destination;
	}

	/**
	 * Yields the traversed edge, or {@code null} if the transition does not
	 * correspond to an edge of a cfg.
	 *
	 * @return the edge
	 */
	public Edge getEdge() {
		return edge;
	}

	/**
	 * Yields the symbolic transition.
	 *
	 * @return the transition
	 */
	public SymbolicStore getStore() {
		return store;
	}

	/**
	 * Yields the statement raising the error that this transition moves to, if
	 * it leads to a raised error and the statement is known.
	 *
	 * @return the statement, or {@code null}
	 */
	public Statement getThrower() {
		return thrower;
	}

	@Override
	public String toString() {
		return (edge != null ? edge.toString() : source + " -> " + destination) + ": " + store;
	}
}
