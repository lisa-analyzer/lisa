package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.datastructures.graph.Node;

/**
 * A node of a {@link BaseCallGraph}, representing a single {@link CodeMember}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CallGraphNode implements Node<CallGraphNode, CallGraphEdge, BaseCallGraph> {

	private final BaseCallGraph graph;
	private final CodeMember cm;

	/**
	 * Builds the node.
	 * 
	 * @param graph the parent graph
	 * @param cm    the code member represented by this node
	 */
	public CallGraphNode(BaseCallGraph graph, CodeMember cm) {
		this.graph = graph;
		this.cm = cm;
	}

	/**
	 * Yields the {@link CodeMember} represented by this node.
	 * 
	 * @return the code member
	 */
	public CodeMember getCodeMember() {
		return cm;
	}

	/**
	 * Yields the parent {@link BaseCallGraph} containing this node.
	 * 
	 * @return the parent graph
	 */
	public BaseCallGraph getGraph() {
		return graph;
	}

	@Override
	public int setOffset(int offset) {
		return offset;
	}

	@Override
	public <V> boolean accept(GraphVisitor<BaseCallGraph, CallGraphNode, CallGraphEdge, V> visitor, V tool) {
		return visitor.visit(tool, graph, this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cm == null) ? 0 : cm.hashCode());
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
		CallGraphNode other = (CallGraphNode) obj;
		if (cm == null) {
			if (other.cm != null)
				return false;
		} else if (!cm.equals(other.cm))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return cm.toString();
	}
}
