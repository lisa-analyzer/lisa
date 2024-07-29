package it.unive.lisa.cron;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.CronConfiguration;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.stability.Stability;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint.FixpointImplementation;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class StabilityTest extends AnalysisTestExecutor {

	@Test
	public void testStability() throws ParsingException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		ValueEnvironment<Interval> aux = new ValueEnvironment<>(new Interval()).top();
		conf.abstractState = DefaultConfiguration.simpleState(
				DefaultConfiguration.defaultHeapDomain(),
				new Stability<>(aux),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.testDir = "stability";
		conf.programFile = "stability.imp";
		conf.semanticChecks.add(new CoContraVarianceCheck<>(aux, conf));
		perform(conf, true);
	}

	private static class CoContraVarianceCheck<V extends ValueDomain<V>>
			implements
			SemanticCheck<SimpleAbstractState<
					MonolithicHeap,
					Stability<V>,
					TypeEnvironment<InferredTypes>>> {

		private final V aux;
		private final FixpointConfiguration conf;

		private CoContraVarianceCheck(
				V aux,
				LiSAConfiguration conf) {
			this.aux = aux;
			this.conf = new FixpointConfiguration(conf);
		}

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<MonolithicHeap, Stability<V>, TypeEnvironment<InferredTypes>>> tool,
				CFG graph) {
			Fixpoint<CFG, Statement, Edge, Stability<V>> fix = new Fixpoint<>(graph, false);
			// we start at bottom to have the empty state at the beginning
			Stability<V> beginning = new Stability<>(aux).bottom();

			Map<Statement, Stability<V>> entrypoints = new HashMap<>();
			for (Statement entry : graph.getEntrypoints())
				entrypoints.put(entry, beginning);

			for (AnalyzedCFG<
					SimpleAbstractState<
							MonolithicHeap,
							Stability<V>,
							TypeEnvironment<InferredTypes>>> result : tool.getResultOf(graph))
				try {
					Map<Statement, Stability<V>> fixpoint = fix.fixpoint(entrypoints, FIFOWorkingSet.mk(),
							new FixpointImplementation<Statement, Edge, Stability<V>>() {

								@Override
								public Stability<V> union(
										Statement node,
										Stability<V> left,
										Stability<V> right)
										throws Exception {
									return left.lub(right);
								}

								@Override
								public Stability<V> traverse(
										Edge edge,
										Stability<V> entrystate)
										throws Exception {
									return entrystate;
								}

								@Override
								public boolean equality(
										Statement node,
										Stability<V> approx,
										Stability<V> old)
										throws Exception {
									return approx.lessOrEqual(old);
								}

								@Override
								public Stability<V> semantics(
										Statement node,
										Stability<V> entrystate)
										throws Exception {
									Stability<V> post;
									if (result instanceof OptimizedAnalyzedCFG)
										post = ((OptimizedAnalyzedCFG<
												SimpleAbstractState<
														MonolithicHeap,
														Stability<V>,
														TypeEnvironment<InferredTypes>>>) result)
																.getUnwindedAnalysisStateAfter(node, conf)
																.getState()
																.getValueState();
									else
										post = result.getAnalysisStateAfter(node)
												.getState()
												.getValueState();
									return entrystate.combine(post);
								}

								@Override
								public Stability<V> operation(
										Statement node,
										Stability<V> approx,
										Stability<V> old)
										throws Exception {
									return approx.lub(old);
								}
							});

					for (Statement exit : graph.getAllExitpoints()) {
						Stability<V> state = fixpoint.get(exit);
						MapRepresentation repr = new MapRepresentation(
								state.getCovarianceClasses(),
								StringRepresentation::new, v -> new SetRepresentation(v, StringRepresentation::new));
						tool.warnOn(exit, "Classes computed: " + StringUtilities.flatten(repr.toString()));
					}
				} catch (FixpointException e) {
					throw new AnalysisExecutionException(e);
				}

			return SemanticCheck.super.visit(tool, graph);
		}
	}
}
