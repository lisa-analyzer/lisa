package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.type.Type;
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
	protected Call(CFG cfg, CodeLocation location, CallType type,
			String qualifier, String targetName, EvaluationOrder order, Type staticType, Expression... parameters) {
		super(cfg, location, completeName(qualifier, targetName), order, staticType, parameters);
		Objects.requireNonNull(targetName, "The name of the target of a call cannot be null");
		this.targetName = targetName;
		this.qualifier = qualifier;
		this.callType = type;
	}

	private static String completeName(String qualifier, String name) {
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
	public void setSource(UnresolvedCall source) {
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
	public boolean equals(Object obj) {
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

	/**
	 * Yields an array containing the runtime types of the parameters of this
	 * call, retrieved by accessing the given {@link StatementStore}.
	 * 
	 * @param <A>         the type of {@link AbstractState}
	 * @param <H>         the type of the {@link HeapDomain}
	 * @param <V>         the type of the {@link ValueDomain}
	 * @param <T>         the type of {@link TypeDomain}
	 * @param expressions the store containing the computed states for the
	 *                        parameters
	 * 
	 * @return the array of parameter types
	 */
	@SuppressWarnings("unchecked")
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> Set<Type>[] parameterTypes(StatementStore<A, H, V, T> expressions) {
		Expression[] actuals = getParameters();
		Set<Type>[] types = new Set[actuals.length];
		for (int i = 0; i < actuals.length; i++)
			types[i] = expressions.getState(actuals[i]).getDomainInstance(TypeDomain.class).getInferredRuntimeTypes();
		return types;
	}
}
