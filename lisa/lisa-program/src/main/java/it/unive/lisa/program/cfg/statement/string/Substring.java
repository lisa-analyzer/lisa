package it.unive.lisa.program.cfg.statement.string;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

/**
 * An expression modeling the string substring operation. The type of the first
 * operand must be {@link StringType}, while the other two operands' types must
 * be {@link NumericType}. The type of this expression is the
 * {@link StringType}. <br>
 * <br>
 * Since in most languages string operations are provided through calls to
 * library functions, this class contains a field whose purpose is to optionally
 * store a {@link Statement} that is rewritten to an instance of this class
 * (i.e., a call to a {@link NativeCFG} modeling the library function). If
 * present, such statement will be used as {@link ProgramPoint} for semantics
 * computations. This allows subclasses to implement {@link PluggableStatement}
 * easily without redefining the semantics provided by this class.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Substring extends it.unive.lisa.program.cfg.statement.TernaryExpression {

	/**
	 * Statement that has been rewritten to this operation, if any. This is to
	 * accomodate the fact that, in most languages, string operations are
	 * performed through calls, and one might want to provide the semantics of
	 * those calls through {@link NativeCFG} that rewrites to instances of this
	 * class.
	 */
	protected Statement originating;

	/**
	 * Builds the substring.
	 * 
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param left     the left-hand side of this operation
	 * @param middle   the middle operand of this operation
	 * @param right    the right-hand side of this operation
	 */
	public Substring(CFG cfg, CodeLocation location, Expression left, Expression middle, Expression right) {
		super(cfg, location, "substring", cfg.getDescriptor().getUnit().getProgram().getTypes().getStringType(), left,
				middle, right);
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> ternarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		TypeSystem types = getProgram().getTypes();
		if (left.getRuntimeTypes(types).stream().noneMatch(Type::isStringType))
			return state.bottom();
		if (middle.getRuntimeTypes(types).stream().noneMatch(Type::isNumericType))
			return state.bottom();
		if (right.getRuntimeTypes(types).stream().noneMatch(Type::isNumericType))
			return state.bottom();

		return state.smallStepSemantics(
				new TernaryExpression(
						getStaticType(),
						left,
						middle,
						right,
						StringSubstring.INSTANCE,
						getLocation()),
				originating == null ? this : originating);
	}
}
