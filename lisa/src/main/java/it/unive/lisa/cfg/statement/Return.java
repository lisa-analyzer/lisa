package it.unive.lisa.cfg.statement;

import java.util.Objects;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFG.ExpressionStates;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueIdentifier;

/**
 * Returns an expression to the caller CFG, terminating the execution of the CFG
 * where this statement lies. For terminating CFGs that do not return any value,
 * use {@link Ret}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Return extends Statement implements MetaVariableCreator {

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
	public int setOffset(int offset) {
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

	@Override
	public final Identifier getMetaVariable() {
		return new ValueIdentifier(expression.getRuntimeTypes(), "ret_value@" + getCFG().getDescriptor().getName());
	}

	@Override
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> semantics(
			AnalysisState<H, V> entryState, CallGraph callGraph, ExpressionStates<H, V> expressions)
			throws SemanticException {
		AnalysisState<H, V> exprResult = expression.semantics(entryState, callGraph, expressions);
		expressions.put(expression, exprResult);
		
		AnalysisState<H, V> result = null;
		Identifier meta = getMetaVariable();
		for (SymbolicExpression expr : exprResult.getComputedExpressions()) {
			AnalysisState<H, V> tmp = exprResult.assign(meta, expr);
			if (result == null)
				result =  tmp;
			else
				result = result.lub(tmp);
		}
		
		if (!expression.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(expression.getMetaVariables());
		return result;
	}
}
