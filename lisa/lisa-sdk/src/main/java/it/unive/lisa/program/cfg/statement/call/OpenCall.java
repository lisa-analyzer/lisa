package it.unive.lisa.program.cfg.statement.call;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;

/**
 * A call to a CFG that is not under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class OpenCall extends CallWithResult implements MetaVariableCreator {

	/**
	 * The name of the target of this call
	 */
	private final String targetName;

	/**
	 * Builds the open call, happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where the expression is defined within the
	 *                       source file. If unknown, use {@code null}
	 * @param targetName the name of the target of this open call
	 * @param parameters the parameters of this call
	 * @param staticType the static type of this call
	 */
	public OpenCall(CFG cfg, CodeLocation location, String targetName, Type staticType,
			Expression... parameters) {
		super(cfg, location, staticType, parameters);
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpenCall other = (OpenCall) obj;
		if (targetName == null) {
			if (other.targetName != null)
				return false;
		} else if (!targetName.equals(other.targetName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[open call]" + targetName + "(" + StringUtils.join(getParameters(), ", ") + ")";
	}

	@Override
	public final Identifier getMetaVariable() {
		return new Variable(getRuntimeTypes(), "open_call_ret_value@" + getLocation(), getLocation());
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> compute(
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> entryState,
					ExpressionSet<SymbolicExpression>[] parameters)
					throws SemanticException {
		return interprocedural.getAbstractResultOf(this, entryState, parameters);
	}
}
