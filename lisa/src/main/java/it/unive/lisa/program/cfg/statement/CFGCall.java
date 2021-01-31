package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueIdentifier;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A call to one or more of the CFGs under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGCall extends Call implements MetaVariableCreator {

	/**
	 * The targets of this call
	 */
	private final Collection<CFG> targets;

	/**
	 * The qualified name of the static target of this call
	 */
	private final String qualifiedName;

	/**
	 * Builds the CFG call. The location where this call happens is unknown
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param qualifiedName the qualified name of the static target of this call
	 * @param target        the CFG that is targeted by this CFG call.
	 * @param parameters    the parameters of this call
	 */
	public CFGCall(CFG cfg, String qualifiedName, CFG target, Expression... parameters) {
		this(cfg, null, -1, -1, qualifiedName, target, parameters);
	}

	/**
	 * Builds the CFG call. The location where this call happens is unknown
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param qualifiedName the qualified name of the static target of this call
	 * @param targets       the CFGs that are targeted by this CFG call.
	 * @param parameters    the parameters of this call
	 */
	public CFGCall(CFG cfg, String qualifiedName, Collection<CFG> targets, Expression... parameters) {
		this(cfg, null, -1, -1, qualifiedName, targets, parameters);
	}

	/**
	 * Builds the CFG call, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param sourceFile    the source file where this expression happens. If
	 *                          unknown, use {@code null}
	 * @param line          the line number where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param col           the column where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param qualifiedName the qualified name of the static target of this call
	 * @param target        the CFG that is targeted by this CFG call
	 * @param parameters    the parameters of this call
	 */
	public CFGCall(CFG cfg, String sourceFile, int line, int col, String qualifiedName, CFG target,
			Expression... parameters) {
		this(cfg, sourceFile, line, col, qualifiedName, Collections.singleton(target), parameters);
	}

	/**
	 * Builds the CFG call, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param sourceFile    the source file where this expression happens. If
	 *                          unknown, use {@code null}
	 * @param line          the line number where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param col           the column where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param qualifiedName the qualified name of the static target of this call
	 * @param targets       the CFGs that are targeted by this CFG call
	 * @param parameters    the parameters of this call
	 */
	public CFGCall(CFG cfg, String sourceFile, int line, int col, String qualifiedName, Collection<CFG> targets,
			Expression... parameters) {
		super(cfg, sourceFile, line, col, getCommonReturnType(targets), parameters);
		Objects.requireNonNull(qualifiedName, "The qualified name of the static target of a CFG call cannot be null");
		Objects.requireNonNull(targets, "The targets of a CFG call cannot be null");
		for (CFG target : targets)
			Objects.requireNonNull(target, "A target of a CFG call cannot be null");
		this.targets = targets;
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

		return result;
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
		result = prime * result + ((targets == null) ? 0 : targets.hashCode());
		return result;
	}

	@Override
	public boolean isEqualTo(Statement st) {
		if (this == st)
			return true;
		if (getClass() != st.getClass())
			return false;
		if (!super.isEqualTo(st))
			return false;
		CFGCall other = (CFGCall) st;
		if (targets == null) {
			if (other.targets != null)
				return false;
		} else if (!targets.equals(other.targets))
			return false;
		if (qualifiedName == null) {
			if (other.qualifiedName != null)
				return false;
		} else if (!qualifiedName.equals(other.qualifiedName))
			return false;
		return super.isEqualTo(other);
	}

	@Override
	public String toString() {
		return "[" + targets.size() + " targets]" + qualifiedName + "(" + StringUtils.join(getParameters(), ", ") + ")";
	}

	@Override
	public final Identifier getMetaVariable() {
		return new ValueIdentifier(getRuntimeTypes(), "call_ret_value@" + offset);
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState, CallGraph callGraph, AnalysisState<A, H, V>[] computedStates,
					Collection<SymbolicExpression>[] params)
					throws SemanticException {
		// it corresponds to the analysis state after the evaluation of all the
		// parameters of this call, it is the entry state if this call has no
		// parameters
		// (the semantics of this call does not need information about the
		// intermediate analysis states)
		AnalysisState<A, H,
				V> lastPostState = computedStates.length == 0 ? entryState : computedStates[computedStates.length - 1];

		// this will contain only the information about the returned
		// metavariable
		AnalysisState<A, H, V> returned = callGraph.getAbstractResultOf(this, lastPostState, params);
		// the lub will include the metavariable inside the state
		AnalysisState<A, H, V> lub = lastPostState.lub(returned);

		if (getStaticType().isVoidType())
			// no need to add the meta variable since nothing has been pushed on
			// the stack
			return lub.smallStepSemantics(new Skip(), this);

		Identifier meta = getMetaVariable();
		for (SymbolicExpression expr : returned.getComputedExpressions())
			getMetaVariables().add((Identifier) expr);
		getMetaVariables().add(meta);

		AnalysisState<A, H, V> result = null;
		for (SymbolicExpression expr : lub.getComputedExpressions()) {
			AnalysisState<A, H, V> tmp = lub.assign(meta, expr, this);
			if (result == null)
				result = tmp;
			else
				result = result.lub(tmp);
		}

		return result;
	}
}
