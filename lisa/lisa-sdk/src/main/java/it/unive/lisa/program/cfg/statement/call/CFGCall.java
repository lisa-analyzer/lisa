package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
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
	 * Builds the CFG call, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where the expression is defined within
	 *                          the source file. If unknown, use {@code null}
	 * @param qualifiedName the qualified name of the static target of this call
	 * @param target        the CFG that is targeted by this CFG call
	 * @param parameters    the parameters of this call
	 */
	public CFGCall(CFG cfg, CodeLocation location, String qualifiedName, CFG target,
			Expression... parameters) {
		this(cfg, location, qualifiedName, Collections.singleton(target), parameters);
	}

	/**
	 * Builds the CFG call, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where this expression is defined within
	 *                          the source file. If unknown, use {@code null}
	 * @param qualifiedName the qualified name of the static target of this call
	 * @param targets       the CFGs that are targeted by this CFG call
	 * @param parameters    the parameters of this call
	 */
	public CFGCall(CFG cfg, CodeLocation location, String qualifiedName, Collection<CFG> targets,
			Expression... parameters) {
		super(cfg, location, getCommonReturnType(targets), parameters);
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
		CFGCall other = (CFGCall) obj;
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
		return "[" + targets.size() + " targets]" + qualifiedName + "(" + StringUtils.join(getParameters(), ", ") + ")";
	}

	@Override
	public final Identifier getMetaVariable() {
		return new Variable(getRuntimeTypes(), "call_ret_value@" + getLocation(), getLocation());
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V>[] computedStates,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException {
		// it corresponds to the analysis state after the evaluation of all the
		// parameters of this call, it is the entry state if this call has no
		// parameters (the semantics of this call does not need information
		// about the intermediate analysis states)
		AnalysisState<A, H, V> callState = computedStates.length == 0
				? entryState
				: computedStates[computedStates.length - 1];
		// the stack has to be empty
		callState = new AnalysisState<>(callState.getState(), new ExpressionSet<>());

		// this will contain only the information about the returned
		// metavariable
		AnalysisState<A, H, V> returned = interprocedural.getAbstractResultOf(this, callState, params);

		if (getStaticType().isVoidType() ||
				(getStaticType().isUntyped() && returned.getComputedExpressions().isEmpty()) ||
				(returned.getComputedExpressions().size() == 1
						&& returned.getComputedExpressions().iterator().next() instanceof Skip))
			// no need to add the meta variable since nothing has been pushed on
			// the stack
			return returned.smallStepSemantics(new Skip(getLocation()), this);

		Identifier meta = getMetaVariable();
		for (SymbolicExpression expr : returned.getComputedExpressions())
			// It might be the case it chose a
			// target with void return type
			getMetaVariables().add((Identifier) expr);

		// propagates the annotations of the targets
		// to the metavariable of this cfg call
		for (CFG target : targets)
			for (Annotation ann : target.getDescriptor().getAnnotations())
				meta.addAnnotation(ann);

		getMetaVariables().add(meta);

		AnalysisState<A, H, V> result = returned.bottom();
		for (SymbolicExpression expr : returned.getComputedExpressions()) {
			AnalysisState<A, H, V> tmp = returned.assign(meta, expr, this);
			result = result.lub(tmp.smallStepSemantics(meta, this));
			// We need to perform this evaluation of the identifier not pushed
			// with the scope since otherwise
			// the value associated with the returned variable would be lost
		}

		return result;
	}
}
