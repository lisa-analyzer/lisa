package it.unive.lisa.cfg.statement;

import java.util.Objects;

import it.unive.lisa.cfg.CFG;

/**
 * A statement assigning the result of an expression to an assignable
 * expression.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Assignment extends Expression {

	/**
	 * The target that will get its value overwritten
	 */
	private final Expression target;

	/**
	 * The expression whose result is to be stored in the target of this assignment
	 */
	private final Expression expression;

	/**
	 * Builds the assignment, assigning {@code expression} to {@code target}. The
	 * location where this assignment happens is unknown (i.e. no source
	 * file/line/column is available).
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param target     the target of the assignment
	 * @param expression the expression to assign to {@code target}
	 */
	public Assignment(CFG cfg, Expression target, Expression expression) {
		this(cfg, null, -1, -1, target, expression);
	}

	/**
	 * Builds the assignment, assigning {@code expression} to {@code target},
	 * happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param sourceFile the source file where this statement happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this statement happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source file.
	 *                   If unknown, use {@code -1}
	 * @param target     the target of the assignment
	 * @param expression the expression to assign to {@code target}
	 */
	public Assignment(CFG cfg, String sourceFile, int line, int col, Expression target, Expression expression) {
		super(cfg, sourceFile, line, col);
		Objects.requireNonNull(target, "The target of an assignment cannot be null");
		Objects.requireNonNull(expression, "The expression of an assignment cannot be null");
		this.target = target;
		this.expression = expression;
	}

	/**
	 * Yields the target of this assignment, that is, the expression whose value is
	 * overwritten.
	 * 
	 * @return the target of this assignment
	 */
	public final Expression getTarget() {
		return target;
	}

	/**
	 * Yields the expression of this assignment, that is, the expression whose value
	 * is stored into the target of this assignment.
	 * 
	 * @return the expression of this assignment
	 */
	public final Expression getExpression() {
		return expression;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (getClass() != st.getClass())
			return false;
		Assignment other = (Assignment) st;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.isEqualTo(other.expression))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.isEqualTo(other.target))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return target + " = " + expression;
	}
}
