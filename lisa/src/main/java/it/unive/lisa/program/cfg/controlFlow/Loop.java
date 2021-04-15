package it.unive.lisa.program.cfg.controlFlow;

import java.util.Collection;
import java.util.HashSet;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;

public class Loop extends ControlFlowStructure {

	private final Collection<Statement> body;

	private final boolean trueBranch;

	public Loop(AdjacencyMatrix<Statement, Edge, CFG> cfgMatrix, Statement condition, Statement firstFollower,
			Collection<Statement> body) {
		this(cfgMatrix, condition, firstFollower, body, true);
	}

	public Loop(AdjacencyMatrix<Statement, Edge, CFG> cfgMatrix, Statement condition, Statement firstFollower,
			Collection<Statement> body, boolean trueBranch) {
		super(cfgMatrix, condition, firstFollower);
		this.body = body;
		this.trueBranch = trueBranch;
	}

	@Override
	public Collection<Statement> bodyStatements() {
		return new HashSet<>(getBody());
	}

	public Collection<Statement> getBody() {
		return body;
	}

	@Override
	public boolean contains(Statement st) {
		return body.contains(st);
	}

	@Override
	public void simplify() {
		body.removeIf(NoOp.class::isInstance);
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
		} else if (!areEqual(body, other.body))
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
