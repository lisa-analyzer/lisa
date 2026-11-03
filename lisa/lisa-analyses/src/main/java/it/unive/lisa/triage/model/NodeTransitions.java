package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The symbolic transitions starting at a node of a cfg, as extracted by a
 * {@link TransitionExtractor}. All transitions start from the values that
 * identifiers have before the node. They are:
 * <ul>
 * <li>one transition for each feasible outgoing edge of the node (see
 * {@link EdgeTransition});</li>
 * <li>the transition to the end of the cfg, if the node is an exit point and
 * the execution can terminate normally after it;</li>
 * <li>the transitions to the errors raised by the node (or by its
 * sub-expressions), that is, the error continuations of its post-state whose
 * thrower is the node (note that these errors might be caught afterwards,
 * through the error edges leaving the node);</li>
 * <li>the transitions from the beginning of the node to the end of each of its
 * sub-expressions (and of the node itself), that allow conditions to be
 * evaluated in the middle of the node.</li>
 * </ul>
 * Executions that halt are not represented, since they do not reach any other
 * program point.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NodeTransitions {

	private final ModelLocation location;

	private final List<EdgeTransition> outgoing;

	private final SymbolicStore exit;

	private final Map<Error, SymbolicStore> errors;

	private final Map<Statement, SymbolicStore> partial;

	/**
	 * Builds the transitions.
	 *
	 * @param location the location of the node
	 * @param outgoing the transitions through the outgoing edges
	 * @param exit     the transition to the end of the cfg, or {@code null}
	 * @param errors   the transitions to the errors raised by the node
	 * @param partial  the transitions to the end of each sub-expression
	 */
	public NodeTransitions(
			ModelLocation location,
			List<EdgeTransition> outgoing,
			SymbolicStore exit,
			Map<Error, SymbolicStore> errors,
			Map<Statement, SymbolicStore> partial) {
		this.location = location;
		this.outgoing = Collections.unmodifiableList(outgoing);
		this.exit = exit;
		this.errors = Collections.unmodifiableMap(errors);
		this.partial = Collections.unmodifiableMap(partial);
	}

	/**
	 * Yields the location of the node.
	 *
	 * @return the location
	 */
	public ModelLocation getLocation() {
		return location;
	}

	/**
	 * Yields the node.
	 *
	 * @return the node
	 */
	public Statement getNode() {
		return location.getNode();
	}

	/**
	 * Yields the transitions through the feasible outgoing edges of the node.
	 *
	 * @return the transitions
	 */
	public List<EdgeTransition> getOutgoing() {
		return outgoing;
	}

	/**
	 * Yields the transition to the end of the cfg, if the node is an exit point
	 * and the execution can terminate normally after it.
	 *
	 * @return the transition, or {@code null}
	 */
	public SymbolicStore getExit() {
		return exit;
	}

	/**
	 * Yields the transitions to the errors raised by the node or by its
	 * sub-expressions, as found in the post-state of the node (they might be
	 * caught afterwards, through the error edges leaving the node).
	 *
	 * @return the transitions, for each error
	 */
	public Map<Error, SymbolicStore> getErrors() {
		return errors;
	}

	/**
	 * Yields the transition from the beginning of the node to the end of the
	 * given sub-expression (or of the node itself).
	 *
	 * @param st the sub-expression, or the node
	 *
	 * @return the transition, or {@code null} if the sub-expression is never
	 *             evaluated
	 */
	public SymbolicStore getPartial(
			Statement st) {
		return partial.get(st);
	}

	@Override
	public String toString() {
		return location + ": " + outgoing + (exit == null ? "" : " exit " + exit)
				+ (errors.isEmpty() ? "" : " errors " + errors);
	}
}
