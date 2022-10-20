package it.unive.lisa.program.cfg.controlFlow;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.Collection;
import java.util.HashSet;

/**
 * A control flow structure of a {@link CFG}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class ControlFlowStructure {

	/**
	 * The matrix of the cfg containing this structure.
	 */
	protected final NodeList<CFG, Statement, Edge> cfgMatrix;

	private final Statement condition;

	private Statement firstFollower;

	/**
	 * Builds the structure.
	 * 
	 * @param cfgMatrix     the matrix of the cfg containing this structure
	 * @param condition     the condition of the structure
	 * @param firstFollower the first statement after the structure exits
	 */
	protected ControlFlowStructure(NodeList<CFG, Statement, Edge> cfgMatrix, Statement condition,
			Statement firstFollower) {
		this.cfgMatrix = cfgMatrix;
		this.condition = condition;
		this.firstFollower = firstFollower;
	}

	/**
	 * Yields the condition of this structure.
	 * 
	 * @return the condition
	 */
	public final Statement getCondition() {
		return condition;
	}

	/**
	 * Yields the first follower of this structure, that is, the first statement
	 * after the conditional structure exits.
	 * 
	 * @return the follower
	 */
	public final Statement getFirstFollower() {
		return firstFollower;
	}

	/**
	 * Sets the first follower of this structure, that is, the first statement
	 * after the conditional structure exits.
	 * 
	 * @param firstFollower the new follower
	 */
	public void setFirstFollower(Statement firstFollower) {
		this.firstFollower = firstFollower;
	}

	/**
	 * Yields all the {@link Statement}s contained in this structure, including
	 * the condition and the first follower.
	 * 
	 * @return the statements of the body of this structure
	 */
	public final Collection<Statement> allStatements() {
		Collection<Statement> all = new HashSet<>(bodyStatements());
		all.add(getCondition());
		all.add(getFirstFollower());
		return all;
	}

	/**
	 * Yields all the {@link Statement}s contained in the body of this structure
	 * (thus excluding the condition and the first follower).
	 * 
	 * @return the statements of the body of this structure
	 */
	protected abstract Collection<Statement> bodyStatements();

	/**
	 * Yields {@code true} if the given statement is part of the body of this
	 * structure. Note that this method will return {@code false} if {@code st}
	 * is either the condition of the first follower of this structure.
	 * 
	 * @param st the statement to check
	 * 
	 * @return {@code true} if {@code st} is in the body of this structure
	 */
	public abstract boolean contains(Statement st);

	/**
	 * Simplifies this structure, removing all {@link NoOp}s from its body.
	 */
	public abstract void simplify();

	/**
	 * Yields the minimum distance, in terms of number of edges to traverse,
	 * between the condition of this structure and the given node. If {@code st}
	 * is not in this structure, this method returns {@code -1}. If the distance
	 * is greater than {@link Integer#MAX_VALUE}, {@link Integer#MAX_VALUE} is
	 * returned.
	 * 
	 * @param st the node to reach starting at the condition
	 * 
	 * @return the minimum distance, in terms of number of edges to traverse,
	 *             between the condition and the given node
	 */
	public int distance(Statement st) {
		if (st == condition)
			return 0;

		if (st != firstFollower && !bodyStatements().contains(st))
			return -1;

		return cfgMatrix.distance(condition, st);
	}

	/**
	 * Yields an {@link NodeList} containing the full structure (nodes and
	 * edges) represented by this conditional structure. The returned matrix
	 * will also contain the condition and the first follower, if any.
	 * 
	 * @return the matrix containing the full structure
	 */
	public NodeList<CFG, Statement, Edge> getCompleteStructure() {
		NodeList<CFG, Statement, Edge> complete = new NodeList<>(new SequentialEdge(), false);

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
		result = prime * result + ((cfgMatrix == null) ? 0 : cfgMatrix.hashCode());
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
		if (cfgMatrix == null) {
			if (other.cfgMatrix != null)
				return false;
		} else if (!cfgMatrix.equals(other.cfgMatrix))
			return false;
		if (firstFollower == null) {
			if (other.firstFollower != null)
				return false;
		} else if (!firstFollower.equals(other.firstFollower))
			return false;
		return true;
	}

	@Override
	public abstract String toString();
}
