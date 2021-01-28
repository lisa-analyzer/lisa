package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.CFG;
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
	 * Builds the binary expression. The location where it happens is unknown
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param cfg   the cfg that this statement belongs to
	 * @param left  the left hand-side of the expression
	 * @param right the right hand-side of the expression
	 */
	protected BinaryExpression(CFG cfg, Expression left, Expression right) {
		this(cfg, null, -1, -1, left, right);
	}

	/**
	 * Builds the binary expression, happening at the given location in the
	 * program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param sourceFile the source file where this statement happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this statement happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param left       the left hand-side of the expression
	 * @param right      the right hand-side of the expression
	 */
	public BinaryExpression(CFG cfg, String sourceFile, int line, int col, Expression left, Expression right) {
		super(cfg, sourceFile, line, col);
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
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (getClass() != st.getClass())
			return false;
		if (!super.isEqualTo(st))
			return false;
		BinaryExpression other = (BinaryExpression) st;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.isEqualTo(other.right))
			return false;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.isEqualTo(other.left))
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
