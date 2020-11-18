package it.unive.lisa.cfg.statement;

import java.util.Objects;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFG.ExpressionStates;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

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
	public int setOffset(int offset) {
		this.offset = offset;
		return expression.setOffset(target.setOffset(offset + 1) + 1);
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
		if (!super.isEqualTo(st))
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

	/**
	 * Semantics of an assignment ({@code left = right}) is evaluated as follows:
	 * <ol>
	 * <li>the semantic of the {@code right} is evaluated using the given
	 * {@code entryState}, returning a new analysis state
	 * {@code as_r = <state_r, expr_r>}</li>
	 * <li>the semantic of the {@code left} is evaluated using {@code as_r},
	 * returning a new analysis state {@code as_l = <state_l, expr_l>}</li>
	 * <li>the final post-state is evaluated through
	 * {@link AnalysisState#assign(Identifier, SymbolicExpression)}, using
	 * {@code expr_l} as {@code id} and {@code expr_r} as {@code value}</li>
	 * </ol>
	 * This means that all side effects from {@code right} are evaluated before the
	 * ones from {@code left}.<br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> semantics(
			AnalysisState<H, V> entryState, CallGraph callGraph, ExpressionStates<H, V> expressions)
			throws SemanticException {
		AnalysisState<H, V> right = expression.semantics(entryState, callGraph, expressions);
		AnalysisState<H, V> left = target.semantics(right, callGraph, expressions);
		expressions.put(expression, right);
		expressions.put(target, left);
		AnalysisState<H, V> result = left.assign((Identifier) left.getLastComputedExpression(), right.getLastComputedExpression());
		if (!expression.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(expression.getMetaVariables());
		if (!target.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(target.getMetaVariables());
		return result;
	}
}
