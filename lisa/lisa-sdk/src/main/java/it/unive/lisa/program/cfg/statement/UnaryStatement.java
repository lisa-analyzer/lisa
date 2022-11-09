package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.Objects;

/**
 * A unary statement.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class UnaryStatement extends Statement {

	private final Expression expression;

	/**
	 * Builds the unary statement, happening at the given location in the
	 * program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param location   the location where this statement is defined within the
	 *                       program
	 * @param expression the argument of this statement
	 */
	protected UnaryStatement(CFG cfg, CodeLocation location, Expression expression) {
		super(cfg, location);
		Objects.requireNonNull(expression, "The argument of a unary statement cannot be null");
		this.expression = expression;
		expression.setParentStatement(this);
	}

	/**
	 * Yields the expression that is the argument of this unary statement.
	 * 
	 * @return the argument
	 */
	public final Expression getExpression() {
		return expression;
	}

	@Override
	public final int setOffset(int offset) {
		this.offset = offset;
		return expression.setOffset(offset + 1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
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
		UnaryStatement other = (UnaryStatement) obj;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		return true;
	}

	@Override
	public final <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		if (!expression.accept(visitor, tool))
			return false;
		return visitor.visit(tool, getCFG(), this);
	}

	@Override
	public Statement getStatementEvaluatedBefore(Statement other) {
		return other == this ? expression : null;
	}
}
