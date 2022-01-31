package it.unive.lisa.imp.expressions;

import org.apache.commons.lang3.ArrayUtils;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;

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
public class IMPNewObj extends NaryExpression {

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
		super(cfg, new SourceCodeLocation(sourceFile, line, col), "new " + type, type, parameters);
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>, T extends TypeDomain<T>> AnalysisState<A, H, V, T> expressionSemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					ExpressionSet<SymbolicExpression>[] params,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		HeapAllocation created = new HeapAllocation(getStaticType(), getLocation());

		// we need to add the receiver to the parameters
		VariableRef paramThis = new VariableRef(getCFG(), getLocation(), "this", getStaticType());
		Expression[] fullExpressions = ArrayUtils.insert(0, getSubExpressions(), paramThis);
		ExpressionSet<SymbolicExpression>[] fullParams = ArrayUtils.insert(0, params, new ExpressionSet<>(created));

		UnresolvedCall call = new UnresolvedCall(getCFG(), getLocation(),
				IMPFrontend.ASSIGN_STRATEGY, IMPFrontend.MATCHING_STRATEGY, IMPFrontend.TRAVERSAL_STRATEGY, true,
				getStaticType().toString(), getStaticType().toString(), fullExpressions);
		call.setRuntimeTypes(getRuntimeTypes());
		AnalysisState<A, H, V, T> sem = call.expressionSemantics(interprocedural, state, fullParams, expressions);

		if (!call.getMetaVariables().isEmpty())
			sem = sem.forgetIdentifiers(call.getMetaVariables());

		sem = sem.smallStepSemantics(created, this);

		AnalysisState<A, H, V, T> result = state.bottom();
		for (SymbolicExpression loc : sem.getComputedExpressions())
			result = result.lub(sem.smallStepSemantics(
					new HeapReference(new ReferenceType(loc.getRuntimeTypes()), loc, getLocation()), call));

		return result;
	}
}
