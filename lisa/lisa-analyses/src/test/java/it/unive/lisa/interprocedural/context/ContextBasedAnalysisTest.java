package it.unive.lisa.interprocedural.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFeatures;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.NoEntryPointException;
import it.unive.lisa.interprocedural.WorstCasePolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.numeric.IntInterval;
import org.junit.jupiter.api.Test;

public class ContextBasedAnalysisTest {

	private ContextBasedAnalysis<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkAnalysis(
					Program p)
					throws InterproceduralAnalysisException,
					CallGraphConstructionException {
		ContextBasedAnalysis<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = new ContextBasedAnalysis<>();
		RTACallGraph callgraph = new RTACallGraph();
		Application app = new Application(p);
		callgraph.init(app, null);
		analysis.init(
				app,
				callgraph,
				WorstCasePolicy.INSTANCE,
				null,
				new Analysis<>(DefaultConfiguration.defaultAbstractDomain()));
		return analysis;
	}

	private AnalysisState<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkState() {
		return new AnalysisState<>(
				new ProgramState<>(DefaultConfiguration.defaultAbstractDomain().makeLattice(), new ExpressionSet()));
	}

	private FixpointConfiguration<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkConf() {
		LiSAConfiguration base = new LiSAConfiguration();
		base.glbThreshold = 5;
		base.wideningThreshold = 5;
		return new FixpointConfiguration<>(base);
	}

	@Test
	public void throwsWhenTheApplicationHasNoEntryPoints()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException {
		// a Program with no entry points registered (IMPFrontend registers
		// every parsed method as one by default, so we build the Program by
		// hand here instead of parsing text)
		Program p = new Program(new IMPFeatures(), new IMPTypeSystem());
		ContextBasedAnalysis<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = mkAnalysis(p);

		assertThrows(NoEntryPointException.class, () -> analysis.fixpoint(mkState(), mkConf()));
	}

	@Test
	public void eachCallSiteIsAnalyzedWithItsOwnArgumentValue()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException,
			FixpointException {
		// the same method is called twice, with two different constant
		// arguments: a context-sensitive analysis must keep the two calls
		// separate, so each assignment should see the exact argument passed
		// at its own call site, not a value merged across both calls
		Program p = IMPFrontend.processText(
				"class c { callee(x) { return x; } "
						+ "caller() { def y1 = this.callee(5); def y2 = this.callee(10); } }");
		ContextBasedAnalysis<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = mkAnalysis(p);

		analysis.fixpoint(mkState(), mkConf());

		CFG caller = p.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("caller")).findFirst()
				.get();
		var results = analysis.getAnalysisResultsOf(caller);
		assertFalse(results.isEmpty());

		var exitState = results.iterator().next().getAnalysisStateAfter(caller.getAllExitpoints().iterator().next());
		Variable y1 = new Variable(Untyped.INSTANCE, "y1", SyntheticLocation.INSTANCE);
		Variable y2 = new Variable(Untyped.INSTANCE, "y2", SyntheticLocation.INSTANCE);
		IntInterval v1 = exitState.getExecutionState().valueState.getState(y1);
		IntInterval v2 = exitState.getExecutionState().valueState.getState(y2);

		assertEquals(new IntInterval(5, 5), v1,
				"expected the first call site to see its own argument (5), but y1 was " + v1);
		assertEquals(new IntInterval(10, 10), v2,
				"expected the second call site to see its own argument (10), but y2 was " + v2);
	}
}
