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
import it.unive.lisa.program.cfg.statement.UnaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.test.imp.types.IntType;
import it.unive.lisa.test.imp.types.StringType;

public class StringLength extends NativeCFG {

	public StringLength(CompilationUnit stringUnit) {
		super(new CFGDescriptor(stringUnit, true, "len", IntType.INSTANCE,
				new Parameter("this", StringType.INSTANCE)),
				IMPStringLength.class);
	}

	public static class IMPStringLength extends UnaryNativeCall {

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
			return exprState
					.smallStepSemantics(new UnaryExpression(getRuntimeTypes(), expr, UnaryOperator.STRING_LENGTH));
		}

	}

}