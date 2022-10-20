package it.unive.lisa.program.cfg.statement.logic;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.BoolType;

/**
 * An expression modeling the logical negation ({@code !} or {@code not}). The
 * operand's type must be instance of {@link BooleanType}. The type of this
 * expression is the {@link BoolType}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Not extends it.unive.lisa.program.cfg.statement.UnaryExpression {

	/**
	 * Builds the logical negation.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param location   the location where this literal is defined
	 * @param expression the operand of this operation
	 */
	public Not(CFG cfg, CodeLocation location, Expression expression) {
		super(cfg, location, "!", BoolType.INSTANCE, expression);
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
		if (expr.getRuntimeTypes().noneMatch(Type::isBooleanType))
			return state.bottom();

		return state.smallStepSemantics(
				new UnaryExpression(
						BoolType.INSTANCE,
						expr,
						LogicalNegation.INSTANCE,
						getLocation()),
				this);
	}
}
