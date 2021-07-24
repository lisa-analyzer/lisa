package it.unive.lisa.program.cfg.statement;

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
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A call to one or more {@link CFG}s or {@link NativeCFG}s under analysis.
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
	 * The qualified name of the static target of this call
	 */
	private final String qualifiedName;

	/**
	 * Builds the CFG call, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where this expression is defined within
	 *                          the source file. If unknown, use {@code null}
	 * @param qualifiedName the qualified name of the static target of this call
	 * @param targets       the CFGs that are targeted by this CFG call
	 * @param nativeTargets the NativeCFGs that are targeted by this CFG call
	 * @param parameters    the parameters of this call
	 */
	public HybridCall(CFG cfg, CodeLocation location, String qualifiedName, Collection<CFG> targets,
			Collection<NativeCFG> nativeTargets, Expression... parameters) {
		super(cfg, location, getCommonReturnType(targets), parameters);
		Objects.requireNonNull(qualifiedName, "The qualified name of the static target of a CFG call cannot be null");
		Objects.requireNonNull(targets, "The targets of a CFG call cannot be null");
		Objects.requireNonNull(nativeTargets, "The native targets of a CFG call cannot be null");
		for (CFG target : targets)
			Objects.requireNonNull(target, "A target of a CFG call cannot be null");
		this.targets = targets;
		this.nativeTargets = nativeTargets;
		this.qualifiedName = qualifiedName;
	}

	private static Type getCommonReturnType(Collection<CFG> targets) {
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

	/**
	 * Yields the qualified name of the static target of this call.
	 * 
	 * @return the qualified name
	 */
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((nativeTargets == null) ? 0 : nativeTargets.hashCode());
		result = prime * result + ((qualifiedName == null) ? 0 : qualifiedName.hashCode());
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
		if (qualifiedName == null) {
			if (other.qualifiedName != null)
				return false;
		} else if (!qualifiedName.equals(other.qualifiedName))
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
		return "[" + targets.size() + nativeTargets.size() + " targets]" + qualifiedName + "("
				+ StringUtils.join(getParameters(), ", ") + ")";
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V>[] computedStates,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException {
		AnalysisState<A, H, V> result = entryState.bottom();

		if (!targets.isEmpty()) {
			CFGCall cfgcall = new CFGCall(getCFG(), getLocation(), qualifiedName, getTargets(), getParameters());
			cfgcall.setRuntimeTypes(getRuntimeTypes());
			result = cfgcall.callSemantics(entryState, interprocedural, computedStates, params);
			getMetaVariables().addAll(cfgcall.getMetaVariables());
		}

		for (NativeCFG nat : nativeTargets)
			try {
				NativeCall rewritten = nat.rewrite(this, getParameters());
				result = result.lub(rewritten.callSemantics(entryState, interprocedural, computedStates, params));
				getMetaVariables().addAll(rewritten.getMetaVariables());
			} catch (CallResolutionException e) {
				throw new SemanticException("Unable to resolve call " + this, e);
			}

		return result;
	}
}
