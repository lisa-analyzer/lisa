package it.unive.lisa.program.cfg.statement.comparison;

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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.BoolType;

/**
 * An expression modeling the less than operation ({@code <}). Both operands'
 * types must be instances of {@link NumericType}. The type of this expression
 * is the {@link BoolType}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LessThan extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the less than.
	 * 
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public LessThan(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "<", BoolType.INSTANCE, left, right);
	}

	@Override
	protected <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> binarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		if (left.getRuntimeTypes().noneMatch(Type::isNumericType))
			return state.bottom();
		if (right.getRuntimeTypes().noneMatch(Type::isNumericType))
			return state.bottom();

		return state.smallStepSemantics(
				new BinaryExpression(
						BoolType.INSTANCE,
						left,
						right,
						ComparisonLt.INSTANCE,
						getLocation()),
				this);
	}
}
