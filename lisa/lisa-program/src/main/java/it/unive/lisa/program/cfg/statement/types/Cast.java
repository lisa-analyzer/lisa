package it.unive.lisa.program.cfg.statement.types;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.literal.TypeLiteral;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;

/**
 * An expression that yields its first operand, but casted or converted
 * (according to {@link Type#castIsConversion()}) to the type specified by the
 * second operand.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Cast
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the expression.
	 * 
	 * @param cfg      the cfg to which this statement belongs
	 * @param location the location in the source code where this statement
	 *                     appears
	 * @param left     the left operand, which is the expression to cast
	 * @param right    the right operand, which is a {@link TypeLiteral}
	 *                     specifying the type to cast to
	 */
	public Cast(
			CFG cfg,
			CodeLocation location,
			Expression left,
			TypeLiteral right) {
		super(cfg, location, "as", left, right);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0; // no extra fields to compare
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		if (!(right instanceof Constant) || !(((Constant) right).getValue() instanceof Type))
			return state.bottom();

		Type target = (Type) ((Constant) right).getValue();
		return interprocedural.getAnalysis().smallStepSemantics(
				state,
				new BinaryExpression(
						getProgram().getTypes().getBooleanType(),
						left,
						right,
						target.castIsConversion() ? TypeConv.INSTANCE : TypeCast.INSTANCE,
						getLocation()),
				this);
	}

}
