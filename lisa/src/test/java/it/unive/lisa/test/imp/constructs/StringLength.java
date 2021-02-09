package it.unive.lisa.test.imp.constructs;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.test.imp.types.IntType;
import it.unive.lisa.test.imp.types.StringType;

/**
 * The native construct representing the length operation. This construct can be
 * invoked on a string variable {@code x} with {@code x.len()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringLength extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringLength(CompilationUnit stringUnit) {
		super(new CFGDescriptor(stringUnit, true, "len", IntType.INSTANCE,
				new Parameter("this", StringType.INSTANCE)),
				IMPStringLength.class);
	}

	/**
	 * An expression modeling the string length operation. The type of the
	 * operand must be {@link StringType}. The type of this expression is the
	 * {@link IntType}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IMPStringLength extends UnaryNativeCall implements PluggableStatement {

		private Statement original;

		@Override
		public void setOriginatingStatement(Statement st) {
			original = st;
		}

		/**
		 * Builds the length.
		 * 
		 * @param cfg        the {@link CFG} where this operation lies
		 * @param sourceFile the source file name where this operation is
		 *                       defined
		 * @param line       the line number where this operation is defined
		 * @param col        the column where this operation is defined
		 * @param parameter  the operand of this operation
		 */
		public IMPStringLength(CFG cfg, String sourceFile, int line, int col,
				Expression parameter) {
			super(cfg, sourceFile, line, col, "len", IntType.INSTANCE, parameter);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> unarySemantics(AnalysisState<A, H, V> entryState,
						CallGraph callGraph, AnalysisState<A, H, V> exprState, SymbolicExpression expr)
						throws SemanticException {
			// we allow untyped for the type inference phase
			if (!expr.getDynamicType().isStringType() && !expr.getDynamicType().isUntyped())
				return entryState.bottom();

			return exprState
					.smallStepSemantics(new UnaryExpression(getRuntimeTypes(), expr, UnaryOperator.STRING_LENGTH),
							original);
		}

	}

}