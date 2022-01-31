package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.program.cfg.statement.call.assignment.PythonLikeAssigningStrategy;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * A call to a CFG that is not under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class OpenCall extends CallWithResult implements MetaVariableCreator {

	/**
	 * Builds the open call, happening at the given location in the program.
	 * 
	 * @param cfg          the cfg that this expression belongs to
	 * @param location     the location where the expression is defined within
	 *                         the program
	 * @param instanceCall whether or not this is a call to an instance cfg of a
	 *                         unit (that can be overridden) or not
	 * @param qualifier    the optional qualifier of the call (can be null or
	 *                         empty - see {@link #getFullTargetName()} for more
	 *                         info)
	 * @param targetName   the name of the target of this open call
	 * @param parameters   the parameters of this call
	 */
	public OpenCall(CFG cfg, CodeLocation location, boolean instanceCall, String qualifier, String targetName,
			Expression... parameters) {
		// if a call is open we don't really care if it's instance or not and we
		// will never perform parameter assignment
		super(cfg, location, PythonLikeAssigningStrategy.INSTANCE, instanceCall, qualifier, targetName,
				LeftToRightEvaluation.INSTANCE, Untyped.INSTANCE, parameters);
	}

	/**
	 * Builds the open call, happening at the given location in the program.
	 * 
	 * @param cfg          the cfg that this expression belongs to
	 * @param location     the location where the expression is defined within
	 *                         the program
	 * @param instanceCall whether or not this is a call to an instance cfg of a
	 *                         unit (that can be overridden) or not
	 * @param qualifier    the optional qualifier of the call (can be null or
	 *                         empty - see {@link #getFullTargetName()} for more
	 *                         info)
	 * @param targetName   the name of the target of this open call
	 * @param order        the evaluation order of the sub-expressions
	 * @param parameters   the parameters of this call
	 */
	public OpenCall(CFG cfg, CodeLocation location, boolean instanceCall, String qualifier, String targetName,
			EvaluationOrder order, Expression... parameters) {
		// if a call is open we don't really care if it's instance or not and we
		// will never perform parameter assignment
		super(cfg, location, PythonLikeAssigningStrategy.INSTANCE, instanceCall, qualifier, targetName, order,
				Untyped.INSTANCE,
				parameters);
	}

	/**
	 * Builds the open call, happening at the given location in the program. The
	 * {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg          the cfg that this expression belongs to
	 * @param location     the location where the expression is defined within
	 *                         the program
	 * @param instanceCall whether or not this is a call to an instance cfg of a
	 *                         unit (that can be overridden) or not
	 * @param qualifier    the optional qualifier of the call (can be null or
	 *                         empty - see {@link #getFullTargetName()} for more
	 *                         info)
	 * @param targetName   the name of the target of this open call
	 * @param parameters   the parameters of this call
	 * @param staticType   the static type of this call
	 */
	public OpenCall(CFG cfg, CodeLocation location, boolean instanceCall, String qualifier, String targetName,
			Type staticType, Expression... parameters) {
		// if a call is open we don't really care if it's instance or not and we
		// will never perform parameter assignment
		this(cfg, location, instanceCall, qualifier, targetName, LeftToRightEvaluation.INSTANCE, staticType,
				parameters);
	}

	/**
	 * Builds the open call, happening at the given location in the program.
	 * 
	 * @param cfg          the cfg that this expression belongs to
	 * @param location     the location where the expression is defined within
	 *                         the program
	 * @param instanceCall whether or not this is a call to an instance cfg of a
	 *                         unit (that can be overridden) or not
	 * @param qualifier    the optional qualifier of the call (can be null or
	 *                         empty - see {@link #getFullTargetName()} for more
	 *                         info)
	 * @param targetName   the name of the target of this open call
	 * @param order        the evaluation order of the sub-expressions
	 * @param parameters   the parameters of this call
	 * @param staticType   the static type of this call
	 */
	public OpenCall(CFG cfg, CodeLocation location, boolean instanceCall, String qualifier, String targetName,
			EvaluationOrder order, Type staticType, Expression... parameters) {
		// if a call is open we don't really care if it's instance or not and we
		// will never perform parameter assignment
		super(cfg, location, PythonLikeAssigningStrategy.INSTANCE, instanceCall, qualifier, targetName, order,
				staticType,
				parameters);
	}

	/**
	 * Creates an open call as the resolved version of the given {@code source}
	 * call, copying all its data.
	 * 
	 * @param source the unresolved call to copy
	 */
	public OpenCall(UnresolvedCall source) {
		this(source.getCFG(), source.getLocation(), source.isInstanceCall(), source.getQualifier(),
				source.getTargetName(), source.getOrder(), source.getStaticType(), source.getParameters());
	}

	@Override
	public String toString() {
		return "[open] " + super.toString();
	}

	@Override
	public final Identifier getMetaVariable() {
		return new Variable(getStaticType(), "open_call_ret_value@" + getLocation(), getLocation());
	}

	@Override
	protected <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> compute(
					AnalysisState<A, H, V, T> entryState,
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					StatementStore<A, H, V, T> expressions,
					ExpressionSet<SymbolicExpression>[] parameters)
					throws SemanticException {
		return interprocedural.getAbstractResultOf(this, entryState, parameters, expressions);
	}
}
