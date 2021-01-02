package it.unive.lisa.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.Parameter;
import it.unive.lisa.cfg.type.Untyped;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Collection;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A call that happens inside the program to analyze. At this stage, the
 * target(s) of the call is (are) unknown. During the semantic computation, the
 * {@link CallGraph} used by the analysis will resolve this to a {@link CFGCall}
 * or to an {@link OpenCall}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UnresolvedCall extends Call {

	/**
	 * An enum defining the different types of resolution strategies for call
	 * signatures. Depending on the language, targets of calls might be resolved
	 * (at compile time or runtime) relying on the static or runtime type of
	 * their parameters. Each strategy in this enum comes with a different
	 * {@link #matches(Parameter[], Expression[])} implementation that can
	 * automatically detect if the signature of a cfg is matched by the given
	 * expressions representing the parameters for a call to that cfg.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public enum ResolutionStrategy {

		/**
		 * A strategy where the static types of the parameters of the call are
		 * evaluated against the signature of a cfg: for each parameter, if the
		 * static type of the actual parameter can be assigned to the type of
		 * the formal parameter, than
		 * {@link #matches(Parameter[], Expression[])} return {@code true}.
		 */
		STATIC_TYPES {
			@Override
			protected boolean matches(int pos, Parameter formal, Expression actual) {
				return actual.getStaticType().canBeAssignedTo(formal.getStaticType());
			}
		},

		/**
		 * A strategy where the dynamic (runtime) types of the parameters of the
		 * call are evaluated against the signature of a cfg: for each
		 * parameter, if at least one of the runtime types of the actual
		 * parameter can be assigned to the type of the formal parameter, than
		 * {@link #matches(Parameter[], Expression[])} return {@code true}.
		 */
		DYNAMIC_TYPES {
			@Override
			protected boolean matches(int pos, Parameter formal, Expression actual) {
				return actual.getRuntimeTypes().anyMatch(rt -> rt.canBeAssignedTo(formal.getStaticType()));
			}
		},

		/**
		 * A strategy where the first parameter is tested using
		 * {@link #DYNAMIC_TYPES}, while the rest is tested using
		 * {@link #STATIC_TYPES}.
		 */
		FIRST_DYNAMIC_THEN_STATIC {
			@Override
			protected boolean matches(int pos, Parameter formal, Expression actual) {
				return pos == 0 ? DYNAMIC_TYPES.matches(pos, formal, actual)
						: STATIC_TYPES.matches(pos, formal, actual);
			}
		};

		/**
		 * Yields {@code true} if and only if the signature of a cfg (i.e. the
		 * types of its parameters) is matched by the given actual parameters,
		 * according to this strategy.
		 * 
		 * @param formals the parameters definition of the cfg
		 * @param actuals the expression that are used as call parameters
		 * 
		 * @return {@code true} if and only if that condition holds
		 */
		public final boolean matches(Parameter[] formals, Expression[] actuals) {
			if (formals.length != actuals.length)
				return false;

			for (int i = 0; i < formals.length; i++)
				if (!matches(i, formals[i], actuals[i]))
					return false;

			return true;
		}

		/**
		 * Yields {@code true} if and only if the signature of the
		 * {@code pos}-th parameter of a cfg is matched by the given actual
		 * parameter, according to this strategy.
		 * 
		 * @param pos    the position of the parameter being evaluated
		 * @param formal the parameter definition of the cfg
		 * @param actual the expression that is used as parameter
		 * 
		 * @return {@code true} if and only if that condition holds
		 */
		protected abstract boolean matches(int pos, Parameter formal, Expression actual);
	}

	/**
	 * The {@link ResolutionStrategy} of the parameters of this call
	 */
	private final ResolutionStrategy strategy;

	/**
	 * The qualified name of the call target
	 */
	private final String qualifiedName;

	/**
	 * Builds the call. The location where this call happens is unknown (i.e. no
	 * source file/line/column is available).
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param strategy      the {@link ResolutionStrategy} of the parameters of
	 *                          this call
	 * @param qualifiedName the qualified name of the target of this call
	 * @param parameters    the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, ResolutionStrategy strategy, String qualifiedName, Expression... parameters) {
		this(cfg, null, -1, -1, strategy, qualifiedName, parameters);
	}

	/**
	 * Builds the CFG call, happening at the given location in the program. The
	 * static type of this CFGCall is the one return type of the descriptor of
	 * {@code target}.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param sourceFile    the source file where this expression happens. If
	 *                          unknown, use {@code null}
	 * @param line          the line number where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param col           the column where this expression happens in the
	 *                          source file. If unknown, use {@code -1}
	 * @param strategy      the {@link ResolutionStrategy} of the parameters of
	 *                          this call
	 * @param qualifiedName the qualified name of the target of this call
	 * @param parameters    the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, String sourceFile, int line, int col, ResolutionStrategy strategy,
			String qualifiedName,
			Expression... parameters) {
		super(cfg, sourceFile, line, col, Untyped.INSTANCE, parameters);
		Objects.requireNonNull(qualifiedName, "The qualified name of an unresolved call cannot be null");
		this.strategy = strategy;
		this.qualifiedName = qualifiedName;
	}

	/**
	 * Yields the {@link ResolutionStrategy} of the parameters of this call.
	 * 
	 * @return the resolution strategy
	 */
	public ResolutionStrategy getStrategy() {
		return strategy;
	}

	/**
	 * Yields the qualified name of the target of this call.
	 * 
	 * @return the qualified name of the target
	 */
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((qualifiedName == null) ? 0 : qualifiedName.hashCode());
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
		UnresolvedCall other = (UnresolvedCall) st;
		if (qualifiedName == null) {
			if (other.qualifiedName != null)
				return false;
		} else if (!qualifiedName.equals(other.qualifiedName))
			return false;
		return super.isEqualTo(other);
	}

	@Override
	public String toString() {
		return "[unresolved]" + qualifiedName + "(" + StringUtils.join(getParameters(), ", ") + ")";
	}

	@Override
	public <A extends AbstractState<A, H, TypeEnvironment>,
			H extends HeapDomain<H>> AnalysisState<A, H, TypeEnvironment> callTypeInference(
					AnalysisState<A, H, TypeEnvironment> computedState, CallGraph callGraph,
					Collection<SymbolicExpression>[] params) throws SemanticException {
		Call resolved = callGraph.resolve(this);
		AnalysisState<A, H, TypeEnvironment> result = resolved.callTypeInference(computedState, callGraph, params);
		getMetaVariables().addAll(resolved.getMetaVariables());
		setRuntimeTypes(result.getState().getValueState().getLastComputedTypes().getRuntimeTypes());
		return result;
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
					throws SemanticException {
		Call resolved = callGraph.resolve(this);
		resolved.setRuntimeTypes(getRuntimeTypes());
		AnalysisState<A, H, V> result = resolved.callSemantics(computedState, callGraph, params);
		getMetaVariables().addAll(resolved.getMetaVariables());
		return result;
	}

	/**
	 * Updates this call's runtime types to match the ones of the given
	 * expression.
	 * 
	 * @param other the expression to inherit from
	 */
	public void inheritRuntimeTypesFrom(Expression other) {
		setRuntimeTypes(other.getRuntimeTypes());
	}
}
