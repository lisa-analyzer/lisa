package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A call to one or more {@link CodeMember}s under analysis, implemented through
 * a sequence of calls.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MultiCall
		extends
		Call
		implements
		ResolvedCall {

	/**
	 * The underlying calls
	 */
	private final Collection<Call> calls;

	/**
	 * Creates a multi call as the resolved version of the given {@code source}
	 * call, copying all its data.
	 * 
	 * @param source the unresolved call to copy
	 * @param calls  the calls underlying this one
	 */
	public MultiCall(
			UnresolvedCall source,
			Call... calls) {
		super(
				source.getCFG(),
				source.getLocation(),
				source.getCallType(),
				source.getQualifier(),
				source.getTargetName(),
				source.getOrder(),
				getCommonReturnType(calls),
				source.getParameters());
		Objects.requireNonNull(calls, "The calls underlying a multi call cannot be null");
		for (Call target : calls) {
			Objects.requireNonNull(target, "A call underlying a multi call cannot be null");
			if (!(target instanceof ResolvedCall))
				throw new IllegalArgumentException(target + " has not been resolved yet");
		}
		this.calls = List.of(calls);
	}

	private static Type getCommonReturnType(
			Call... targets) {
		Type result = null;
		for (Call c : targets) {
			Type current = c.getStaticType();
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
	public boolean equals(
			Object obj) {
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
	protected int compareCallAux(
			Call o) {
		MultiCall other = (MultiCall) o;
		int cmp;
		if ((cmp = Integer.compare(calls.size(), other.calls.size())) != 0)
			return cmp;
		List<Call> l1 = new LinkedList<>(calls);
		List<Call> l2 = new LinkedList<>(other.calls);
		for (int i = 0; i < l1.size(); i++)
			if ((cmp = l1.get(i).compareTo(l2.get(i))) != 0)
				return cmp;
		return 0;
	}

	@Override
	public String toString() {
		return "[multi] " + super.toString();
	}

	@Override
	public void setSource(
			UnresolvedCall source) {
		super.setSource(source);
		calls.forEach(c -> c.setSource(source));
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		AnalysisState<A> result = state.bottom();

		for (Call call : calls) {
			result = result.lub(call.forwardSemanticsAux(interprocedural, state, params, expressions));
			getMetaVariables().addAll(call.getMetaVariables());
		}

		return result;
	}

	@Override
	public Collection<CodeMember> getTargets() {
		return calls.stream()
				.map(ResolvedCall.class::cast)
				.flatMap(c -> c.getTargets().stream())
				.collect(Collectors.toSet());
	}

}
