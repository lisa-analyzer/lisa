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
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A call to one or more of the CFGs under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGCall extends CallWithResult implements MetaVariableCreator, CanRemoveReceiver {

	/**
	 * The targets of this call
	 */
	private final Collection<CFG> targets;

	/**
	 * Builds the CFG call, happening at the given location in the program. The
	 * {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}. The static type of this call is the common
	 * supertype of the return types of all targets.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where this expression is defined within
	 *                       program
	 * @param callType   the call type of this call
	 * @param qualifier  the optional qualifier of the call (can be null or
	 *                       empty - see {@link #getFullTargetName()} for more
	 *                       info)
	 * @param targetName the qualified name of the static target of this call
	 * @param targets    the CFGs that are targeted by this CFG call
	 * @param parameters the parameters of this call
	 */
	public CFGCall(CFG cfg, CodeLocation location, CallType callType, String qualifier, String targetName,
			Collection<CFG> targets, Expression... parameters) {
		this(cfg, location, callType, qualifier, targetName, LeftToRightEvaluation.INSTANCE, targets, parameters);
	}

	/**
	 * Builds the CFG call, happening at the given location in the program. The
	 * static type of this call is the common supertype of the return types of
	 * all targets.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where this expression is defined within
	 *                       program
	 * @param callType   the call type of this call
	 * @param qualifier  the optional qualifier of the call (can be null or
	 *                       empty - see {@link #getFullTargetName()} for more
	 *                       info)
	 * @param targetName the qualified name of the static target of this call
	 * @param order      the evaluation order of the sub-expressions
	 * @param targets    the CFGs that are targeted by this CFG call
	 * @param parameters the parameters of this call
	 */
	public CFGCall(CFG cfg, CodeLocation location, CallType callType, String qualifier, String targetName,
			EvaluationOrder order, Collection<CFG> targets, Expression... parameters) {
		super(cfg, location, callType, qualifier, targetName, order, getCommonReturnType(targets), parameters);
		Objects.requireNonNull(targets, "The targets of a CFG call cannot be null");
		for (CFG target : targets)
			Objects.requireNonNull(target, "A target of a CFG call cannot be null");
		this.targets = targets;
	}

	/**
	 * Creates a cfg call as the resolved version of the given {@code source}
	 * call, copying all its data.
	 * 
	 * @param source  the unresolved call to copy
	 * @param targets the {@link CFG}s that the call has been resolved against
	 */
	public CFGCall(UnresolvedCall source, Collection<CFG> targets) {
		this(source.getCFG(), source.getLocation(), source.getCallType(), source.getQualifier(), source.getTargetName(),
				targets, source.getParameters());
		for (Expression param : source.getParameters())
			// make sure they stay linked to the original call
			param.setParentStatement(source);
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
	public Collection<CFG> getTargetedCFGs() {
		return targets;
	}

	@Override
	public Collection<CodeMember> getTargets() {
		return targets.stream().map(CodeMember.class::cast).collect(Collectors.toSet());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
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
		if (targets == null) {
			if (other.targets != null)
				return false;
		} else if (!targets.equals(other.targets))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + targets.size() + " targets] " + super.toString();
	}

	@Override
	public final Identifier getMetaVariable() {
		Variable meta = new Variable(getStaticType(), "call_ret_value@" + getLocation(), getLocation());
		// propagates the annotations of the targets
		// to the metavariable of this cfg call
		for (CFG target : targets)
			for (Annotation ann : target.getDescriptor().getAnnotations())
				meta.addAnnotation(ann);
		return meta;
	}

	@Override
	public <A extends AbstractState<A, H, V, T>,
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

	@Override
	public TruncatedParamsCall removeFirstParameter() {
		return new TruncatedParamsCall(
				new CFGCall(getCFG(), getLocation(), getCallType(), getQualifier(), getFullTargetName(), getOrder(),
						targets, CanRemoveReceiver.truncate(getParameters())));
	}
}
