package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.Objects;

/**
 * A binary expression.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class BinaryExpression extends Expression {

	private final Expression left;

	private final Expression right;

	/**
	 * Builds the binary expression, happening at the given location in the
	 * program.
	 * 
	 * @param cfg      the cfg that this statement belongs to
	 * @param location the location where the expression is defined within the
	 *                     source file. If unknown, use {@code null}
	 * @param left     the left hand-side of the expression
	 * @param right    the right hand-side of the expression
	 */
	public BinaryExpression(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location);
		Objects.requireNonNull(left, "The left-handside of a binary expression cannot be null");
		Objects.requireNonNull(right, "The right-handside of a binary expression cannot be null");
		this.left = left;
		this.right = right;
		left.setParentStatement(this);
		right.setParentStatement(this);
	}

	/**
	 * Yields the left hand-side of the expression.
	 * 
	 * @return the left hand-side of the expression
	 */
	public final Expression getLeft() {
		return left;
	}

	/**
	 * Yields the right hand-side of the expression.
	 * 
	 * @return the right hand-side of the expression
	 */
	public final Expression getRight() {
		return right;
	}

	@Override
	public final int setOffset(int offset) {
		this.offset = offset;
		return right.setOffset(left.setOffset(offset + 1) + 1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
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
		BinaryExpression other = (BinaryExpression) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}

	@Override
	public final <V> boolean accept(
			GraphVisitor<CFG, Statement, it.unive.lisa.program.cfg.edge.Edge, V> visitor,
			V tool) {
		if (!left.accept(visitor, tool))
			return false;

		if (!right.accept(visitor, tool))
			return false;

		return visitor.visit(tool, getCFG(), this);
	}
}
