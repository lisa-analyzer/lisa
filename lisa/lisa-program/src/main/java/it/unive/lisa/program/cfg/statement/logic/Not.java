package it.unive.lisa.program.cfg.statement.logic;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;

/**
 * An expression modeling the logical negation ({@code !} or {@code not}). The
 * operand's type must be instance of {@link BooleanType}. The type of this
 * expression is the {@link BooleanType}.
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
	public Not(
			CFG cfg,
			CodeLocation location,
			Expression expression) {
		super(cfg, location, "!", cfg.getDescriptor().getUnit().getProgram().getTypes().getBooleanType(), expression);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0; // no extra fields to compare
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		if (state.getState().getRuntimeTypesOf(expr, this, state.getState()).stream().noneMatch(Type::isBooleanType))
			return state.bottom();

		return state.smallStepSemantics(
				new UnaryExpression(
						getStaticType(),
						expr,
						LogicalNegation.INSTANCE,
						getLocation()),
				this);
	}
}
