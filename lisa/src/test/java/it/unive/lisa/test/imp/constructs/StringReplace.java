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
import it.unive.lisa.program.cfg.statement.TernaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.test.imp.types.BoolType;
import it.unive.lisa.test.imp.types.StringType;

/**
 * The native construct representing the replace operation. This construct can
 * be invoked on a string variable {@code x} with
 * {@code x.replace(search, replacement)}, where {@code search} is the string to
 * replace and {@code replacement} is the string to use as a replacement.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringReplace extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringReplace(CompilationUnit stringUnit) {
		super(new CFGDescriptor(stringUnit, true, "replace", BoolType.INSTANCE,
				new Parameter("this", StringType.INSTANCE), new Parameter("search", StringType.INSTANCE),
				new Parameter("replacement", StringType.INSTANCE)),
				IMPStringReplace.class);
	}

	/**
	 * An expression modeling the string replace operation. The type of all
	 * three operands must be {@link StringType}. The type of this expression is
	 * the {@link StringType}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IMPStringReplace extends TernaryNativeCall implements PluggableStatement {

		private Statement original;

		@Override
		public void setOriginatingStatement(Statement st) {
			original = st;
		}

		/**
		 * Builds the replace.
		 * 
		 * @param cfg        the {@link CFG} where this operation lies
		 * @param sourceFile the source file name where this operation is
		 *                       defined
		 * @param line       the line number where this operation is defined
		 * @param col        the column where this operation is defined
		 * @param left       the left-hand side of this operation
		 * @param middle     the middle operand of this operation
		 * @param right      the right-hand side of this operation
		 */
		public IMPStringReplace(CFG cfg, String sourceFile, int line, int col, Expression left,
				Expression middle, Expression right) {
			super(cfg, sourceFile, line, col, "replace", StringType.INSTANCE, left, middle, right);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> ternarySemantics(AnalysisState<A, H, V> entryState,
						CallGraph callGraph, AnalysisState<A, H, V> leftState, SymbolicExpression leftExp,
						AnalysisState<A, H, V> middleState, SymbolicExpression middleExp,
						AnalysisState<A, H, V> rightState, SymbolicExpression rightExp) throws SemanticException {
			// we allow untyped for the type inference phase
			if (!leftExp.getDynamicType().isStringType() && !leftExp.getDynamicType().isUntyped())
				return entryState.bottom();
			if (!middleExp.getDynamicType().isStringType() && !middleExp.getDynamicType().isUntyped())
				return entryState.bottom();
			if (!rightExp.getDynamicType().isStringType() && !rightExp.getDynamicType().isUntyped())
				return entryState.bottom();

			return rightState.smallStepSemantics(new TernaryExpression(getRuntimeTypes(), leftExp, middleExp, rightExp,
					TernaryOperator.STRING_REPLACE), original);
		}
	}
}
