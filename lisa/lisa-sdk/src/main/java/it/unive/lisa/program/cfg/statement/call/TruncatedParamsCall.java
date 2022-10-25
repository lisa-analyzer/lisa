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
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Collection;

/**
 * A call that wraps another one that has been created through
 * {@link CanRemoveReceiver#removeFirstParameter()}. The purpose of this class
 * is to remove the first parameter also during the semantic computation, before
 * forwarding the call to the actual implementation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TruncatedParamsCall extends Call implements ResolvedCall {

	private final Call call;

	/**
	 * Builds the call.
	 * 
	 * @param call the call to be wrapped
	 */
	public TruncatedParamsCall(Call call) {
		super(call.getCFG(), call.getLocation(), call.getCallType(), call.getQualifier(), call.getTargetName(),
				call.getOrder(), call.getStaticType(), call.getParameters());
		if (!(call instanceof ResolvedCall))
			throw new IllegalArgumentException("The given call has not been resolved yet");
		this.call = call;
	}

	@Override
	public int setOffset(int offset) {
		// we do not reset the offsets here
		Expression[] params = getParameters();
		return params[params.length - 1].getOffset();
	}

	@Override
	public void setSource(UnresolvedCall source) {
		super.setSource(source);
		call.setSource(source);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((call == null) ? 0 : call.hashCode());
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
		TruncatedParamsCall other = (TruncatedParamsCall) obj;
		if (call == null) {
			if (other.call != null)
				return false;
		} else if (!call.equals(other.call))
			return false;
		return true;
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
		Expression[] actuals = getParameters();
		AnalysisState<A, H, V, T> post;
		if (params.length == actuals.length) {
			post = call.expressionSemantics(interprocedural, state, params, expressions);
		} else {
			@SuppressWarnings("unchecked")
			ExpressionSet<SymbolicExpression>[] truncatedParams = new ExpressionSet[actuals.length];
			if (actuals.length > 0)
				System.arraycopy(params, 1, truncatedParams, 0, params.length - 1);
			post = call.expressionSemantics(interprocedural, state, truncatedParams, expressions);
		}

		getMetaVariables().addAll(call.getMetaVariables());
		return post;
	}

	@Override
	public Collection<CodeMember> getTargets() {
		return ((ResolvedCall) call).getTargets();
	}
}
