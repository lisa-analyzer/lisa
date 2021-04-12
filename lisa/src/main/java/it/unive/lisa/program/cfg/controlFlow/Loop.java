package it.unive.lisa.program.cfg.controlFlow;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;

public class Loop extends ControlFlowStructure {

	private final AdjacencyMatrix<Statement, Edge, CFG> body;

	private final boolean trueBranch;

	public Loop(Statement condition, Statement firstFollower, AdjacencyMatrix<Statement, Edge, CFG> body) {
		this(condition, firstFollower, body, true);
	}

	public Loop(Statement condition, Statement firstFollower, AdjacencyMatrix<Statement, Edge, CFG> body,
			boolean trueBranch) {
		super(condition, firstFollower);
		this.body = body;
		this.trueBranch = trueBranch;
	}

	public AdjacencyMatrix<Statement, Edge, CFG> getBody() {
		return body;
	}

	@Override
	public boolean contains(Statement st) {
		return body.containsNode(st, true);
	}

	@Override
	public int distance(Statement st) {
		return distanceAux(body, st);
	}

	@Override
	public void simplify() {
		Set<Statement> targets = body.getNodes().stream().filter(NoOp.class::isInstance).collect(Collectors.toSet());
		body.simplify(targets, Collections.emptySet());
	}

	@Override
	public AdjacencyMatrix<Statement, Edge, CFG> getCompleteStructure() {
		AdjacencyMatrix<Statement, Edge, CFG> complete = new AdjacencyMatrix<>(body);
		complete.addNode(getCondition());
		if (getFirstFollower() != null)
			complete.addNode(getFirstFollower());
		
		if (trueBranch) {
			complete.addEdge(new TrueEdge(getCondition(), body.getEntries().iterator().next()));
			if (getFirstFollower() != null && !body.getNodes().isEmpty())
				complete.addEdge(new FalseEdge(getCondition(), getFirstFollower()));
		} else {
			complete.addEdge(new FalseEdge(getCondition(), body.getEntries().iterator().next()));
			if (getFirstFollower() != null && !body.getNodes().isEmpty())
				complete.addEdge(new TrueEdge(getCondition(), getFirstFollower()));
		}
		return complete;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + (trueBranch ? 1231 : 1237);
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
		Loop other = (Loop) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (trueBranch != other.trueBranch)
			return false;
		return true;
	}

	@Override
	public boolean isEqualTo(ControlFlowStructure obj) {
		if (this == obj)
			return true;
		if (!super.isEqualTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Loop other = (Loop) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.isEqualTo(other.body))
			return false;
		if (trueBranch != other.trueBranch)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "loop[" + getCondition() + "]";
	}
}
