package it.unive.lisa.cron;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.Stability;
import it.unive.lisa.checks.semantic.CheckToolWithAnalysisResults;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.numeric.Trend;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.datastructures.graph.algorithms.ForwardFixpoint;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class StabilityTest
		extends
		IMPCronExecutor {

	@Test
	public void testStability()
			throws ParsingException {
		CronConfiguration conf = new CronConfiguration();
		conf.serializeResults = true;
		Interval aux = new Interval();
		conf.analysis = DefaultConfiguration.simpleDomain(
				DefaultConfiguration.defaultHeapDomain(),
				new Stability<>(aux),
				DefaultConfiguration.defaultTypeDomain());
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.testDir = "stability";
		conf.programFile = "stability.imp";
		conf.semanticChecks.add(new CoContraVarianceCheck(aux, conf));
		conf.allMethods = true;
		perform(conf);
	}

	private static class CoContraVarianceCheck
			implements
			SemanticCheck<
					SimpleAbstractState<Monolith,
							ValueLatticeProduct<ValueEnvironment<Trend>, ValueEnvironment<IntInterval>>,
							TypeEnvironment<TypeSet>>,
					SimpleAbstractDomain<Monolith,
							ValueLatticeProduct<ValueEnvironment<Trend>, ValueEnvironment<IntInterval>>,
							TypeEnvironment<TypeSet>>> {

		private final Interval aux;

		private final FixpointConfiguration conf;

		private CoContraVarianceCheck(
				Interval aux,
				LiSAConfiguration conf) {
			this.aux = aux;
			this.conf = new FixpointConfiguration(conf);
		}

		@Override
		public boolean visit(
				CheckToolWithAnalysisResults<
						SimpleAbstractState<Monolith,
								ValueLatticeProduct<ValueEnvironment<Trend>, ValueEnvironment<IntInterval>>,
								TypeEnvironment<TypeSet>>,
						SimpleAbstractDomain<Monolith,
								ValueLatticeProduct<ValueEnvironment<Trend>, ValueEnvironment<IntInterval>>,
								TypeEnvironment<TypeSet>>> tool,
				CFG graph) {
			Stability<ValueEnvironment<IntInterval>> analysis = new Stability<>(aux);
			// we start at bottom to have the empty state at the beginning
			ValueEnvironment<Trend> beginning = new ValueEnvironment<>(Trend.BOTTOM).bottom();

			Map<Statement, ValueEnvironment<Trend>> entrypoints = new HashMap<>();
			for (Statement entry : graph.getEntrypoints())
				entrypoints.put(entry, beginning);

			for (AnalyzedCFG<SimpleAbstractState<Monolith,
					ValueLatticeProduct<ValueEnvironment<Trend>, ValueEnvironment<IntInterval>>,
					TypeEnvironment<TypeSet>>> result : tool.getResultOf(graph))
				try {
					StabilityFixpoint fix = new StabilityFixpoint(graph, false, analysis, this.conf, result);
					Map<Statement, ValueEnvironment<Trend>> fixpoint = fix.fixpoint(
							entrypoints,
							new FIFOWorkingSet<>());

					for (Statement exit : graph.getAllExitpoints()) {
						ValueEnvironment<Trend> state = fixpoint.get(exit);
						MapRepresentation repr = new MapRepresentation(
								analysis.getCovarianceClasses(state),
								StringRepresentation::new,
								v -> new SetRepresentation(v, StringRepresentation::new));
						tool.warnOn(exit, "Classes computed: " + StringUtilities.flatten(repr.toString()));
					}
				} catch (FixpointException e) {
					throw new AnalysisExecutionException(e);
				}

			return SemanticCheck.super.visit(tool, graph);
		}

	}

	private static class StabilityFixpoint
			extends
			ForwardFixpoint<CFG, Statement, Edge, ValueEnvironment<Trend>> {

		private final Stability<ValueEnvironment<IntInterval>> analysis;

		private final FixpointConfiguration conf;

		private final AnalyzedCFG<
				SimpleAbstractState<
						Monolith,
						ValueLatticeProduct<ValueEnvironment<Trend>, ValueEnvironment<IntInterval>>,
						TypeEnvironment<TypeSet>>> result;

		public StabilityFixpoint(
				CFG graph,
				boolean forceFullEvaluation,
				Stability<ValueEnvironment<IntInterval>> analysis,
				FixpointConfiguration conf,
				AnalyzedCFG<SimpleAbstractState<Monolith,
						ValueLatticeProduct<ValueEnvironment<Trend>, ValueEnvironment<IntInterval>>,
						TypeEnvironment<TypeSet>>> result) {
			super(graph, forceFullEvaluation);
			this.analysis = analysis;
			this.conf = conf;
			this.result = result;
		}

		@Override
		public ValueEnvironment<Trend> union(
				Statement node,
				ValueEnvironment<Trend> left,
				ValueEnvironment<Trend> right)
				throws Exception {
			return left.lub(right);
		}

		@Override
		public ValueEnvironment<Trend> traverse(
				Edge edge,
				ValueEnvironment<Trend> entrystate)
				throws Exception {
			return entrystate;
		}

		@Override
		public boolean leq(
				Statement node,
				ValueEnvironment<Trend> approx,
				ValueEnvironment<Trend> old)
				throws Exception {
			return approx.lessOrEqual(old);
		}

		@Override
		@SuppressWarnings("unchecked")
		public ValueEnvironment<Trend> semantics(
				Statement node,
				ValueEnvironment<Trend> entrystate)
				throws Exception {
			ValueEnvironment<Trend> post;
			if (result instanceof OptimizedAnalyzedCFG)
				post = ((OptimizedAnalyzedCFG<
						SimpleAbstractState<Monolith,
								ValueLatticeProduct<ValueEnvironment<Trend>,
										ValueEnvironment<IntInterval>>,
								TypeEnvironment<TypeSet>>,
						SimpleAbstractDomain<Monolith,
								ValueLatticeProduct<ValueEnvironment<Trend>,
										ValueEnvironment<IntInterval>>,
								TypeEnvironment<TypeSet>>>) result)
										.getUnwindedAnalysisStateAfter(node, conf)
										.getExecutionState().valueState.first;
			else
				post = result.getAnalysisStateAfter(node)
						.getExecutionState().valueState.first;
			return analysis.combine(entrystate, post);
		}

		@Override
		public ValueEnvironment<Trend> join(
				Statement node,
				ValueEnvironment<Trend> approx,
				ValueEnvironment<Trend> old)
				throws Exception {
			return approx.lub(old);
		}
	}
}
