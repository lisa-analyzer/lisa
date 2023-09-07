package it.unive.lisa.program.cfg.statement.numeric;

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
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

/**
 * An expression modeling the division operation ({@code /}). Both operands'
 * types must be instances of {@link NumericType}. The type of this expression
 * is the common numerical type of its operands, according to the type
 * inference.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Division extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the division.
	 * 
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public Division(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "/", left, right);
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> binarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		TypeSystem types = getProgram().getTypes();
		if (left.getRuntimeTypes(types).stream().noneMatch(Type::isNumericType))
			return state.bottom();
		if (right.getRuntimeTypes(types).stream().noneMatch(Type::isNumericType))
			return state.bottom();

		return state.smallStepSemantics(
				new BinaryExpression(
						getStaticType(),
						left,
						right,
						NumericNonOverflowingDiv.INSTANCE,
						getLocation()),
				this);
	}
}
