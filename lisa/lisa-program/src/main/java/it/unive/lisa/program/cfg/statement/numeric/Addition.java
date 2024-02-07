package it.unive.lisa.program.cfg.statement.numeric;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;

/**
 * An expression modeling the addition operation ({@code +}). Both operands'
 * types must be instances of {@link NumericType}. The type of this expression
 * is the common numerical type of its operands, according to the type
 * inference.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Addition extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the addition.
	 * 
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public Addition(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "+", left, right);
	}

	/**
	 * Builds the addition.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param location   the location where this literal is defined
	 * @param staticType the static type of this operation
	 * @param left       the left-hand side of this operation
	 * @param right      the right-hand side of this operation
	 */
	public Addition(
			CFG cfg,
			CodeLocation location,
			Type staticType,
			Expression left,
			Expression right) {
		super(cfg, location, "+", staticType, left, right);
	}

	/**
	 * Builds the addition.
	 * 
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param order    the evaluation order of the sub-expressions
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public Addition(
			CFG cfg,
			CodeLocation location,
			EvaluationOrder order,
			Expression left,
			Expression right) {
		super(cfg, location, "+", order, left, right);
	}

	/**
	 * Builds the addition.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param location   the location where this literal is defined
	 * @param order      the evaluation order of the sub-expressions
	 * @param staticType the static type of this operation
	 * @param left       the left-hand side of this operation
	 * @param right      the right-hand side of this operation
	 */
	public Addition(
			CFG cfg,
			CodeLocation location,
			EvaluationOrder order,
			Type staticType,
			Expression left,
			Expression right) {
		super(cfg, location, "+", order, staticType, left, right);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0; // no extra fields to compare
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		if (state.getState().getRuntimeTypesOf(right, this, state.getState()).stream().noneMatch(Type::isNumericType))
			return state.bottom();
		if (state.getState().getRuntimeTypesOf(right, this, state.getState()).stream().noneMatch(Type::isNumericType))
			return state.bottom();

		return state.smallStepSemantics(
				new BinaryExpression(
						getStaticType(),
						left,
						right,
						NumericNonOverflowingAdd.INSTANCE,
						getLocation()),
				this);
	}
}
