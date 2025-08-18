package it.unive.lisa.util.frontend;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Utility class for frontends that can be used to track breaks/continues and
 * connect them to the appropriate targets in the control flow graph when a
 * control flow structure is parsed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ControlFlowTracker {

	private final List<Pair<Statement, String>> modifiers;

	/**
	 * Builds a new control flow tracker.
	 */
	public ControlFlowTracker() {
		this.modifiers = new LinkedList<>();
	}

	/**
	 * Adds a new modifier that modifies the control flow of the program.
	 * 
	 * @param modifier the statement that modifies the control flow
	 */
	public void addModifier(
			Statement modifier) {
		modifiers.add(Pair.of(modifier, null));
	}

	/**
	 * Adds a new modifier with the given label.
	 * 
	 * @param modifier the statement that modifies the control flow
	 * @param label    the label of the modifier, if any
	 */
	public void addModifier(
			Statement modifier,
			String label) {
		modifiers.add(Pair.of(modifier, label));
	}

	/**
	 * Yields the modifiers that are currently active and awaiting connection to
	 * their targets.
	 * 
	 * @return the modifiers
	 */
	public List<Pair<Statement, String>> getModifiers() {
		return modifiers;
	}

	/**
	 * Ends the control flow of a given structure, connecting all (possibly
	 * labelled) modifiers to the appropriate target statements.
	 * 
	 * @param list                the list of statements in the control flow
	 *                                structure
	 * @param condition           the condition of the control flow structure
	 * @param targetForBreaking   the target statement to which the control flow
	 *                                should be redirected when a break is
	 *                                encountered
	 * @param targetForContinuing the target statement to which the control flow
	 *                                should be redirected when a continue is
	 *                                encountered
	 * @param label               the label of the control structure, if any; if
	 *                                {@code null}, all label-less modifiers are
	 *                                considered
	 * 
	 * @throws IllegalStateException if a modifier is not supported
	 */
	public void endControlFlowOf(
			NodeList<CFG, Statement, Edge> list,
			Statement condition,
			Statement targetForBreaking,
			Statement targetForContinuing,
			String label) {
		Iterator<Pair<Statement, String>> it = modifiers.iterator();
		while (it.hasNext()) {
			Pair<Statement, String> modifier = it.next();
			if (modifier.getRight() == null || modifier.getRight().equals(label)) {
				list.getOutgoingEdges(modifier.getLeft()).forEach(list::removeEdge);
				if (targetForBreaking != null && modifier.getLeft().breaksControlFlow())
					list.addEdge(new SequentialEdge(modifier.getLeft(), targetForBreaking));
				else if (targetForContinuing != null && modifier.getLeft().continuesControlFlow())
					list.addEdge(new SequentialEdge(modifier.getLeft(), targetForContinuing));
				else
					throw new IllegalStateException(
						"Statement " + modifier.getLeft() + " not supported at " + condition.getLocation());
				it.remove();
			}
		}
	}
}
