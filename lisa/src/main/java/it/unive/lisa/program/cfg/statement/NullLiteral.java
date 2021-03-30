package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.NullConstant;
import it.unive.lisa.type.NullType;

/**
 * A literal representing the {@code null} constant.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NullLiteral extends Literal {

	private static final Object NULL_CONST = new Object();

	/**
	 * Builds the null literal. The location where this literal happens is
	 * unknown (i.e. no source file/line/column is available). The type of a
	 * null literal is {@link NullType}.
	 * 
	 * @param cfg the cfg that this expression belongs to
	 */
	public NullLiteral(CFG cfg) {
		super(cfg, NULL_CONST, NullType.INSTANCE);
	}

	/**
	 * Builds the null literal, happening at the given location in the program.
	 * The type of a null literal is {@link NullType}.
	 * 
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location where the expression is defined within the
	 *                     source file. If unknown, use {@code null}
	 */
	public NullLiteral(CFG cfg, CodeLocation location) {
		super(cfg, location, NULL_CONST, NullType.INSTANCE);
	}

	@Override
	public String toString() {
		return "null";
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
					StatementStore<A, H, V> expressions)
					throws SemanticException {
		return entryState.smallStepSemantics(NullConstant.INSTANCE, this);
	}
}
