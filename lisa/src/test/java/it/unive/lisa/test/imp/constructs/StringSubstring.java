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
import it.unive.lisa.program.cfg.statement.TernaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.test.imp.types.BoolType;
import it.unive.lisa.test.imp.types.IntType;
import it.unive.lisa.test.imp.types.StringType;

public class StringSubstring extends NativeCFG {

	public StringSubstring(CompilationUnit stringUnit) {
		super(new CFGDescriptor(stringUnit, true, "substring", BoolType.INSTANCE,
				new Parameter("this", StringType.INSTANCE), new Parameter("start", IntType.INSTANCE),
				new Parameter("end", IntType.INSTANCE)),
				IMPStringSubstring.class);
	}

	public static class IMPStringSubstring extends TernaryNativeCall {
		public IMPStringSubstring(CFG cfg, String sourceFile, int line, int col, Expression left,
				Expression middle, Expression right) {
			super(cfg, sourceFile, line, col, "substring", StringType.INSTANCE, left, middle, right);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> ternarySemantics(AnalysisState<A, H, V> entryState,
						CallGraph callGraph, AnalysisState<A, H, V> leftState, SymbolicExpression leftExp,
						AnalysisState<A, H, V> middleState, SymbolicExpression middleExp,
						AnalysisState<A, H, V> rightState, SymbolicExpression rightExp) throws SemanticException {
			return rightState.smallStepSemantics(new TernaryExpression(getRuntimeTypes(), leftExp, middleExp, rightExp,
					TernaryOperator.STRING_SUBSTRING));
		}
	}
}
