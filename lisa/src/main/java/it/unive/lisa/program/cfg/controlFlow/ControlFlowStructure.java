package it.unive.lisa.program.cfg.controlFlow;

import java.util.Collection;
import java.util.HashSet;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;

public abstract class ControlFlowStructure {

	protected final AdjacencyMatrix<Statement, Edge, CFG> cfgMatrix;

	private final Statement condition;

	private Statement firstFollower;

	protected ControlFlowStructure(AdjacencyMatrix<Statement, Edge, CFG> cfgMatrix, Statement condition,
			Statement firstFollower) {
		this.cfgMatrix = cfgMatrix;
		this.condition = condition;
		this.firstFollower = firstFollower;
	}

	public final Statement getCondition() {
		return condition;
	}

	public final Statement getFirstFollower() {
		return firstFollower;
	}

	public void setFirstFollower(Statement firstFollower) {
		this.firstFollower = firstFollower;
	}

	public final Collection<Statement> allStatements() {
		Collection<Statement> all = new HashSet<>(bodyStatements());
		all.add(getCondition());
		all.add(getFirstFollower());
		return all;
	}

	protected abstract Collection<Statement> bodyStatements();

	public abstract boolean contains(Statement st);

	public abstract void simplify();

	public final int distance(Statement st) {
		if (st == condition)
			return 0;

		if (st != firstFollower && !bodyStatements().contains(st))
			throw new IllegalArgumentException("The given statement is not in the conditional");

		return cfgMatrix.distance(condition, st);
	}

	public final AdjacencyMatrix<Statement, Edge, CFG> getCompleteStructure() {
		AdjacencyMatrix<Statement, Edge, CFG> complete = new AdjacencyMatrix<>();
		
		// add all nodes
		complete.addNode(getCondition());
		bodyStatements().forEach(complete::addNode);
		if (getFirstFollower() != null)
			complete.addNode(getFirstFollower());

		// add all edges that connect the added nodes
		Collection<Statement> nodes = complete.getNodes();
		for (Statement st : nodes)
			for (Statement follow : cfgMatrix.followersOf(st))
				if (nodes.contains(follow))
					complete.addEdge(cfgMatrix.getEdgeConnecting(st, follow));

		return complete;
	}

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
	
	protected static boolean areEqual(Collection<Statement> first,	Collection<Statement> second) {
		// the following keeps track of the unmatched nodes in second
		Collection<Statement> copy = new HashSet<>(second);
		boolean found;
		for (Statement f : first) {
			found = false;
			for (Statement s : second)
				if (copy.contains(s) && f.isEqualTo(s)) {
					copy.remove(s);
					found = true;
					break;
				}
			if (!found)
				return false;
		}

		if (!copy.isEmpty())
			return false;

		return true;
	}
}
