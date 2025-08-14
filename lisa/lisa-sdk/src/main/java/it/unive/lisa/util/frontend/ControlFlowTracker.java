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

public class ControlFlowTracker {

	private final List<Pair<Statement, String>> modifiers;

	public ControlFlowTracker() {
		this.modifiers = new LinkedList<>();
	}

	public void addModifier(
			Statement statement) {
		modifiers.add(Pair.of(statement, null));
	}

	public void addModifier(
			Statement statement,
			String label) {
		modifiers.add(Pair.of(statement, label));
	}

	public List<Pair<Statement, String>> getModifiers() {
		return modifiers;
	}

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
