package it.unive.lisa.cron;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.stability.Stability;
import it.unive.lisa.analysis.stability.Trend;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class StabilityTest extends AnalysisTestExecutor {

	@Test
	public void testScale() throws ParsingException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new Stability<>(new ValueEnvironment<>(new Interval()).top()),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.testDir = "stability";
		conf.testSubDir = "scale";
		conf.programFile = "scale.imp";
		conf.analysisGraphs = GraphType.HTML;
		// conf.semanticChecks.add(new CoContraVarianceCheck<>());
		perform(conf, true);
	}

	/*
	 * USE WITH CAUTION: THIS NEEDS TESTING.
	 */
	@SuppressWarnings("unused")
	private static class CoContraVarianceCheck<V extends ValueDomain<V>>
			implements
			SemanticCheck<SimpleAbstractState<
					MonolithicHeap,
					Stability<V>,
					TypeEnvironment<InferredTypes>>> {

		Map<Statement, Stability<V>> preStatesMap = new HashMap<>();
		Map<Statement, Stability<V>> resultsMap = new LinkedHashMap<>();

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<
								MonolithicHeap,
								Stability<V>,
								TypeEnvironment<InferredTypes>>> tool,
				CFG graph,
				Statement node) {
			if (graph.containsNode(node)) {
				for (AnalyzedCFG<
						SimpleAbstractState<
								MonolithicHeap,
								Stability<V>,
								TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph)) {
					Stability<V> postState = result.getAnalysisStateAfter(node).getState().getValueState();
					Stability<V> cumulativeState = postState;

					if (preStatesMap.containsKey(node)) {
						Stability<V> preState = preStatesMap.get(node);
						try {
							cumulativeState = preState.combine(postState);
						} catch (SemanticException e) {
							throw new AnalysisExecutionException(e);
						}
					}

					resultsMap.put(node, cumulativeState);

					for (Statement next : graph.followersOf(node)) {
						if (preStatesMap.containsKey(next)) {
							try {
								preStatesMap.put(next, preStatesMap.get(next).lub(cumulativeState));
							} catch (SemanticException e) {
								e.printStackTrace();
							}
						} else
							preStatesMap.put(next, cumulativeState);
					}
				}
			}
			return true;
		}

		@Override
		public void afterExecution(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<MonolithicHeap, Stability<V>, TypeEnvironment<InferredTypes>>> tool) {
			// Print resultsMap
			for (Map.Entry<Statement, Stability<V>> entry : resultsMap.entrySet()) {

				// get covClasses and switch Trend for String representing that
				// trend
				HashMap<String, ArrayList<Identifier>> covClasses = new HashMap<>();
				for (Map.Entry<Trend, ArrayList<Identifier>> el : entry.getValue().getCovarianceClasses().entrySet()) {
					covClasses.put(el.getKey().representation().toString(), el.getValue());
				}

				// get line number of Statement
				String locationString = entry.getKey().getLocation().getCodeLocation();
				locationString = locationString.substring(locationString.indexOf(':') + 1,
						locationString.lastIndexOf(':'));

				System.out.println("[" + locationString + "] " + entry.getKey() + ": " + covClasses);
			}

			SemanticCheck.super.afterExecution(tool);
		}
	}

}
