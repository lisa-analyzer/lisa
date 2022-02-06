package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.cfg.statement.call.assignment.ParameterAssigningStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An expression that can be used to for by-name parameter passing to
 * {@link Call}s. In some languages, parameters can be passed in a different
 * order from the one declared in the target procedure's signature. This is made
 * possible by prefixing the expression representing the parameter's value with
 * its name. This expression models the by-name parameter passing, and instances
 * of {@link ParameterAssigningStrategy} can use instances of this class to
 * detect which parameter is being assingned.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NamedParameterExpression extends UnaryExpression {

	private final String parameterName;

	/**
	 * Builds the expression. The static type of this expression is the one of
	 * {@code subExpression}.
	 * 
	 * @param cfg           the {@link CFG} where this operation lies
	 * @param location      the location where this literal is defined
	 * @param parameterName the name of the parameter being assigned here
	 * @param subExpression the expression being assigned to the target
	 *                          parameter
	 */
	public NamedParameterExpression(CFG cfg, CodeLocation location, String parameterName, Expression subExpression) {
		super(cfg, location, parameterName + "=", subExpression.getStaticType(), subExpression);
		this.parameterName = parameterName;
	}

	/**
	 * Yields the name of the parameter targeted by this expression.
	 * 
	 * @return the name of the parameter
	 */
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
	protected <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> unarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					SymbolicExpression expr,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		return state.smallStepSemantics(expr, this);
	}
}
