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
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A call to one or more {@link CodeMember}s under analysis, implemented through
 * a sequence of calls.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MultiCall extends Call implements ResolvedCall {

	/**
	 * The underlying calls
	 */
	private final Collection<Call> calls;

	/**
	 * Builds the multi-call, happening at the given location in the program.
	 * The {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}. The static type of this call is the common
	 * supertype of the return types of all targets.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where this expression is defined within
	 *                       the program
	 * @param callType   the call type of this call
	 * @param qualifier  the optional qualifier of the call (can be null or
	 *                       empty - see {@link #getFullTargetName()} for more
	 *                       info)
	 * @param targetName the qualified name of the static target of this call
	 * @param calls      the Calls underlying this one
	 * @param parameters the parameters of this call
	 */
	public MultiCall(CFG cfg, CodeLocation location, CallType callType, String qualifier, String targetName,
			Collection<Call> calls, Expression... parameters) {
		this(cfg, location, callType, qualifier, targetName, LeftToRightEvaluation.INSTANCE, calls, parameters);
	}

	/**
	 * Builds the multi call, happening at the given location in the program.
	 * The static type of this call is the common supertype of the return types
	 * of all targets.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where this expression is defined within
	 *                       the program
	 * @param callType   the call type of this call
	 * @param qualifier  the optional qualifier of the call (can be null or
	 *                       empty - see {@link #getFullTargetName()} for more
	 *                       info)
	 * @param targetName the qualified name of the static target of this call
	 * @param order      the evaluation order of the sub-expressions
	 * @param calls      the Calls underlying this one
	 * @param parameters the parameters of this call
	 */
	public MultiCall(CFG cfg, CodeLocation location, CallType callType, String qualifier, String targetName,
			EvaluationOrder order, Collection<Call> calls, Expression... parameters) {
		super(cfg, location, callType, qualifier, targetName, order, getCommonReturnType(calls), parameters);
		Objects.requireNonNull(calls, "The calls underlying a multi call cannot be null");
		for (Call target : calls) {
			Objects.requireNonNull(target, "A call underlying a multi call cannot be null");
			if (!(target instanceof ResolvedCall))
				throw new IllegalArgumentException(target + " has not been resolved yet");
		}
		this.calls = calls;
	}

	private static Type getCommonReturnType(Collection<Call> targets) {
		Iterator<Call> it = targets.iterator();
		Type result = null;
		while (it.hasNext()) {
			Type current = it.next().getStaticType();
			if (result == null)
				result = current;
			else if (current.canBeAssignedTo(result))
				continue;
			else if (result.canBeAssignedTo(current))
				result = current;
			else
				result = result.commonSupertype(current);

			if (current.isUntyped())
				break;
		}

		return result == null ? Untyped.INSTANCE : result;
	}

	/**
	 * Creates a multi call as the resolved version of the given {@code source}
	 * call, copying all its data.
	 * 
	 * @param source the unresolved call to copy
	 * @param calls  the calls underlying this one
	 */
	public MultiCall(UnresolvedCall source, Call... calls) {
		this(source.getCFG(), source.getLocation(), source.getCallType(), source.getQualifier(), source.getTargetName(),
				List.of(calls), source.getParameters());
		for (Expression param : source.getParameters())
			// make sure they stay linked to the original call
			param.setParentStatement(source);
	}

	/**
	 * Yields the calls underlying this multi call.
	 * 
	 * @return the calls
	 */
	public Collection<Call> getCalls() {
		return calls;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((calls == null) ? 0 : calls.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MultiCall other = (MultiCall) obj;
		if (calls == null) {
			if (other.calls != null)
				return false;
		} else if (!calls.equals(other.calls))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[multi] " + super.toString();
	}

	@Override
	public void setSource(UnresolvedCall source) {
		super.setSource(source);
		calls.forEach(c -> c.setSource(source));
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> expressionSemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					ExpressionSet<SymbolicExpression>[] params,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		AnalysisState<A, H, V, T> result = state.bottom();

		for (Call call : calls) {
			result = result.lub(call.expressionSemantics(interprocedural, state, params, expressions));
			getMetaVariables().addAll(call.getMetaVariables());
		}

		return result;
	}

	@Override
	public Collection<CodeMember> getTargets() {
		return calls.stream().map(ResolvedCall.class::cast).flatMap(c -> c.getTargets().stream())
				.collect(Collectors.toSet());
	}
}
