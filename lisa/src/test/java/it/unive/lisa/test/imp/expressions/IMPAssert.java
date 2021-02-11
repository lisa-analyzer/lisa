package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.UnaryStatement;
import it.unive.lisa.symbolic.value.Skip;

/**
 * An assertion in an IMP program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPAssert extends UnaryStatement {

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
		super(cfg, sourceFile, line, col, expression);
	}

	@Override
	public String toString() {
		return "assert " + getExpression();
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
					AnalysisState<A, H, V> entryState, CallGraph callGraph, StatementStore<A, H, V> expressions)
					throws SemanticException {
		AnalysisState<A, H, V> result = getExpression().semantics(entryState, callGraph, expressions);
		expressions.put(getExpression(), result);
		if (!getExpression().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getExpression().getMetaVariables());
		if (!getExpression().getDynamicType().isBooleanType())
			return result.bottom();
		return result.smallStepSemantics(new Skip(), this);
	}
}
