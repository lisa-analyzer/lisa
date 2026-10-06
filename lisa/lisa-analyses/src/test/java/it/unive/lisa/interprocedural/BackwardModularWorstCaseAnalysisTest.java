package it.unive.lisa.interprocedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class BackwardModularWorstCaseAnalysisTest {

	private BackwardModularWorstCaseAnalysis<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkAnalysis(
					Program p)
					throws InterproceduralAnalysisException,
					CallGraphConstructionException {
		BackwardModularWorstCaseAnalysis<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = new BackwardModularWorstCaseAnalysis<>();
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
	public void needsCallGraphIsFalse()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException {
		Program p = IMPFrontend.processText("class empty { foo() { } }");
		assertFalse(mkAnalysis(p).needsCallGraph());
	}

	@Test
	public void resolveNeverReturnsARealCall()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			CallResolutionException,
			ParsingException {
		Program p = IMPFrontend.processText("class c { bar() { } foo() { this.bar(); } }");
		BackwardModularWorstCaseAnalysis<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = mkAnalysis(p);

		CFG foo = p.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("foo")).findFirst().get();
		UnresolvedCall call = new UnresolvedCall(foo, SyntheticLocation.INSTANCE, CallType.INSTANCE, null, "bar");

		@SuppressWarnings("unchecked")
		Set<Type>[] types = new Set[0];
		Call resolved = analysis.resolve(call, types, null);
		assertInstanceOf(OpenCall.class, resolved);
	}

	@Test
	public void fixpointRequiresABackwardFixpointStrategy()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException {
		Program p = IMPFrontend.processText("class empty { foo() { } }");
		BackwardModularWorstCaseAnalysis<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = mkAnalysis(p);

		// a fixpoint strategy is only supplied for the *forward* direction:
		// a class whose whole point is running a backward analysis must
		// refuse to proceed rather than silently doing nothing useful
		LiSAConfiguration base = new LiSAConfiguration();
		base.backwardFixpoint = null;
		FixpointConfiguration<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> conf = new FixpointConfiguration<>(base);

		assertThrows(IllegalArgumentException.class, () -> analysis.fixpoint(mkState(), conf));
	}

	@Test
	public void everyCfgIsAnalyzedIndependentlyOfTheCallGraph()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException,
			FixpointException {
		Program p = IMPFrontend.processText("class c { used() { } alone() { } caller() { this.used(); } }");
		BackwardModularWorstCaseAnalysis<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<
						Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = mkAnalysis(p);

		analysis.fixpoint(mkState(), mkConf());

		for (CFG cfg : p.getAllCFGs())
			assertFalse(analysis.getAnalysisResultsOf(cfg).isEmpty(),
					"expected " + cfg.getDescriptor().getName() + " to have been analyzed");
	}
}
