package it.unive.lisa.cfg.statement;

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.type.Untyped;
import it.unive.lisa.symbolic.SymbolicExpression;

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
	 * The qualified name of the call target
	 */
	private final String qualifiedName;

	/**
	 * Builds the call. The location where this call happens is unknown (i.e. no
	 * source file/line/column is available).
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param qualifiedName the qualified name of the target of this call
	 * @param parameters    the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, String qualifiedName, Expression... parameters) {
		this(cfg, null, -1, -1, qualifiedName, parameters);
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
	 * @param qualifiedName the qualified name of the target of this call
	 * @param parameters    the parameters of this call
	 */
	public UnresolvedCall(CFG cfg, String sourceFile, int line, int col, String qualifiedName,
			Expression... parameters) {
		super(cfg, sourceFile, line, col, Untyped.INSTANCE, parameters);
		Objects.requireNonNull(qualifiedName, "The qualified name of an unresolved call cannot be null");
		this.qualifiedName = qualifiedName;
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
	public <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> callTypeInference(
			AnalysisState<H, TypeEnvironment> computedState, CallGraph callGraph,
			Collection<SymbolicExpression>[] params) throws SemanticException {
		Call resolved = callGraph.resolve(this);
		AnalysisState<H, TypeEnvironment> result = resolved.callTypeInference(computedState, callGraph, params);
		getMetaVariables().addAll(resolved.getMetaVariables());
		setRuntimeTypes(result.getState().getValueState().getLastComputedTypes().getRuntimeTypes());
		return result;
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
			throws SemanticException {
		Call resolved = callGraph.resolve(this);
		resolved.setRuntimeTypes(getRuntimeTypes());
		AnalysisState<H, V> result = resolved.callSemantics(computedState, callGraph, params);
		getMetaVariables().addAll(resolved.getMetaVariables());
		return result;
	}
	
	public void inheritRuntimeTypesFrom(Expression other) {
		setRuntimeTypes(other.getRuntimeTypes());
	}
}
