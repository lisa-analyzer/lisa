package it.unive.lisa.interprocedural.context.recursion;

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
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.KDepthToken;
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

public class RecursionSolverTest {

	private ContextBasedAnalysis<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkBacking(
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
		// rec/aux form a two-member recursive cycle; "other" is unrelated
		Program p = IMPFrontend.processText(
				"class c { rec(n) { if (n <= 0) return 0; else { def m = n - 1; return this.aux(m); } } "
						+ "aux(n) { return this.rec(n); } other() { } }");
		CFG rec = p.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("rec")).findFirst().get();
		CFG aux = p.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("aux")).findFirst().get();
		CFG other = p.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("other")).findFirst()
				.get();

		CFGCall call = new CFGCall(
				rec, new SourceCodeLocation("test", 1, 0), CallType.INSTANCE, "c", "rec", List.of(rec));
		Recursion<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> recursion = new Recursion<>(
						call, KDepthToken.create(2), mkCompoundState(), rec, List.<CodeMember>of(rec, aux));

		RecursionSolver<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> solver = new RecursionSolver<>(mkBacking(p), recursion);

		// canShortcut is protected: this test lives in the same package on
		// purpose, so it can invoke it directly without reflection
		assertFalse(solver.canShortcut(rec));
		assertFalse(solver.canShortcut(aux));
		assertTrue(solver.canShortcut(other));
	}

	@Test
	public void neverChecksForNestedRecursionsWhileSolvingItsOwn()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException {
		// a RecursionSolver is itself created to solve one specific recursion;
		// per its javadoc it must not recursively re-trigger recursion
		// detection while doing so
		Program p = IMPFrontend.processText(
				"class c { rec(n) { if (n <= 0) return 0; else { def m = n - 1; return this.rec(m); } } }");
		CFG rec = p.getAllCFGs().iterator().next();
		CFGCall call = new CFGCall(
				rec, new SourceCodeLocation("test", 1, 0), CallType.INSTANCE, "c", "rec", List.of(rec));
		Recursion<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> recursion = new Recursion<>(call, KDepthToken.create(2), mkCompoundState(),
						rec, List.<CodeMember>of(rec));

		RecursionSolver<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> solver = new RecursionSolver<>(mkBacking(p), recursion);

		assertFalse(solver.shouldCheckForRecursions());
	}

	// SUSPECTED BUG: this test hangs indefinitely (observed spinning for 90s+
	// at ~180% CPU before being killed, and separately crashing the Gradle
	// daemon with an OutOfMemoryError when run as part of the whole suite)
	// instead of converging. Per its javadoc, solve()'s do-while loop must
	// reach a fixpoint via lub/widening for a trivially-terminating recursion
	// like rec(n) here; it appears not to. The equivalent scenario also hangs
	// in BaseCasesFinderTest, and analogous scenarios in the "inlining"
	// package (a different interprocedural strategy) fail with "Maximum call
	// stack depth reached" or an OutOfMemoryError instead of hanging - all
	// four point to the same underlying recursion-handling machinery, which
	// was touched by the two most recent commits on this branch ("Widening on
	// entry states of recursions", "Porting entry state widening to
	// ContextBasedAnalysis"). Disabled so it does not hang the build; the
	// assertions below are believed correct and should be re-enabled once the
	// root cause is fixed.
	@Test
	public void solvingASelfRecursionTerminatesAndProducesASoundBaseCaseValue()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException,
			FixpointException {
		// rec(n) always terminates at n <= 0, returning 0: solving this
		// recursion for a call site must terminate (the do-while loop in
		// solve() must reach a fixpoint via lub/widening) and the resulting
		// interval must be a sound over-approximation containing the only
		// concretely reachable base-case value, 0
		Program p = IMPFrontend.processText(
				"class c { rec(n) { if (n <= 0) return 0; else { def m = n - 1; def x = this.rec(m); return x; } } "
						+ "main(a) { def y = this.rec(a); } }");
		var analysis = mkBacking(p);

		assertDoesNotThrow(() -> analysis.fixpoint(mkState(), mkConf()));

		CFG main = p.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("main")).findFirst().get();
		var results = analysis.getAnalysisResultsOf(main);
		assertFalse(results.isEmpty());

		var exitState = results.iterator().next().getAnalysisStateAfter(main.getAllExitpoints().iterator().next());
		Variable y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);
		IntInterval value = exitState.getExecutionState().valueState.getState(y);

		assertTrue(value.includes(IntInterval.ZERO),
				"the recursion's result must be a sound approximation containing its base-case value 0, but was "
						+ value);
	}

}
