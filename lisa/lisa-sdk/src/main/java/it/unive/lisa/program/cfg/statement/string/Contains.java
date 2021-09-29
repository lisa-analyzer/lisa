package it.unive.lisa.program.cfg.statement.string;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.BinaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.type.common.StringType;

/**
 * An expression modeling the string contains operation. The type of both
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
public class Contains extends BinaryNativeCall {

	/**
	 * Statement that has been rewritten to this operation, if any. This is to
	 * accomodate the fact that, in most languages, string operations are
	 * performed through calls, and one might want to provide the semantics of
	 * those calls through {@link NativeCFG} that rewrites to instances of this
	 * class.
	 */
	protected Statement originating;

	/**
	 * Builds the contains.
	 * 
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the code location where this operation is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public Contains(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "contains", BoolType.INSTANCE, left, right);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
					AnalysisState<A, H, V> entryState,
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> leftState,
					SymbolicExpression leftExp,
					AnalysisState<A, H, V> rightState,
					SymbolicExpression rightExp)
					throws SemanticException {
		// we allow untyped for the type inference phase
		if (!leftExp.getDynamicType().isStringType() && !leftExp.getDynamicType().isUntyped())
			return entryState.bottom();
		if (!rightExp.getDynamicType().isStringType() && !rightExp.getDynamicType().isUntyped())
			return entryState.bottom();

		return rightState.smallStepSemantics(
				new BinaryExpression(
						Caches.types().mkSingletonSet(BoolType.INSTANCE),
						leftExp,
						rightExp,
						BinaryOperator.STRING_CONTAINS,
						getLocation()),
				originating == null ? this : originating);
	}
}
