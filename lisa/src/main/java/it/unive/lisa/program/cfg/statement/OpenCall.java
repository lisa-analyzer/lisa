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
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A call to a CFG that is not under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class OpenCall extends Call implements MetaVariableCreator {

	/**
	 * The name of the target of this call
	 */
	private final String targetName;

	/**
	 * Builds the untyped open call. The location where this call happens is
	 * unknown (i.e. no source file/line/column is available). The static type
	 * of this call is {@link Untyped}.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param targetName the name of the target of this open call
	 * @param parameters the parameters of this call
	 */
	public OpenCall(CFG cfg, String targetName, Expression... parameters) {
		this(cfg, null, -1, -1, targetName, Untyped.INSTANCE, parameters);
	}

	/**
	 * Builds the open call. The location where this call happens is unknown
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param targetName the name of the target of this open call
	 * @param parameters the parameters of this call
	 * @param staticType the static type of this call
	 */
	public OpenCall(CFG cfg, String targetName, Type staticType, Expression... parameters) {
		this(cfg, null, -1, -1, targetName, staticType, parameters);
	}

	/**
	 * Builds the open call, happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param sourceFile the source file where this expression happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this expression happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this expression happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param targetName the name of the target of this open call
	 * @param parameters the parameters of this call
	 * @param staticType the static type of this call
	 */
	public OpenCall(CFG cfg, String sourceFile, int line, int col, String targetName, Type staticType,
			Expression... parameters) {
		super(cfg, sourceFile, line, col, staticType, parameters);
		Objects.requireNonNull(targetName, "The name of the target of an open call cannot be null");
		this.targetName = targetName;
	}

	/**
	 * Yields the name of the target of this open call.
	 * 
	 * @return the name of the target
	 */
	public String getTargetName() {
		return targetName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((targetName == null) ? 0 : targetName.hashCode());
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
		OpenCall other = (OpenCall) st;
		if (targetName == null) {
			if (other.targetName != null)
				return false;
		} else if (!targetName.equals(other.targetName))
			return false;
		return super.isEqualTo(other);
	}

	@Override
	public String toString() {
		return "[open call]" + targetName + "(" + StringUtils.join(getParameters(), ", ") + ")";
	}

	@Override
	public final Identifier getMetaVariable() {
		return new ValueIdentifier(getRuntimeTypes(), "open_call_ret_value@" + offset);
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState, CallGraph callGraph, AnalysisState<A, H, V>[] computedStates,
					Collection<SymbolicExpression>[] params)
					throws SemanticException {
		// TODO too coarse
		AnalysisState<A, H, V> poststate = entryState.top();

		if (getStaticType().isVoidType())
			return poststate.smallStepSemantics(new Skip(), this);
		else
			return poststate.smallStepSemantics(getMetaVariable(), this);
	}
}
