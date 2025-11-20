package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Expression;
import java.util.Collection;

/**
 * A call that wraps another one that has been created through
 * {@link CanRemoveReceiver#removeFirstParameter()}. The purpose of this class
 * is to remove the first parameter also during the semantic computation, before
 * forwarding the call to the actual implementation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TruncatedParamsCall
		extends
		Call
		implements
		ResolvedCall {

	private final Call call;

	/**
	 * Builds the call.
	 * 
	 * @param call the call to be wrapped
	 */
	public TruncatedParamsCall(
			Call call) {
		super(
				call.getCFG(),
				call.getLocation(),
				call.getCallType(),
				call.getQualifier(),
				call.getTargetName(),
				call.getOrder(),
				call.getStaticType(),
				call.getParameters());
		if (!(call instanceof ResolvedCall))
			throw new IllegalArgumentException("The given call has not been resolved yet");
		this.call = call;
	}

	@Override
	public void setSource(
			UnresolvedCall source) {
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
	public boolean equals(
			Object obj) {
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
	protected int compareCallAux(
			Call o) {
		return call.compareCallAux(((TruncatedParamsCall) o).call);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		Expression[] actuals = getParameters();
		AnalysisState<A> post;
		if (params.length == actuals.length) {
			post = call.forwardSemanticsAux(interprocedural, state, params, expressions);
		} else {
			ExpressionSet[] truncatedParams = new ExpressionSet[actuals.length];
			if (actuals.length > 0)
				System.arraycopy(params, 1, truncatedParams, 0, params.length - 1);
			post = call.forwardSemanticsAux(interprocedural, state, truncatedParams, expressions);
		}

		getMetaVariables().addAll(call.getMetaVariables());
		return post;
	}

	@Override
	public Collection<CodeMember> getTargets() {
		return ((ResolvedCall) call).getTargets();
	}

	/**
	 * Yields the original call that this one was created from.
	 * 
	 * @return the call
	 */
	public Call getInnerCall() {
		return call;
	}

}
