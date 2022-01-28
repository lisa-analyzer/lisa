package it.unive.lisa.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;

/**
 * An expression modeling the array element access operation
 * ({@code array[index]}). The type of this expression is the one of the
 * resolved array element.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPArrayAccess extends BinaryExpression {

	/**
	 * Builds the array access.
	 * 
	 * @param cfg        the {@link ImplementedCFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param container  the expression representing the array reference that
	 *                       will receive the access
	 * @param location   the expression representing the accessed element
	 */
	public IMPArrayAccess(ImplementedCFG cfg, String sourceFile, int line, int col, Expression container,
			Expression location) {
		super(cfg, new SourceCodeLocation(sourceFile, line, col), "[]", container, location);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A, H, V> expressions)
					throws SemanticException {

		if (!left.getDynamicType().isArrayType() && !left.getDynamicType().isUntyped())
			return state.bottom();
		// it is not possible to detect the correct type of the field without
		// resolving it. we rely on the rewriting that will happen inside heap
		// domain to translate this into a variable that will have its correct
		// type
		HeapDereference deref = new HeapDereference(getRuntimeTypes(), left, getLocation());
		return state.smallStepSemantics(new AccessChild(getRuntimeTypes(), deref, right, getLocation()), this);
	}
}
