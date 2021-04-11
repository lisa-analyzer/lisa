package it.unive.lisa.program.cfg.controlFlow;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;

public class IfThenElse extends ControlFlowStructure {

	private final AdjacencyMatrix<Statement, Edge, CFG> trueBranch;

	private final AdjacencyMatrix<Statement, Edge, CFG> falseBranch;

	public IfThenElse(Statement condition, Statement firstFollower, AdjacencyMatrix<Statement, Edge, CFG> trueBranch,
			AdjacencyMatrix<Statement, Edge, CFG> falseBranch) {
		super(condition, firstFollower);
		this.trueBranch = trueBranch;
		this.falseBranch = falseBranch;
	}

	public AdjacencyMatrix<Statement, Edge, CFG> getTrueBranch() {
		return trueBranch;
	}

	public AdjacencyMatrix<Statement, Edge, CFG> getFalseBranch() {
		return falseBranch;
	}
	
	@Override
	public boolean contains(Statement st) {
		return trueBranch.containsNode(st, true) || falseBranch.containsNode(st, true);
	}
	
	@Override
	public int distance(Statement st) {
		return Math.min(distanceAux(trueBranch, st), distanceAux(falseBranch, st));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((falseBranch == null) ? 0 : falseBranch.hashCode());
		result = prime * result + ((trueBranch == null) ? 0 : trueBranch.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		IfThenElse other = (IfThenElse) obj;
		if (falseBranch == null) {
			if (other.falseBranch != null)
				return false;
		} else if (!falseBranch.equals(other.falseBranch))
			return false;
		if (trueBranch == null) {
			if (other.trueBranch != null)
				return false;
		} else if (!trueBranch.equals(other.trueBranch))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "if-then-else[" + getCondition() + "]";
	}
}
