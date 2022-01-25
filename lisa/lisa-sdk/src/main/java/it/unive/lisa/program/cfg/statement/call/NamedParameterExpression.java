package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class NamedParameterExpression extends UnaryExpression {

	private final String parameterName;

	public NamedParameterExpression(CFG cfg, CodeLocation location, String parameterName, Expression subExpression) {
		this(cfg, location, parameterName, Untyped.INSTANCE, subExpression);
	}

	public NamedParameterExpression(CFG cfg, CodeLocation location, String parameterName, Type staticType,
			Expression subExpression) {
		super(cfg, location, parameterName + "=", staticType, subExpression);
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return parameterName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((parameterName == null) ? 0 : parameterName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof NamedParameterExpression))
			return false;
		NamedParameterExpression other = (NamedParameterExpression) obj;
		if (parameterName == null) {
			if (other.parameterName != null)
				return false;
		} else if (!parameterName.equals(other.parameterName))
			return false;
		return true;
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> unarySemantics(
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> state,
					SymbolicExpression expr,
					StatementStore<A, H, V> expressions)
					throws SemanticException {
		return state.smallStepSemantics(expr, this);
	}
}
