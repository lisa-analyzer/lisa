package it.unive.lisa.program.cfg.statement.comparison;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;

/**
 * An expression modeling the less or equal operation ({@code <=}). Both
 * operands' types must be instances of {@link NumericType}. The type of this
 * expression is the {@link BooleanType}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LessOrEqual extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the less or equal.
	 * 
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public LessOrEqual(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "<=", cfg.getDescriptor().getUnit().getProgram().getTypes().getBooleanType(), left, right);
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> binaryFwdSemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		if (state.getState().getRuntimeTypesOf(left, this, state.getState()).stream().noneMatch(Type::isNumericType))
			return state.bottom();
		if (state.getState().getRuntimeTypesOf(right, this, state.getState()).stream().noneMatch(Type::isNumericType))
			return state.bottom();

		return state.smallStepSemantics(
				new BinaryExpression(
						getStaticType(),
						left,
						right,
						ComparisonLe.INSTANCE,
						getLocation()),
				this);
	}
}
