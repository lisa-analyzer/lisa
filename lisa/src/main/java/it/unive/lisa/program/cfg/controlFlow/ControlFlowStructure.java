package it.unive.lisa.program.cfg.controlFlow;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;

public abstract class ControlFlowStructure {

	private final Statement condition;

	private final Statement firstFollower;

	public ControlFlowStructure(Statement condition, Statement firstFollower) {
		this.condition = condition;
		this.firstFollower = firstFollower;
	}

	public final Statement getCondition() {
		return condition;
	}

	public final Statement getFirstFollower() {
		return firstFollower;
	}
	
	public abstract boolean contains(Statement st);
	
	public abstract void simplify();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((firstFollower == null) ? 0 : firstFollower.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ControlFlowStructure other = (ControlFlowStructure) obj;
		if (condition == null) {
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		if (firstFollower == null) {
			if (other.firstFollower != null)
				return false;
		} else if (!firstFollower.equals(other.firstFollower))
			return false;
		return true;
	}
	
	public boolean isEqualTo(ControlFlowStructure obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ControlFlowStructure other = (ControlFlowStructure) obj;
		if (condition == null) {
			if (other.condition != null)
				return false;
		} else if (!condition.isEqualTo(other.condition))
			return false;
		if (firstFollower == null) {
			if (other.firstFollower != null)
				return false;
		} else if (!firstFollower.isEqualTo(other.firstFollower))
			return false;
		return true;
	}

	@Override
	public abstract String toString();

	public abstract int distance(Statement st);
	
	protected static int distanceAux(AdjacencyMatrix<Statement, Edge, CFG> body, Statement st) {
		int min = -1, m;
		for (Statement root : body.getRoots())
			if ((m = body.distance(root, st)) < min || min == -1)
				min = m;
		// we add 1 to consider the edge from the condition to the root
		return min == -1 ? min : 1 + min;
	}
}
