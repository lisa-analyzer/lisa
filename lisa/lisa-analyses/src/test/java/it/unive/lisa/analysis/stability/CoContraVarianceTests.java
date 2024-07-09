package it.unive.lisa.analysis.stability;

import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class CoContraVarianceTests extends AnalysisTestExecutor {

	@Test
	public void testThreeLevelsTaint() throws ParsingException {

		Program program = IMPFrontend.processFile("imp-testcases/stability/saneScale.imp");
		LiSAConfiguration conf = new DefaultConfiguration();
		conf.workdir = "output/stability/DCS";
		// conf.serializeResults = true;
		conf.abstractState = new SimpleAbstractState<>(
				// heap domain
				new MonolithicHeap(),
				// new FieldSensitivePointBasedHeap(),
				// value domain
				new Stability<>(new ValueEnvironment<>(new Interval()).top()),
				// new ValueEnvironment<>(new Sign()),
				// type domain
				new TypeEnvironment<>(new InferredTypes()));
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>(); // ??

		conf.semanticChecks.add(new CoContraVarianceCheck<Interval>());

		LiSA lisa = new LiSA(conf);
		lisa.run(program);
	}

	private static class CoContraVarianceCheck<T extends BaseNonRelationalValueDomain<T>>
			implements
			SemanticCheck<SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>> {

		Map<Statement, Stability<T>> preStatesMap = new HashMap<>();
		Map<Statement, Stability<T>> resultsMap = new LinkedHashMap<>();

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>> tool,
				CFG graph,
				Statement node) {

			if (graph.containsNode(node)) {
				for (AnalyzedCFG<
						SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>> result : tool
								.getResultOf(graph)) {

					Stability<T> postState = result.getAnalysisStateAfter(node).getState().getValueState();
					Stability<T> cumulativeState = postState;

					// computing cumulative trend for node
					if (preStatesMap.containsKey(node)) {
						Stability<T> preState = preStatesMap.get(node);
						cumulativeState = preState.environmentCombine(postState);
					}
					resultsMap.put(node, cumulativeState);

					// computing new entry state for next
					for (Statement next : graph.followersOf(node)) {
						if (preStatesMap.containsKey(next)) {
							// join converging branches
							try {
								System.out.println("IT HAPPENED");
								preStatesMap.put(next, preStatesMap.get(next).lub(cumulativeState));
							} catch (SemanticException e) {
								e.printStackTrace();
							}
						} else
							preStatesMap.put(next, cumulativeState); // the pre
																		// of
																		// next
																		// is
																		// the
																		// post
																		// of
																		// this
					}
				}
			}
			return true;
		}

		@Override
		public void afterExecution(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<MonolithicHeap, Stability<T>, TypeEnvironment<InferredTypes>>> tool) {

			// Print resultsMap
			for (Map.Entry<Statement, Stability<T>> entry : resultsMap.entrySet()) {

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
