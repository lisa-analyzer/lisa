package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Skip;

/**
 * An assertion in an IMP program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPAssert extends Statement {

	private final Expression expression;

	/**
	 * Builds the assertion.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param expression the expression being asserted
	 */
	public IMPAssert(CFG cfg, String sourceFile, int line, int col, Expression expression) {
		super(cfg, sourceFile, line, col);
		this.expression = expression;
	}

	@Override
	public int setOffset(int offset) {
		return this.offset = offset;
	}

	@Override
	public String toString() {
		return "assert " + expression;
	}

	/**
	 * Yields the expression being asserted.
	 * 
	 * @return the expression
	 */
	public Expression getExpression() {
		return expression;
	}

	@Override
	public <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> typeInference(
			AnalysisState<H, TypeEnvironment> entryState, CallGraph callGraph,
			StatementStore<H, TypeEnvironment> expressions) throws SemanticException {
		AnalysisState<H, TypeEnvironment> result = expression.typeInference(entryState, callGraph, expressions);
		expressions.put(expression, result);
		if (!expression.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(expression.getMetaVariables());
		if (!expression.getDynamicType().isBooleanType())
			return result.bottom();
		return result.smallStepSemantics(new Skip());
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> semantics(
			AnalysisState<H, V> entryState, CallGraph callGraph, StatementStore<H, V> expressions)
			throws SemanticException {
		AnalysisState<H, V> result = expression.semantics(entryState, callGraph, expressions);
		expressions.put(expression, result);
		if (!expression.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(expression.getMetaVariables());
		if (!expression.getDynamicType().isBooleanType())
			return result.bottom();
		return result.smallStepSemantics(new Skip());
	}
}
