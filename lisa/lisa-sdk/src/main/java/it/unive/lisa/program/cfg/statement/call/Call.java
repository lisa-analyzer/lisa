package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.VoidType;
import it.unive.lisa.util.collections.CollectionUtilities;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * A call to another cfg.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Call extends NaryExpression {

	/**
	 * Possible types of a call, identifying the type of targets (instance or
	 * static) it can have.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public enum CallType {

		/**
		 * Only instance code members are targeted by the call. The first
		 * parameter of the call is the receiver.
		 */
		INSTANCE,
		/**
		 * Only non-instance code members are targeted by the call. The call has
		 * no receiver, and the optional qualifier can be used to restrict the
		 * possible targets representing the name of the unit where the target
		 * is defined.
		 */
		STATIC,

		/**
		 * Both instance and non-instance code members are targeted by the call.
		 * The call should be resolved by both considering the first parameter
		 * as a receiver, and by removing the first parameter and using it as a
		 * qualifier.
		 */
		UNKNOWN
	}

	/**
	 * The original {@link UnresolvedCall} that has been resolved to this one
	 */
	private UnresolvedCall source = null;

	/**
	 * An optional qualifier for the call.
	 */
	private final String qualifier;

	/**
	 * The name of the target of the call.
	 */
	private final String targetName;

	/**
	 * The call type of this call.
	 */
	private final CallType callType;

	/**
	 * Builds a call happening at the given source location.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where the expression is defined within the
	 *                       program
	 * @param type       the call type of this call
	 * @param qualifier  the optional qualifier of the call (can be null or
	 *                       empty - see {@link #getFullTargetName()} for more
	 *                       info)
	 * @param targetName the name of the target of this call
	 * @param order      the evaluation order of the sub-expressions
	 * @param staticType the static type of this call
	 * @param parameters the parameters of this call
	 */
	protected Call(
			CFG cfg,
			CodeLocation location,
			CallType type,
			String qualifier,
			String targetName,
			EvaluationOrder order,
			Type staticType,
			Expression... parameters) {
		super(cfg, location, completeName(qualifier, targetName), order, staticType, parameters);
		Objects.requireNonNull(targetName, "The name of the target of a call cannot be null");
		this.targetName = targetName;
		this.qualifier = qualifier;
		this.callType = type;
	}

	private static String completeName(
			String qualifier,
			String name) {
		return StringUtils.isNotBlank(qualifier) ? qualifier + "::" + name : name;
	}

	/**
	 * Yields the call that this call originated from, if any. A call <i>r</i>
	 * originates from a call <i>u</i> if:
	 * <ul>
	 * <li><i>u</i> is an {@link UnresolvedCall}, while <i>r</i> is not,
	 * and</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to <i>r</i>, or</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to a call <i>c</i> (e.g. a
	 * {@link MultiCall}), and its semantics generated the call <i>u</i></li>
	 * </ul>
	 * 
	 * @return the call that this one originated from
	 */
	public final UnresolvedCall getSource() {
		return source;
	}

	/**
	 * Yields the full name of the target of the call. The full name of the
	 * target of a call follows the following structure:
	 * {@code qualifier::targetName}, where {@code qualifier} is optional and,
	 * when it is not present (i.e. null or empty), the {@code ::} are omitted.
	 * This cfg returns is an alias of {@link #getConstructName()}.
	 * 
	 * @return the full name of the target of the call
	 */
	public String getFullTargetName() {
		return getConstructName();
	}

	/**
	 * Yields the name of the target of the call. The full name of the target of
	 * a call follows the following structure: {@code qualifier::targetName},
	 * where {@code qualifier} is optional and, when it is not present (i.e.
	 * null or empty), the {@code ::} are omitted.
	 * 
	 * @return the name of the target of the call
	 */
	public String getTargetName() {
		return targetName;
	}

	/**
	 * Yields the optional qualifier of the target of the call. The full name of
	 * the target of a call follows the following structure:
	 * {@code qualifier::targetName}, where {@code qualifier} is optional and,
	 * when it is not present (i.e. null or empty), the {@code ::} are omitted.
	 * 
	 * @return the qualifier of the target of the call
	 */
	public String getQualifier() {
		return qualifier;
	}

	/**
	 * Yields the parameters of this call. This is a shortcut to invoke
	 * {@link #getSubExpressions()}.
	 * 
	 * @return the parameters of this call
	 */
	public final Expression[] getParameters() {
		return getSubExpressions();
	}

	/**
	 * Yields the call type of this call.
	 * 
	 * @return the call type
	 */
	public CallType getCallType() {
		return callType;
	}

	/**
	 * Sets the call that this call originated from. A call <i>r</i> originates
	 * from a call <i>u</i> if:
	 * <ul>
	 * <li><i>u</i> is an {@link UnresolvedCall}, while <i>r</i> is not,
	 * and</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to <i>r</i>, or</li>
	 * <li>a {@link CallGraph} resolved <i>u</i> to a call <i>c</i> (e.g. a
	 * {@link MultiCall}), and its semantics generated the call <i>u</i></li>
	 * </ul>
	 * 
	 * @param source the call that this one originated from
	 */
	public void setSource(
			UnresolvedCall source) {
		this.source = source;
	}

	@Override
	public String toString() {
		return getConstructName() + "(" + StringUtils.join(getParameters(), ", ") + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
		result = prime * result + ((targetName == null) ? 0 : targetName.hashCode());
		result = prime * result + ((callType == null) ? 0 : callType.hashCode());
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
		Call other = (Call) obj;
		if (qualifier == null) {
			if (other.qualifier != null)
				return false;
		} else if (!qualifier.equals(other.qualifier))
			return false;
		if (targetName == null) {
			if (other.targetName != null)
				return false;
		} else if (!targetName.equals(other.targetName))
			return false;
		if (callType != other.callType)
			return false;
		return true;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		Call other = (Call) o;
		int cmp;
		if ((cmp = targetName.compareTo(other.targetName)) != 0)
			return cmp;
		if ((cmp = CollectionUtilities.nullSafeCompare(true, qualifier, other.qualifier, String::compareTo)) != 0)
			return cmp;
		if ((cmp = callType.compareTo(other.callType)) != 0)
			return cmp;
		return compareCallAux(other);
	}

	/**
	 * Auxiliary method for {@link #compareTo(Statement)} that can safely assume
	 * that the two calls happen at the same {@link CodeLocation}, are instances
	 * of the same class, have the same parameters according to their
	 * implementation of {@link #compareTo(Statement)}, and have all fields
	 * defined in the {@link Call} class equal according to their
	 * {@link Comparable#compareTo(Object)}. This method is thus responsible for
	 * only comparing the implementation-specific fields.
	 * 
	 * @param o the other call
	 * 
	 * @return a negative integer, zero, or a positive integer as this object is
	 *             less than, equal to, or greater than the specified object
	 */
	protected abstract int compareCallAux(
			Call o);

	/**
	 * Yields an array containing the runtime types of the parameters of this
	 * call, retrieved by accessing the given {@link StatementStore}.
	 * 
	 * @param <A>         the kind of {@link AbstractLattice} produced by the
	 *                        domain {@code D}
	 * @param <D>         the kind of {@link AbstractDomain} to run during the
	 *                        analysis
	 * @param expressions the store containing the computed states for the
	 *                        parameters
	 * @param analysis    the {@link Analysis} that is being executed
	 * 
	 * @return the array of parameter types
	 * 
	 * @throws SemanticException if an exception happens while retrieving the
	 *                               types
	 */
	@SuppressWarnings("unchecked")
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Set<Type>[] parameterTypes(
			StatementStore<A> expressions,
			Analysis<A, D> analysis)
			throws SemanticException {
		Expression[] actuals = getParameters();
		Set<Type>[] types = new Set[actuals.length];
		for (int i = 0; i < actuals.length; i++) {
			AnalysisState<A> state = expressions.getState(actuals[i]);
			Set<Type> t = new HashSet<>();
			for (SymbolicExpression e : state.getComputedExpressions())
				t.addAll(analysis.getRuntimeTypesOf(state, e, this));
			types[i] = t;
		}
		return types;
	}

	/**
	 * Assuming that the result of executing this call is {@code returned}, this
	 * method yields whether or not this call returned no value or its return
	 * type is {@link VoidType}. If this method returns {@code true}, then no
	 * value should be assigned to the call's meta variable.
	 * 
	 * @param <A>      the type of {@link AbstractDomain} in the analysis state
	 * @param returned the post-state of the call
	 * 
	 * @return {@code true} if that condition holds
	 */
	public <A extends AbstractLattice<A>> boolean returnsVoid(
			AnalysisState<A> returned) {
		if (getStaticType().isVoidType())
			return true;

		if (!getStaticType().isUntyped())
			return false;

		if (this instanceof CFGCall) {
			CFGCall cfgcall = (CFGCall) this;
			Collection<CFG> targets = cfgcall.getTargetedCFGs();
			if (!targets.isEmpty())
				return !targets.iterator()
					.next()
					.getNormalExitpoints()
					.stream()
					// returned values will be stored in meta variables
					.anyMatch(st -> st instanceof MetaVariableCreator);
		}

		if (this instanceof NativeCall) {
			NativeCall nativecall = (NativeCall) this;
			Collection<NativeCFG> targets = nativecall.getTargetedConstructs();
			if (!targets.isEmpty())
				// native cfgs will always rewrite to expressions and return a
				// value
				return false;
		}

		if (this instanceof TruncatedParamsCall)
			return ((TruncatedParamsCall) this).getInnerCall().returnsVoid(returned);

		if (this instanceof MultiCall) {
			MultiCall multicall = (MultiCall) this;
			Collection<Call> targets = multicall.getCalls();
			if (!targets.isEmpty())
				// we get the return type from one of its targets
				return targets.iterator().next().returnsVoid(returned);
		}

		if (returned != null)
			if (returned.getComputedExpressions().isEmpty())
				return true;
			else if (returned.getComputedExpressions().size() == 1
					&& returned.getComputedExpressions().iterator().next() instanceof Skip)
				return true;

		return false;
	}

}
