package it.unive.lisa.cfg.statement;

import java.util.Objects;

import it.unive.lisa.cfg.CFG;

/**
 * Returns an expression to the caller CFG.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Return extends Statement {

	/**
	 * The expression being returned
	 */
	private final Expression expression;

	/**
	 * Builds the return, returning {@code expression} to the caller CFG. The
	 * location where this return happens is unknown (i.e. no source
	 * file/line/column is available).
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param expression the expression to return
	 */
	public Return(CFG cfg, Expression expression) {
		this(cfg, null, -1, -1, expression);
	}

	/**
	 * Builds the return, returning {@code expression} to the caller CFG, happening
	 * at the given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param sourceFile the source file where this statement happens. If unknown,
	 *                   use {@code null}
	 * @param line       the line number where this statement happens in the source
	 *                   file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source file.
	 *                   If unknown, use {@code -1}
	 * @param expression the expression to return
	 */
	public Return(CFG cfg, String sourceFile, int line, int col, Expression expression) {
		super(cfg, sourceFile, line, col);
		Objects.requireNonNull(expression, "The expression of a return cannot be null");
		this.expression = expression;
	}

	/**
	 * Yields the expression that is being returned.
	 * 
	 * @return the expression being returned
	 */
	public final Expression getExpression() {
		return expression;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		return result;
	}

	@Override
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (getClass() != st.getClass())
			return false;
		Return other = (Return) st;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.isEqualTo(other.expression))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return "return " + expression;
	}
}
