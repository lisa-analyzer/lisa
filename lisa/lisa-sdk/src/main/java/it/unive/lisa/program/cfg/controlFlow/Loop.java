package it.unive.lisa.program.cfg.controlFlow;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.Collection;
import java.util.HashSet;

/**
 * A {@link ControlFlowStructure} representing a loop.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Loop extends ControlFlowStructure {

	private final Collection<Statement> body;

	/**
	 * Builds the loop.
	 * 
	 * @param cfgMatrix     the matrix of the cfg containing this loop
	 * @param condition     the condition of the loop
	 * @param firstFollower the first statement after the loop exits
	 * @param body          the statements in the loop body
	 */
	public Loop(NodeList<CFG, Statement, Edge> cfgMatrix, Statement condition, Statement firstFollower,
			Collection<Statement> body) {
		super(cfgMatrix, condition, firstFollower);
		this.body = body;
	}

	@Override
	public Collection<Statement> bodyStatements() {
		return new HashSet<>(getBody());
	}

	/**
	 * Yields the {@link Statement}s contained in the body of this loop.
	 * 
	 * @return the body of the loop
	 */
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
		return true;
	}

	@Override
	public String toString() {
		return "loop[" + getCondition() + "]";
	}
}
