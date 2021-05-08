package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.datastructures.graph.Node;

public class CallGraphNode implements Node<CallGraphNode, CallGraphEdge, BaseCallGraph> {

	private final BaseCallGraph graph;
	private final CodeMember cm;

	public CallGraphNode(BaseCallGraph graph, CodeMember cfg) {
		this.graph = graph;
		this.cm = cfg;
	}
	
	public CodeMember getCodeMember() {
		return cm;
	}
	
	public BaseCallGraph getGraph() {
		return graph;
	}

	@Override
	public int setOffset(int offset) {
		return offset;
	}

	@Override
	public boolean isEqualTo(CallGraphNode other) {
		return equals(other);
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
