package it.unive.lisa.program.cfg.statement.comparison;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;

/**
 * An expression modeling the greater than operation ({@code >}). Both operands'
 * types must be instances of {@link NumericType}. The type of this expression
 * is the {@link BooleanType}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class GreaterThan
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the greater than.
	 * 
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public GreaterThan(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, ">", cfg.getDescriptor().getUnit().getProgram().getTypes().getBooleanType(), left, right);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0; // no extra fields to compare
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		if (analysis.getRuntimeTypesOf(state, left, this).stream().noneMatch(Type::isNumericType))
			return state.bottom();
		if (analysis.getRuntimeTypesOf(state, right, this).stream().noneMatch(Type::isNumericType))
			return state.bottom();

		return analysis
				.smallStepSemantics(
						state,
						new BinaryExpression(getStaticType(), left, right, ComparisonGt.INSTANCE, getLocation()),
						this);
	}

}
