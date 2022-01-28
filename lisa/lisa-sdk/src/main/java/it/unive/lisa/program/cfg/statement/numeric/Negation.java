package it.unive.lisa.program.cfg.statement.numeric;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.NumericType;

/**
 * An expression modeling the numerical negation operation ({@code -}). The
 * operand's type must be instance of {@link NumericType}. The type of this
 * expression is the same as the one of its operand.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Negation extends it.unive.lisa.program.cfg.statement.UnaryExpression {

	/**
	 * Builds the numerical negation.
	 * 
	 * @param cfg        the {@link ImplementedCFG} where this operation lies
	 * @param location   the location where this literal is defined
	 * @param expression the operand of this operation
	 */
	public Negation(ImplementedCFG cfg, CodeLocation location, Expression expression) {
		super(cfg, location, "-", expression);
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
		// we allow untyped for the type inference phase
		if (!expr.getDynamicType().isNumericType() && !expr.getDynamicType().isUntyped())
			return state.bottom();

		return state.smallStepSemantics(
				new UnaryExpression(
						expr.getTypes(),
						expr,
						NumericNegation.INSTANCE,
						getLocation()),
				this);
	}
}
