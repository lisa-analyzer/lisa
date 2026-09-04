package it.unive.lisa.interprocedural.inlining.recursion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.Recursion;
import it.unive.lisa.interprocedural.WorstCasePolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.inlining.CallStackId;
import it.unive.lisa.interprocedural.inlining.InliningAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BaseCasesFinderTest {

	private <A extends InliningAnalysis<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>>> A mkBacking(
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

	private CompoundState<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkCompoundState() {
		var state = mkState();
		return CompoundState.of(state, new StatementStore<>(state));
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
	public void canShortcutIsFalseOnlyForMembersOfTheRecursion()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException {
		Program p = IMPFrontend.processText("class c { foo() { } bar() { } }");
		var it = p.getAllCFGs().iterator();
		CFG member = it.next();
		CFG outsider = it.next();

		CFGCall call = new CFGCall(
				member, new SourceCodeLocation("test", 1, 0), CallType.INSTANCE, "c", "foo", List.of(member));
		Recursion<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> recursion = new Recursion<>(
						call, CallStackId.create(), mkCompoundState(), member, List.<CodeMember>of(member));

		BaseCasesFinder<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> finder = new BaseCasesFinder<>(
								mkBacking(new InliningAnalysis<>(), p), recursion, false);

		// canShortcut is protected: this test lives in the same package on
		// purpose, so it can invoke it directly without reflection
		assertFalse(finder.canShortcut(member), "a member of the recursion must never be shortcut");
		assertTrue(finder.canShortcut(outsider), "a CFG outside the recursion can still be shortcut");
	}

	@Test
	public void neverChecksForNestedRecursionsAndNeverStoresItsOwnFixpointResults()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException {
		Program p = IMPFrontend.processText("class c { foo() { } }");
		CFG member = p.getAllCFGs().iterator().next();
		CFGCall call = new CFGCall(
				member, new SourceCodeLocation("test", 1, 0), CallType.INSTANCE, "c", "foo", List.of(member));
		Recursion<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> recursion = new Recursion<>(call, CallStackId.create(), mkCompoundState(),
						member, List.<CodeMember>of(member));

		BaseCasesFinder<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> finder = new BaseCasesFinder<>(
								mkBacking(new InliningAnalysis<>(), p), recursion, false);

		assertFalse(finder.shouldCheckForRecursions());
		assertFalse(finder.shouldStoreFixpointResults());
	}

	@Test
	public void aCallTowardsAnUnrelatedRecursionIsRejected()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException,
			FixpointException {
		// per a FIXME in the implementation itself, a call directed at a
		// recursion other than the one currently being solved is explicitly
		// unsupported and must fail loudly rather than silently mis-analyzing
		// it: rec(0) hits the base case directly with no such call, so it
		// must resolve cleanly with no exception. maxCallStackDepth must be
		// bounded and shouldRaiseException false, otherwise the recursion is
		// never detected/solved at all and inlining diverges (see
		// InliningAnalysis's javadoc)
		Program p = IMPFrontend.processText(
				"class c { rec(n) { if (n <= 0) return 0; else { def m = n - 1; def x = this.rec(m); return x; } } "
						+ "main(a) { def y = this.rec(0); } }");
		var analysis = mkBacking(new InliningAnalysis<>(5, false), p);

		assertDoesNotThrow(() -> analysis.fixpoint(mkState(), mkConf()));

		CFG main = p.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("main")).findFirst().get();
		var results = analysis.getAnalysisResultsOf(main);
		assertFalse(results.isEmpty());

		var exitState = results.iterator().next().getAnalysisStateAfter(main.getAllExitpoints().iterator().next());
		Variable y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);
		IntInterval value = exitState.getExecutionState().valueState.getState(y);

		assertTrue(value.includes(IntInterval.ZERO),
				"rec(0) must resolve to (an over-approximation containing) its base-case value 0, but was " + value);
	}

}
