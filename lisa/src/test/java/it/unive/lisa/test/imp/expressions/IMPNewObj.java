package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.NativeCall;
import it.unive.lisa.cfg.statement.UnresolvedCall;
import it.unive.lisa.cfg.statement.Variable;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.test.imp.types.ClassType;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.lang3.ArrayUtils;

/**
 * An expression modeling the object allocation and initialization operation
 * ({@code new className(...)}). The type of this expression is the
 * {@link ClassType} representing the created class. This expression corresponds
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
		Variable paramThis = new Variable(getCFG(), getSourceFile(), getLine(), getCol(), "this", getStaticType());
		Expression[] fullExpressions = ArrayUtils.insert(0, getParameters(), paramThis);
		Collection<SymbolicExpression>[] fullParams = ArrayUtils.insert(0, params, Collections.singleton(created));

		UnresolvedCall call = new UnresolvedCall(getCFG(), getSourceFile(), getLine(), getCol(),
				getStaticType().toString(), fullExpressions);
		call.inheritRuntimeTypesFrom(this);
		return call.callSemantics(entryState, callGraph, computedStates, fullParams).smallStepSemantics(created);
	}

	@Override
	public <A extends AbstractState<A, H, TypeEnvironment>,
			H extends HeapDomain<H>> AnalysisState<A, H, TypeEnvironment> callTypeInference(
					AnalysisState<A, H, TypeEnvironment> entryState, CallGraph callGraph,
					AnalysisState<A, H, TypeEnvironment>[] computedStates,
					Collection<SymbolicExpression>[] params) throws SemanticException {
		// we still need to compute the call to ensure that the type information
		// is propagated in the constructor
		HeapAllocation created = new HeapAllocation(getRuntimeTypes());

		// we need to add the receiver to the parameters
		Variable paramThis = new Variable(getCFG(), getSourceFile(), getLine(), getCol(), "this", getStaticType());
		Expression[] fullExpressions = ArrayUtils.insert(0, getParameters(), paramThis);
		Collection<SymbolicExpression>[] fullParams = ArrayUtils.insert(0, params, Collections.singleton(created));

		for (int i = 0; i < fullParams.length; i++)
			for (SymbolicExpression e : fullParams[i]) {
				Type ref = fullExpressions[i].getStaticType();
				if (!e.getDynamicType().isUntyped() && !e.getTypes().anyMatch(t -> t.canBeAssignedTo(ref)))
					return entryState.bottom();
			}

		UnresolvedCall call = new UnresolvedCall(getCFG(), getSourceFile(), getLine(), getCol(),
				getStaticType().toString(), fullExpressions);
		AnalysisState<A, H,
				TypeEnvironment> typing = call.callTypeInference(entryState, callGraph, computedStates, fullParams);

		// at this stage, the runtime types correspond to the singleton set
		// containing only the static type. This is fine since we are creating
		// exactly an instance of that type
		setRuntimeTypes(getRuntimeTypes());

		return typing.smallStepSemantics(created);
	}
}
