package it.unive.lisa.imp.constructs;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.imp.types.IntType;
import it.unive.lisa.imp.types.StringType;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NativeCall;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.UnaryOperator;

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
	 * @param location   the location where this construct is defined
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringLength(CodeLocation location, CompilationUnit stringUnit) {
		super(new CFGDescriptor(location, stringUnit, true, "len", IntType.INSTANCE,
				new Parameter(location, "this", StringType.INSTANCE)),
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

		/**
		 * Builds a new instance of this native call, according to the
		 * {@link PluggableStatement} contract.
		 * 
		 * @param cfg      the cfg where the native call happens
		 * @param location the location where the native call happens
		 * @param params   the parameters of the native call
		 * 
		 * @return the newly-built call
		 */
		public static NativeCall build(CFG cfg, CodeLocation location, Expression... params) {
			return new IMPStringLength(cfg, location, params[0]);
		}

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
			super(cfg, new SourceCodeLocation(sourceFile, line, col), "len", IntType.INSTANCE, parameter);
		}

		/**
		 * Builds the length.
		 * 
		 * @param cfg       the {@link CFG} where this operation lies
		 * @param location  the code location where this operation is defined
		 * @param parameter the operand of this operation
		 */
		public IMPStringLength(CFG cfg, CodeLocation location, Expression parameter) {
			super(cfg, location, "len", IntType.INSTANCE, parameter);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> unarySemantics(AnalysisState<A, H, V> entryState,
						InterproceduralAnalysis<A, H, V> interprocedural, AnalysisState<A, H, V> exprState,
						SymbolicExpression expr)
						throws SemanticException {
			// we allow untyped for the type inference phase
			if (!expr.getDynamicType().isStringType() && !expr.getDynamicType().isUntyped())
				return entryState.bottom();

			return exprState
					.smallStepSemantics(
							new UnaryExpression(getRuntimeTypes(), expr, UnaryOperator.STRING_LENGTH, getLocation()),
							original);
		}

	}

}