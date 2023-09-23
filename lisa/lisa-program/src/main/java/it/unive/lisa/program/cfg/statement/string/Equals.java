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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;

/**
 * An expression modeling the string equals operation. The type of both operands
 * must be {@link StringType}. The type of this expression is the
 * {@link BooleanType}. <br>
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
public class Equals extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Statement that has been rewritten to this operation, if any. This is to
	 * accomodate the fact that, in most languages, string operations are
	 * performed through calls, and one might want to provide the semantics of
	 * those calls through {@link NativeCFG} that rewrites to instances of this
	 * class.
	 */
	protected Statement originating;

	/**
	 * Builds the equals.
	 * 
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the code location where this operation is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public Equals(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "equals", cfg.getDescriptor().getUnit().getProgram().getTypes().getBooleanType(), left,
				right);
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> binarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		if (state.getState().getRuntimeTypesOf(left, this, state.getState()).stream().noneMatch(Type::isStringType))
			return state.bottom();
		if (state.getState().getRuntimeTypesOf(right, this, state.getState()).stream().noneMatch(Type::isStringType))
			return state.bottom();

		return state.smallStepSemantics(
				new BinaryExpression(
						getStaticType(),
						left,
						right,
						StringEquals.INSTANCE,
						getLocation()),
				originating == null ? this : originating);
	}
}
