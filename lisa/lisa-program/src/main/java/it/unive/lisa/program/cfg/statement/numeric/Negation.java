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
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

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
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param location   the location where this literal is defined
	 * @param expression the operand of this operation
	 */
	public Negation(CFG cfg, CodeLocation location, Expression expression) {
		super(cfg, location, "-", expression);
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> unarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		TypeSystem types = getProgram().getTypes();
		if (expr.getRuntimeTypes(types).stream().noneMatch(Type::isNumericType))
			return state.bottom();

		return state.smallStepSemantics(
				new UnaryExpression(
						expr.getStaticType(),
						expr,
						NumericNegation.INSTANCE,
						getLocation()),
				this);
	}
}
