package it.unive.lisa.program.cfg.statement.string;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.type.common.StringType;

/**
 * An expression modeling the string endsWith operation. The type of both
 * operands must be {@link StringType}. The type of this expression is the
 * {@link BoolType}. <br>
 * <br>
 * Since in most languages string operations are provided through calls to
 * library functions, this class contains a field {@link #originating} whose
 * purpose is to optionally store a {@link Statement} that is rewritten to an
 * instance of this class (i.e., a call to a {@link NativeCFG} modeling the
 * library function). If present, such statement will be used as
 * {@link ProgramPoint} for semantics computations. This allows subclasses to
 * implement {@link PluggableStatement} easily without redefining the semantics
 * provided by this class.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class EndsWith extends it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Statement that has been rewritten to this operation, if any. This is to
	 * accomodate the fact that, in most languages, string operations are
	 * performed through calls, and one might want to provide the semantics of
	 * those calls through {@link NativeCFG} that rewrites to instances of this
	 * class.
	 */
	protected Statement originating;

	/**
	 * Builds the endsWith.
	 * 
	 * @param cfg      the {@link ImplementedCFG} where this operation lies
	 * @param location the code location where this operation is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public EndsWith(ImplementedCFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "endsWith", BoolType.INSTANCE, left, right);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A, H, V> expressions)
					throws SemanticException {
		// we allow untyped for the type inference phase
		if (!left.getDynamicType().isStringType() && !left.getDynamicType().isUntyped())
			return state.bottom();
		if (!right.getDynamicType().isStringType() && !right.getDynamicType().isUntyped())
			return state.bottom();

		return state.smallStepSemantics(
				new BinaryExpression(
						Caches.types().mkSingletonSet(BoolType.INSTANCE),
						left,
						right,
						StringEndsWith.INSTANCE,
						getLocation()),
				originating == null ? this : originating);
	}
}
