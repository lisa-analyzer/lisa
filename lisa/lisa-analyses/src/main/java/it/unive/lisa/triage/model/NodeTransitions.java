package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.ArrayList;
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
 * When callees are expanded, the outgoing transitions also include the ones
 * entering the frames of the callees, and the ones returning from an exit
 * point of a callee to the location where the caller resumes (these do not
 * correspond to edges of a cfg). The calls to cfgs reached while replaying the
 * node are also recorded (see {@link #getCalls()}).
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

	private final List<ReplayedCall> calls;

	private final List<EscapingError> escaping;

	/**
	 * An error raised inside an expanded callee, that is not caught by any of
	 * the frames between the callee and the root frame of the model: it leaves
	 * the root frame, as if raised by a node of the root frame.
	 */
	public static final class EscapingError {

		private final Statement node;

		private final Error error;

		private final SymbolicStore store;

		/**
		 * Builds the error.
		 *
		 * @param node  the node of the root frame raising the error (through
		 *                  its calls)
		 * @param error the error, whose thrower is the node or one of its
		 *                  sub-expressions
		 * @param store the transition from the location where the error is
		 *                  raised to the error, in the scope of the root frame
		 */
		public EscapingError(
				Statement node,
				Error error,
				SymbolicStore store) {
			this.node = node;
			this.error = error;
			this.store = store;
		}

		/**
		 * Yields the node of the root frame raising the error.
		 *
		 * @return the node
		 */
		public Statement getNode() {
			return node;
		}

		/**
		 * Yields the error.
		 *
		 * @return the error
		 */
		public Error getError() {
			return error;
		}

		/**
		 * Yields the transition to the error.
		 *
		 * @return the transition
		 */
		public SymbolicStore getStore() {
			return store;
		}

		@Override
		public String toString() {
			return error + " raised by " + node + ": " + store;
		}
	}

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
		this(location, outgoing, exit, errors, partial, Collections.emptyList());
	}

	/**
	 * Builds the transitions.
	 *
	 * @param location the location of the node
	 * @param outgoing the transitions through the outgoing edges, and the ones
	 *                     entering and returning from callees
	 * @param exit     the transition to the end of the cfg, or {@code null}
	 * @param errors   the transitions to the errors raised by the node
	 * @param partial  the transitions to the end of each sub-expression
	 * @param calls    the calls to cfgs reached while replaying the node, in
	 *                     evaluation order
	 */
	public NodeTransitions(
			ModelLocation location,
			List<EdgeTransition> outgoing,
			SymbolicStore exit,
			Map<Error, SymbolicStore> errors,
			Map<Statement, SymbolicStore> partial,
			List<ReplayedCall> calls) {
		this(location, outgoing, exit, errors, partial, calls, Collections.emptyList());
	}

	/**
	 * Builds the transitions.
	 *
	 * @param location the location of the node
	 * @param outgoing the transitions through the outgoing edges, and the ones
	 *                     entering and returning from callees, and the ones
	 *                     moving errors raised in a callee to the handlers
	 *                     of its callers
	 * @param exit     the transition to the end of the cfg, or {@code null}
	 * @param errors   the transitions to the errors raised by the node
	 * @param partial  the transitions to the end of each sub-expression
	 * @param calls    the calls to cfgs reached while replaying the node, in
	 *                     evaluation order
	 * @param escaping the errors raised by the node, in a callee frame, that
	 *                     leave the root frame of the model
	 */
	public NodeTransitions(
			ModelLocation location,
			List<EdgeTransition> outgoing,
			SymbolicStore exit,
			Map<Error, SymbolicStore> errors,
			Map<Statement, SymbolicStore> partial,
			List<ReplayedCall> calls,
			List<EscapingError> escaping) {
		this.location = location;
		this.outgoing = Collections.unmodifiableList(outgoing);
		this.exit = exit;
		this.errors = Collections.unmodifiableMap(errors);
		this.partial = Collections.unmodifiableMap(partial);
		this.calls = Collections.unmodifiableList(calls);
		this.escaping = Collections.unmodifiableList(escaping);
	}

	/**
	 * Yields the errors raised by the node, in a callee frame, that are not
	 * caught by any frame and leave the root frame of the model.
	 *
	 * @return the errors
	 */
	public List<EscapingError> getEscaping() {
		return escaping;
	}

	/**
	 * Yields a copy of these transitions, with the given additional outgoing
	 * transitions.
	 *
	 * @param more the additional transitions
	 *
	 * @return the copy
	 */
	public NodeTransitions withOutgoing(
			List<EdgeTransition> more) {
		if (more.isEmpty())
			return this;
		List<EdgeTransition> all = new ArrayList<>(outgoing);
		all.addAll(more);
		return new NodeTransitions(location, all, exit, errors, partial, calls, escaping);
	}

	/**
	 * Yields the calls to cfgs reached while replaying the node from this
	 * location, in evaluation order.
	 *
	 * @return the calls
	 */
	public List<ReplayedCall> getCalls() {
		return calls;
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
	 * Yields the transitions through the feasible outgoing edges of the node,
	 * and the ones entering or returning from callees.
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
