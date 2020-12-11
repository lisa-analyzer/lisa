package it.unive.lisa.cfg.statement;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ExpressionStore;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.symbolic.value.Skip;

/**
 * Terminates the execution of the CFG where this statement lies, without
 * returning anything to the caller. For terminating CFGs that must return a
 * value, use {@link Return}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Ret extends Statement {

	/**
	 * Builds the return. The location where this return happens is unknown
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param cfg the cfg that this statement belongs to
	 */
	public Ret(CFG cfg) {
		this(cfg, null, -1, -1);
	}

	/**
	 * Builds the return, happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this statement belongs to
	 * @param sourceFile the source file where this statement happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this statement happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this statement happens in the source
	 *                       file. If unknown, use {@code -1}
	 */
	public Ret(CFG cfg, String sourceFile, int line, int col) {
		super(cfg, sourceFile, line, col);
	}

	@Override
	public int setOffset(int offset) {
		return this.offset = offset;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ getClass().getName().hashCode();
	}

	@Override
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (!super.isEqualTo(st))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return "ret";
	}

	@Override
	public <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> typeInference(
			AnalysisState<H, TypeEnvironment> entryState, CallGraph callGraph,
			ExpressionStore<AnalysisState<H, TypeEnvironment>> expressions) throws SemanticException {
		return entryState.smallStepSemantics(new Skip());
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> semantics(
			AnalysisState<H, V> entryState, CallGraph callGraph, ExpressionStore<AnalysisState<H, V>> expressions)
			throws SemanticException {
		return entryState.smallStepSemantics(new Skip());
	}
}
