package it.unive.lisa.cron.nonInterference;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.nonInterference.NonInterference;
import it.unive.lisa.analysis.inference.InferenceSystem;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Statement;

public class NonInterferenceTest extends AnalysisTestExecutor {

	@Test
	public void testNonInterference() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true)
				.setAbstractState(getDefaultFor(AbstractState.class,
						getDefaultFor(HeapDomain.class), new NonInterference()))
				.addSemanticCheck(new TypeSystemNICheck());
		perform("non-interference", "type-system", "program.imp", conf);
	}

	private static class TypeSystemNICheck implements SemanticCheck {

		@Override
		public void beforeExecution(CheckToolWithAnalysisResults<?, ?, ?> tool) {
		}

		@Override
		public void afterExecution(CheckToolWithAnalysisResults<?, ?, ?> tool) {
		}

		@Override
		public boolean visitCompilationUnit(CheckToolWithAnalysisResults<?, ?, ?> tool, CompilationUnit unit) {
			return true;
		}

		@Override
		public void visitGlobal(CheckToolWithAnalysisResults<?, ?, ?> tool, Unit unit, Global global,
				boolean instance) {
		}

		@Override
		public boolean visit(CheckToolWithAnalysisResults<?, ?, ?> tool, CFG graph) {
			return true;
		}

		@Override
		@SuppressWarnings({ "unchecked" })
		public boolean visit(CheckToolWithAnalysisResults<?, ?, ?> tool, CFG graph, Statement node) {
			if (!(node instanceof Assignment))
				return true;

			Assignment assign = (Assignment) node;
			InferenceSystem<NonInterference> state = (InferenceSystem<NonInterference>) tool.getResultOf(graph)
					.getAnalysisStateAt(assign).getState().getValueState();
			InferenceSystem<NonInterference> left = (InferenceSystem<NonInterference>) tool.getResultOf(graph)
					.getAnalysisStateAt(assign.getLeft()).getState().getValueState();
			InferenceSystem<NonInterference> right = (InferenceSystem<NonInterference>) tool.getResultOf(graph)
					.getAnalysisStateAt(assign.getRight()).getState().getValueState();

			if (left.getInferredValue().isLow() && right.getInferredValue().isHigh())
				tool.warnOn(assign,
						"This assignment assigns a HIGH value to LOW variable, thus violating non-interference");

			if (left.getInferredValue().isLow() && state.getExecutionState().isHigh())
				tool.warnOn(assign,
						"This assignment, located in a HIGH block, assigns a LOW variable, thus violating non-interference");

			return true;
		}

		@Override
		public boolean visit(CheckToolWithAnalysisResults<?, ?, ?> tool, CFG graph, Edge edge) {
			return true;
		}

	}
}
