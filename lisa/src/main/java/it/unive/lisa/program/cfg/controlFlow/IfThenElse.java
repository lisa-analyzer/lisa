package it.unive.lisa.program.cfg.controlFlow;

import java.util.Collection;
import java.util.HashSet;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;

public class IfThenElse extends ControlFlowStructure {

	private final Collection<Statement> trueBranch;

	private final Collection<Statement> falseBranch;

	public IfThenElse(AdjacencyMatrix<Statement, Edge, CFG> cfgMatrix, Statement condition, Statement firstFollower,
			Collection<Statement> trueBranch, Collection<Statement> falseBranch) {
		super(cfgMatrix, condition, firstFollower);
		this.trueBranch = trueBranch;
		this.falseBranch = falseBranch;
	}

	@Override
	public Collection<Statement> bodyStatements() {
		Collection<Statement> all = new HashSet<>(getTrueBranch());
		all.addAll(getFalseBranch());
		return all;
	}

	public Collection<Statement> getTrueBranch() {
		return trueBranch;
	}

	public Collection<Statement> getFalseBranch() {
		return falseBranch;
	}

	@Override
	public boolean contains(Statement st) {
		return trueBranch.contains(st) || falseBranch.contains(st);
	}

	@Override
	public void simplify() {
		trueBranch.removeIf(NoOp.class::isInstance);
		falseBranch.removeIf(NoOp.class::isInstance);
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
	public boolean isEqualTo(ControlFlowStructure obj) {
		if (this == obj)
			return true;
		if (!super.isEqualTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		IfThenElse other = (IfThenElse) obj;
		if (falseBranch == null) {
			if (other.falseBranch != null)
				return false;
		} else if (!areEqual(falseBranch, other.falseBranch))
			return false;
		if (trueBranch == null) {
			if (other.trueBranch != null)
				return false;
		} else if (!areEqual(trueBranch, other.trueBranch))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "if-then-else[" + getCondition() + "]";
	}
}
