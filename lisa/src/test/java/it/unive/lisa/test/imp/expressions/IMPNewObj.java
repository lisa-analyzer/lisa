package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NativeCall;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.lang3.ArrayUtils;

/**
 * An expression modeling the object allocation and initialization operation
 * ({@code new className(...)}). The type of this expression is the
 * {@link UnitType} representing the created class. This expression corresponds
 * to a {@link HeapAllocation} that is used as first parameter (i.e.,
 * {@code this}) for the {@link UnresolvedCall} targeting the invoked
 * constructor. All parameters of the constructor call are provided to the
 * {@link UnresolvedCall}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPNewObj extends NativeCall {

	/**
	 * Builds the object allocation and initialization.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param type       the type of the object that is being created
	 * @param parameters the parameters of the constructor call
	 */
	public IMPNewObj(CFG cfg, String sourceFile, int line, int col, Type type, Expression... parameters) {
		super(cfg, sourceFile, line, col, "new", type, parameters);
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState, CallGraph callGraph, AnalysisState<A, H, V>[] computedStates,
					Collection<SymbolicExpression>[] params)
					throws SemanticException {
		HeapAllocation created = new HeapAllocation(getRuntimeTypes());

		// we need to add the receiver to the parameters
		VariableRef paramThis = new VariableRef(getCFG(), getSourceFile(), getLine(), getCol(), "this",
				getStaticType());
		Expression[] fullExpressions = ArrayUtils.insert(0, getParameters(), paramThis);
		Collection<SymbolicExpression>[] fullParams = ArrayUtils.insert(0, params, Collections.singleton(created));

		UnresolvedCall call = new UnresolvedCall(getCFG(), getSourceFile(), getLine(), getCol(),
				IMPFrontend.CALL_STRATEGY, true, getStaticType().toString(), fullExpressions);
		call.inheritRuntimeTypesFrom(this);
		return call.callSemantics(entryState, callGraph, computedStates, fullParams).smallStepSemantics(created, this);
	}
}
