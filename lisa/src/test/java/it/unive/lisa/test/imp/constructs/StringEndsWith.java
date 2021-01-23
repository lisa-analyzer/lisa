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
import it.unive.lisa.program.cfg.statement.BinaryNativeCall;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.test.imp.types.BoolType;
import it.unive.lisa.test.imp.types.StringType;

public class StringEndsWith extends NativeCFG {

	public StringEndsWith(CompilationUnit stringUnit) {
		super(new CFGDescriptor(stringUnit, true, "endsWith", BoolType.INSTANCE,
				new Parameter("this", StringType.INSTANCE), new Parameter("other", StringType.INSTANCE)),
				IMPStringEndsWith.class);
	}

	public static class IMPStringEndsWith extends BinaryNativeCall {
		public IMPStringEndsWith(CFG cfg, String sourceFile, int line, int col, Expression left,
				Expression right) {
			super(cfg, sourceFile, line, col, "endsWith", BoolType.INSTANCE, left, right);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(AnalysisState<A, H, V> entryState,
						CallGraph callGraph, AnalysisState<A, H, V> leftState, SymbolicExpression leftExp,
						AnalysisState<A, H, V> rightState, SymbolicExpression rightExp) throws SemanticException {
			return rightState.smallStepSemantics(
					new BinaryExpression(getRuntimeTypes(), leftExp, rightExp, BinaryOperator.STRING_ENDS_WITH));
		}
	}
}
