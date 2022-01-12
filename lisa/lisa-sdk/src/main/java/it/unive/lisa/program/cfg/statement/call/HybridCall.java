package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A call to one or more {@link CFG}s and/or {@link NativeCFG}s under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HybridCall extends Call {

	/**
	 * The targets of this call
	 */
	private final Collection<CFG> targets;

	/**
	 * The native targets of this call
	 */
	private final Collection<NativeCFG> nativeTargets;

	/**
	 * Builds the hybrid call, happening at the given location in the program.
	 * The {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}. The static type of this call is the common
	 * supertype of the return types of all targets.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where this expression is defined within
	 *                          the program
	 * @param qualifiedName the qualified name of the static target of this call
	 * @param targets       the CFGs that are targeted by this CFG call
	 * @param nativeTargets the NativeCFGs that are targeted by this CFG call
	 * @param parameters    the parameters of this call
	 */
	public HybridCall(CFG cfg, CodeLocation location, String qualifiedName, Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets, Expression... parameters) {
		super(cfg, location, qualifiedName, getCommonReturnType(targets, nativeTargets), parameters);
		Objects.requireNonNull(targets, "The targets of a hybrid call cannot be null");
		Objects.requireNonNull(nativeTargets, "The native targets of a hybrid call cannot be null");
		for (CFG target : targets)
			Objects.requireNonNull(target, "A target of a hybrid call cannot be null");
		for (NativeCFG target : nativeTargets)
			Objects.requireNonNull(target, "A native target of a hybrid call cannot be null");
		this.targets = targets;
		this.nativeTargets = nativeTargets;
	}

	/**
	 * Builds the hybrid call, happening at the given location in the program.
	 * The static type of this call is the common supertype of the return types
	 * of all targets.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where this expression is defined within
	 *                          the program
	 * @param qualifiedName the qualified name of the static target of this call
	 * @param order         the evaluation order of the sub-expressions
	 * @param targets       the CFGs that are targeted by this CFG call
	 * @param nativeTargets the NativeCFGs that are targeted by this CFG call
	 * @param parameters    the parameters of this call
	 */
	public HybridCall(CFG cfg, CodeLocation location, String qualifiedName, EvaluationOrder order,
			Collection<CFG> targets, Collection<NativeCFG> nativeTargets, Expression... parameters) {
		super(cfg, location, qualifiedName, order, getCommonReturnType(targets, nativeTargets), parameters);
		Objects.requireNonNull(targets, "The targets of a hybrid call cannot be null");
		Objects.requireNonNull(nativeTargets, "The native targets of a hybrid call cannot be null");
		for (CFG target : targets)
			Objects.requireNonNull(target, "A target of a hybrid call cannot be null");
		for (NativeCFG target : nativeTargets)
			Objects.requireNonNull(target, "A native target of a hybrid call cannot be null");
		this.targets = targets;
		this.nativeTargets = nativeTargets;
	}

	private static Type getCommonReturnType(Collection<CFG> targets, Collection<NativeCFG> nativeTargets) {
		Iterator<CFG> it = targets.iterator();
		Type result = null;
		while (it.hasNext()) {
			Type current = it.next().getDescriptor().getReturnType();
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

		Iterator<NativeCFG> it1 = nativeTargets.iterator();
		while (it1.hasNext()) {
			Type current = it1.next().getDescriptor().getReturnType();
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
	 * Yields the CFGs that are targeted by this CFG call.
	 * 
	 * @return the target CFG
	 */
	public Collection<CFG> getTargets() {
		return targets;
	}

	/**
	 * Yields the CFGs that are targeted by this CFG call.
	 * 
	 * @return the target CFG
	 */
	public Collection<NativeCFG> getNativeTargets() {
		return nativeTargets;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((nativeTargets == null) ? 0 : nativeTargets.hashCode());
		result = prime * result + ((targets == null) ? 0 : targets.hashCode());
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
		HybridCall other = (HybridCall) obj;
		if (nativeTargets == null) {
			if (other.nativeTargets != null)
				return false;
		} else if (!nativeTargets.equals(other.nativeTargets))
			return false;
		if (targets == null) {
			if (other.targets != null)
				return false;
		} else if (!targets.equals(other.targets))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + (targets.size() + nativeTargets.size()) + " targets]" + getConstructName() + "("
				+ StringUtils.join(getSubExpressions(), ", ") + ")";
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> expressionSemantics(
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> state,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException {
		AnalysisState<A, H, V> result = state.bottom();

		Expression[] parameters = getSubExpressions();
		if (!targets.isEmpty()) {
			CFGCall cfgcall = new CFGCall(getCFG(), getLocation(), getConstructName(), targets, parameters);
			cfgcall.setRuntimeTypes(getRuntimeTypes());
			cfgcall.setSource(getSource());
			result = cfgcall.expressionSemantics(interprocedural, state, params);
			getMetaVariables().addAll(cfgcall.getMetaVariables());
		}

		for (NativeCFG nat : nativeTargets)
			try {
				NaryExpression rewritten = nat.rewrite(this, parameters);
				result = result.lub(rewritten.expressionSemantics(interprocedural, state, params));
				getMetaVariables().addAll(rewritten.getMetaVariables());
			} catch (CallResolutionException e) {
				throw new SemanticException("Unable to resolve call " + this, e);
			}

		return result;
	}
}
