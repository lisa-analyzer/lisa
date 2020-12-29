package it.unive.lisa.cfg.statement;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.symbolic.value.Skip;

/**
 * A statement that raises an error, stopping the execution of the current CFG
 * and propagating the error to among the call chain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Throw extends UnaryStatement {

	/**
	 * Builds the throw, raising {@code expression} as error. The location where
	 * this throw happens is unknown (i.e. no source file/line/column is
	 * available).
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param expression the expression to raise as error
	 */
	public Throw(CFG cfg, Expression expression) {
		this(cfg, null, -1, -1, expression);
	}

	/**
	 * Builds the throw, raising {@code expression} as error, happening at the
	 * given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param sourceFile the source file where this statement happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this statement happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param expression the expression to raise as error
	 */
	public Throw(CFG cfg, String sourceFile, int line, int col, Expression expression) {
		super(cfg, sourceFile, line, col, expression);
	}

	@Override
	public final String toString() {
		return "throw " + getExpression();
	}

	@Override
	public <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> typeInference(
			AnalysisState<H, TypeEnvironment> entryState, CallGraph callGraph,
			StatementStore<H, TypeEnvironment> expressions) throws SemanticException {
		AnalysisState<H, TypeEnvironment> result = getExpression().typeInference(entryState, callGraph, expressions);
		expressions.put(getExpression(), result);
		if (!getExpression().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getExpression().getMetaVariables());
		return result.smallStepSemantics(new Skip());
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> semantics(
			AnalysisState<H, V> entryState, CallGraph callGraph, StatementStore<H, V> expressions)
			throws SemanticException {
		AnalysisState<H, V> result = getExpression().semantics(entryState, callGraph, expressions);
		expressions.put(getExpression(), result);
		if (!getExpression().getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(getExpression().getMetaVariables());
		return result.smallStepSemantics(new Skip());
	}
}
