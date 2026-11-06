package it.unive.lisa.interprocedural.inlining;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import it.unive.lisa.util.numeric.IntInterval;
import org.junit.jupiter.api.Test;

public class InliningAnalysisTest {

	private <A extends InliningAnalysis<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>> A mkAnalysis(
					A analysis,
					Program p)
					throws InterproceduralAnalysisException,
					CallGraphConstructionException {
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
	public void needsCallGraphIsTrue()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException {
		Program p = IMPFrontend.processText("class empty { foo() { } }");
		// unlike the worst-case modular strategies, calls actually need to be
		// resolved through the call graph to be inlined
		assertTrue(mkAnalysis(new InliningAnalysis<>(), p).needsCallGraph());
	}

	@Test
	public void throwsWhenTheApplicationHasNoEntryPoints()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException {
		Program p = new Program(new IMPFeatures(), new IMPTypeSystem());
		InliningAnalysis<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = mkAnalysis(new InliningAnalysis<>(), p);

		assertThrows(NoEntryPointException.class, () -> analysis.fixpoint(mkState(), mkConf()));
	}

	@Test
	public void unboundedRecursionExceedingTheStackDepthRaisesAnException()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException {
		// no base case: every recursive call keeps growing the call stack,
		// so a small maximum depth must eventually be exceeded
		Program p = IMPFrontend.processText("class c { rec(n) { def x = this.rec(n); return x; } }");
		InliningAnalysis<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = mkAnalysis(new InliningAnalysis<>(2, true), p);

		assertThrows(Exception.class, () -> analysis.fixpoint(mkState(), mkConf()));
	}

	@Test
	public void unboundedRecursionExceedingTheStackDepthFallsBackToTopWhenConfiguredTo()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException {
		Program p = IMPFrontend.processText("class c { rec(n) { def x = this.rec(n); return x; } }");
		InliningAnalysis<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = mkAnalysis(new InliningAnalysis<>(2, false), p);

		// with shouldRaiseException = false, the javadoc promises top is
		// returned once the depth is reached instead of failing the analysis
		assertDoesNotThrow(() -> analysis.fixpoint(mkState(), mkConf()));
	}
}
