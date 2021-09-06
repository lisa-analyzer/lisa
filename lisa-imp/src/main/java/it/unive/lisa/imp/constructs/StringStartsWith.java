package it.unive.lisa.imp.constructs;

import java.util.function.BinaryOperator;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.imp.types.BoolType;
import it.unive.lisa.imp.types.StringType;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.BinaryNativeCall;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.symbolic.SymbolicExpression;
import sun.tools.tree.BinaryExpression;

/**
 * The native construct representing the startsWith operation. This construct
 * can be invoked on a string variable {@code x} with
 * {@code x.startsWith(other)}, where {@code other} is the string that will be
 * checked against prefixes of {@code x}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringStartsWith extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringStartsWith(CodeLocation location, CompilationUnit stringUnit) {
		super(new CFGDescriptor(location, stringUnit, true, "startsWith", BoolType.INSTANCE,
				new Parameter(location, "this", StringType.INSTANCE),
				new Parameter(location, "other", StringType.INSTANCE)),
				IMPStringStartsWith.class);
	}

	/**
	 * An expression modeling the string startsWith operation. The type of both
	 * operands must be {@link StringType}. The type of this expression is the
	 * {@link BoolType}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class IMPStringStartsWith extends BinaryNativeCall implements PluggableStatement {

		private Statement original;

		@Override
		public void setOriginatingStatement(Statement st) {
			original = st;
		}

		/**
		 * Builds the startsWith.
		 * 
		 * @param cfg        the {@link CFG} where this operation lies
		 * @param sourceFile the source file name where this operation is
		 *                       defined
		 * @param line       the line number where this operation is defined
		 * @param col        the column where this operation is defined
		 * @param left       the left-hand side of this operation
		 * @param right      the right-hand side of this operation
		 */
		public IMPStringStartsWith(CFG cfg, String sourceFile, int line, int col, Expression left,
				Expression right) {
			super(cfg, new SourceCodeLocation(sourceFile, line, col), "startsWith", BoolType.INSTANCE, left, right);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(AnalysisState<A, H, V> entryState,
						InterproceduralAnalysis<A, H, V> interprocedural, AnalysisState<A, H, V> leftState,
						SymbolicExpression leftExp, AnalysisState<A, H, V> rightState, SymbolicExpression rightExp)
						throws SemanticException {
			// we allow untyped for the type inference phase
			if (!leftExp.getDynamicType().isStringType() && !leftExp.getDynamicType().isUntyped())
				return entryState.bottom();
			if (!rightExp.getDynamicType().isStringType() && !rightExp.getDynamicType().isUntyped())
				return entryState.bottom();

			return rightState.smallStepSemantics(
					new BinaryExpression(getRuntimeTypes(), leftExp, rightExp, BinaryOperator.STRING_STARTS_WITH,
							getLocation()),
					original);
		}
	}
}
