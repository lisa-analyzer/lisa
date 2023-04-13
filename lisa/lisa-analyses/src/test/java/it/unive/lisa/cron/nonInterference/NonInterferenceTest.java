package it.unive.lisa.cron.nonInterference;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonInterference.NonInterference;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.RecursionFreeToken;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;
import org.junit.Test;

public class NonInterferenceTest extends AnalysisTestExecutor {

	@Test
	public void testConfidentialityNI() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = new SimpleAbstractState<>(
				new MonolithicHeap(),
				new InferenceSystem<>(new NonInterference()),
				new TypeEnvironment<>(new InferredTypes()));
		conf.semanticChecks.add(new NICheck());
		conf.testDir = "non-interference/confidentiality";
		conf.programFile = "program.imp";
		conf.hotspots = st -> st instanceof Assignment
				|| (st instanceof Expression && ((Expression) st).getParentStatement() instanceof Assignment);
		perform(conf);
	}

	@Test
	public void testIntegrityNI() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = new SimpleAbstractState<>(
				new MonolithicHeap(),
				new InferenceSystem<>(new NonInterference()),
				new TypeEnvironment<>(new InferredTypes()));
		conf.semanticChecks.add(new NICheck());
		conf.testDir = "non-interference/integrity";
		conf.programFile = "program.imp";
		conf.hotspots = st -> st instanceof Assignment
				|| (st instanceof Expression && ((Expression) st).getParentStatement() instanceof Assignment);
		perform(conf);
	}

	@Test
	public void testDeclassification() throws AnalysisSetupException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = new SimpleAbstractState<>(
				new MonolithicHeap(),
				new InferenceSystem<>(new NonInterference()),
				new TypeEnvironment<>(new InferredTypes()));
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(RecursionFreeToken.getSingleton());
		conf.semanticChecks.add(new NICheck());
		conf.testDir = "non-interference/interproc";
		conf.programFile = "program.imp";
		conf.hotspots = st -> st instanceof Assignment
				|| (st instanceof Expression && ((Expression) st).getParentStatement() instanceof Assignment);
		perform(conf);
	}

	private static class NICheck
			implements SemanticCheck<
					SimpleAbstractState<MonolithicHeap, InferenceSystem<NonInterference>,
							TypeEnvironment<InferredTypes>>,
					MonolithicHeap,
					InferenceSystem<NonInterference>,
					TypeEnvironment<InferredTypes>> {

		@Override
		@SuppressWarnings({ "unchecked" })
		public boolean visit(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<MonolithicHeap, InferenceSystem<NonInterference>,
								TypeEnvironment<InferredTypes>>,
						MonolithicHeap,
						InferenceSystem<NonInterference>,
						TypeEnvironment<InferredTypes>> tool,
				CFG graph, Statement node) {
			if (!(node instanceof Assignment))
				return true;

			Assignment assign = (Assignment) node;
			Collection<?> results = tool.getResultOf(graph);

			for (Object res : results)
				try {
					AnalyzedCFG<?, ?, ?, ?> result = (AnalyzedCFG<?, ?, ?, ?>) res;
					AnalysisState<?, ?, ?, ?> post = result.getAnalysisStateAfter(assign);
					InferenceSystem<NonInterference> state = post.getDomainInstance(InferenceSystem.class);
					AnalysisState<?, ?, ?, ?> postL = result.getAnalysisStateAfter(assign.getLeft());
					InferenceSystem<NonInterference> left = postL.getDomainInstance(InferenceSystem.class);
					AnalysisState<?, ?, ?, ?> postR = result.getAnalysisStateAfter(assign.getRight());
					InferenceSystem<NonInterference> right = postR.getDomainInstance(InferenceSystem.class);

					for (SymbolicExpression l : postL.rewrite(postL.getComputedExpressions(), assign))
						for (SymbolicExpression r : postR.rewrite(postR.getComputedExpressions(), assign)) {
							NonInterference ll = left.eval((ValueExpression) l, assign.getLeft());
							NonInterference rr = right.eval((ValueExpression) r, assign.getRight());

							if (ll.isLowConfidentiality() && rr.isHighConfidentiality())
								tool.warnOn(assign,
										"This assignment assigns a HIGH confidentiality value to a LOW confidentiality variable, thus violating non-interference");

							if (ll.isLowConfidentiality() && state.getExecutionState().isHighConfidentiality())
								tool.warnOn(assign,
										"This assignment, located in a HIGH confidentiality block, assigns a LOW confidentiality variable, thus violating non-interference");

							if (ll.isHighIntegrity() && rr.isLowIntegrity())
								tool.warnOn(assign,
										"This assignment assigns a LOW integrity value to a HIGH integrity variable, thus violating non-interference");

							if (ll.isHighIntegrity() && state.getExecutionState().isLowIntegrity())
								tool.warnOn(assign,
										"This assignment, located in a LOW integrity block, assigns a HIGH integrity variable, thus violating non-interference");
						}
				} catch (SemanticException e) {
					throw new RuntimeException(e);
				}

			return true;
		}
	}
}
