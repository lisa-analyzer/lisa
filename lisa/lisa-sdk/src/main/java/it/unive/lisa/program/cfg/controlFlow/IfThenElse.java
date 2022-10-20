package it.unive.lisa.program.cfg.controlFlow;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.Collection;
import java.util.HashSet;

/**
 * A {@link ControlFlowStructure} representing a if-then-else.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IfThenElse extends ControlFlowStructure {

	private final Collection<Statement> trueBranch;

	private final Collection<Statement> falseBranch;

	/**
	 * Builds the if-then-else.
	 * 
	 * @param cfgMatrix     the matrix of the cfg containing this if-then-else
	 * @param condition     the condition of the if-then-else
	 * @param firstFollower the first statement after the if-then-else exits one
	 *                          of the branches
	 * @param trueBranch    the statements in the true branch
	 * @param falseBranch   the statements in the false branch
	 */
	public IfThenElse(NodeList<CFG, Statement, Edge> cfgMatrix, Statement condition, Statement firstFollower,
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

	/**
	 * Yields the {@link Statement}s contained in the true branch of this
	 * if-then-else.
	 * 
	 * @return the true branch of the if-then-else
	 */
	public Collection<Statement> getTrueBranch() {
		return trueBranch;
	}

	/**
	 * Yields the {@link Statement}s contained in the false branch of this
	 * if-then-else.
	 * 
	 * @return the false branch of the if-then-else
	 */
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
	public String toString() {
		return "if-then-else[" + getCondition() + "]";
	}
}
