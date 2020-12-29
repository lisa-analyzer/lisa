package it.unive.lisa.cfg.statement;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
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
public class Return extends UnaryStatement implements MetaVariableCreator {

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
	 * Builds the return, returning {@code expression} to the caller CFG,
	 * happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param sourceFile the source file where this statement happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this statement happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param expression the expression to return
	 */
	public Return(CFG cfg, String sourceFile, int line, int col, Expression expression) {
		super(cfg, sourceFile, line, col, expression);
	}

	@Override
	public final String toString() {
		return "return " + getExpression();
	}

	@Override
	public final Identifier getMetaVariable() {
		return new ValueIdentifier(getExpression().getRuntimeTypes(),
				"ret_value@" + getCFG().getDescriptor().getName());
	}

	@Override
	public <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> typeInference(
			AnalysisState<H, TypeEnvironment> entryState, CallGraph callGraph,
			StatementStore<H, TypeEnvironment> expressions) throws SemanticException {
		AnalysisState<H, TypeEnvironment> exprResult = getExpression().typeInference(entryState, callGraph,
				expressions);
		expressions.put(getExpression(), exprResult);

		AnalysisState<H, TypeEnvironment> result = null;
		Identifier meta = getMetaVariable();
		for (SymbolicExpression expr : exprResult.getComputedExpressions()) {
			AnalysisState<H, TypeEnvironment> tmp = exprResult.assign(meta, expr);
			if (result == null)
				result = tmp;
			else
				result = result.lub(tmp);
		}

		if (!getExpression().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getExpression().getMetaVariables());
		return result;
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> semantics(
			AnalysisState<H, V> entryState, CallGraph callGraph, StatementStore<H, V> expressions)
			throws SemanticException {
		AnalysisState<H, V> exprResult = getExpression().semantics(entryState, callGraph, expressions);
		expressions.put(getExpression(), exprResult);

		AnalysisState<H, V> result = null;
		Identifier meta = getMetaVariable();
		for (SymbolicExpression expr : exprResult.getComputedExpressions()) {
			AnalysisState<H, V> tmp = exprResult.assign(meta, expr);
			if (result == null)
				result = tmp;
			else
				result = result.lub(tmp);
		}

		if (!getExpression().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getExpression().getMetaVariables());
		return result;
	}
}
