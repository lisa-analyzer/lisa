package it.unive.lisa.analysis.reachability;

import static it.unive.lisa.analysis.reachability.ReachabilityStatus.POSSIBLY_REACHABLE;
import static it.unive.lisa.analysis.reachability.ReachabilityStatus.REACHABLE;
import static it.unive.lisa.analysis.reachability.ReachabilityStatus.UNREACHABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.TestCallGraph;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.MarkerAbstractDomain;
import it.unive.lisa.analysis.MarkerAbstractLattice;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.protection.CatchBlock;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.protection.ProtectionBlock;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Reachability}. Each test builds the cfgs and the results of
 * the fixpoint by hand, choosing the post-state of each statement, and then
 * queries the reachability of the statements. Conditions are evaluated by
 * {@link Domain}, where the state of a guard records whether the condition
 * might hold ({@link #MAY_HOLD}) and whether it might not hold
 * ({@link #MAY_FAIL}).
 */
public class ReachabilityTest {

	private static final Variable MAY_HOLD = new Variable(Untyped.INSTANCE, "mayHold", SyntheticLocation.INSTANCE);

	private static final Variable MAY_FAIL = new Variable(Untyped.INSTANCE, "mayFail", SyntheticLocation.INSTANCE);

	private static final Variable CONDITION = new Variable(Untyped.INSTANCE, "cond", SyntheticLocation.INSTANCE);

	private static final TestType A = new TestType("A", null);

	private static final TestType B = new TestType("B", A);

	private static final TestType C = new TestType("C", A);

	private Program program;

	private Map<CFG, Collection<AnalyzedCFG<MarkerAbstractLattice>>> results;

	private Collection<CFGCall> calls;

	private int line;

	/**
	 * A domain where conditions are satisfied depending on whether the state
	 * knows {@link ReachabilityTest#MAY_HOLD} and
	 * {@link ReachabilityTest#MAY_FAIL}.
	 */
	private static class Domain
			extends
			MarkerAbstractDomain {

		@Override
		public Satisfiability satisfies(
				MarkerAbstractLattice state,
				SymbolicExpression expression,
				ProgramPoint pp) {
			if (state.isBottom())
				return Satisfiability.BOTTOM;
			if (state.isTop())
				return Satisfiability.UNKNOWN;
			boolean holds = state.known.contains(MAY_HOLD);
			boolean fails = state.known.contains(MAY_FAIL);
			if (holds && !fails)
				return Satisfiability.SATISFIED;
			if (fails && !holds)
				return Satisfiability.NOT_SATISFIED;
			return Satisfiability.UNKNOWN;
		}
	}

	/**
	 * A type with at most one supertype.
	 */
	private static class TestType
			implements
			Type {

		private final String name;

		private final TestType parent;

		private TestType(
				String name,
				TestType parent) {
			this.name = name;
			this.parent = parent;
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			for (TestType t = this; t != null; t = t.parent)
				if (t == other)
					return true;
			return false;
		}

		@Override
		public Type commonSupertype(
				Type other) {
			for (TestType t = this; t != null; t = t.parent)
				if (other.canBeAssignedTo(t))
					return t;
			return Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Set.of(this);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * A scope identified by the stack of calls leading to it.
	 */
	private static class CallStack
			implements
			ScopeId<MarkerAbstractLattice> {

		private final List<CFGCall> stack;

		private CallStack(
				List<CFGCall> stack) {
			this.stack = stack;
		}

		@Override
		public ScopeId<MarkerAbstractLattice> startingId() {
			return new CallStack(List.of());
		}

		@Override
		public boolean isStartingId() {
			return stack.isEmpty();
		}

		@Override
		public ScopeId<MarkerAbstractLattice> push(
				CFGCall c,
				CompoundState<MarkerAbstractLattice> state) {
			List<CFGCall> pushed = new ArrayList<>(stack);
			pushed.add(c);
			return new CallStack(pushed);
		}

		@Override
		public int hashCode() {
			return Objects.hash(stack);
		}

		@Override
		public boolean equals(
				Object obj) {
			return obj instanceof CallStack && stack.equals(((CallStack) obj).stack);
		}
	}

	/**
	 * A scope identified by the stack of calls leading to it, together with the
	 * state at each call.
	 */
	private static class StateStack
			implements
			ScopeId<MarkerAbstractLattice> {

		private final List<Object> stack;

		private StateStack(
				List<Object> stack) {
			this.stack = stack;
		}

		@Override
		public ScopeId<MarkerAbstractLattice> startingId() {
			return new StateStack(List.of());
		}

		@Override
		public boolean isStartingId() {
			return stack.isEmpty();
		}

		@Override
		public ScopeId<MarkerAbstractLattice> push(
				CFGCall c,
				CompoundState<MarkerAbstractLattice> state) {
			List<Object> pushed = new ArrayList<>(stack);
			pushed.add(c);
			pushed.add(state.postState);
			return new StateStack(pushed);
		}

		@Override
		public int hashCode() {
			return Objects.hash(stack);
		}

		@Override
		public boolean equals(
				Object obj) {
			return obj instanceof StateStack && stack.equals(((StateStack) obj).stack);
		}
	}

	@BeforeEach
	public void init() {
		program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		results = new HashMap<>();
		calls = new ArrayList<>();
		line = 0;
	}

	private SourceCodeLocation nextLocation() {
		return new SourceCodeLocation("test", ++line, 0);
	}

	private CFG cfg(
			String name,
			boolean entrypoint) {
		CFG cfg = new CFG(new CodeMemberDescriptor(nextLocation(), program, false, name));
		program.addCodeMember(cfg);
		if (entrypoint)
			program.addEntryPoint(cfg);
		return cfg;
	}

	private Statement node(
			CFG cfg,
			boolean entrypoint) {
		Statement st = new NoOp(cfg, nextLocation());
		cfg.addNode(st, entrypoint);
		return st;
	}

	private Statement node(
			CFG cfg) {
		return node(cfg, false);
	}

	private CFGCall call(
			CFG cfg,
			CFG target,
			boolean entrypoint) {
		CFGCall call = new CFGCall(cfg, nextLocation(), CallType.STATIC, "test", target.getDescriptor().getName(),
				List.of(target));
		cfg.addNode(call, entrypoint);
		calls.add(call);
		return call;
	}

	private static void seq(
			Statement... sts) {
		for (int i = 0; i < sts.length - 1; i++)
			sts[i].getCFG().addEdge(new SequentialEdge(sts[i], sts[i + 1]));
	}

	private static void branch(
			Statement guard,
			Statement ifTrue,
			Statement ifFalse) {
		guard.getCFG().addEdge(new TrueEdge(guard, ifTrue));
		guard.getCFG().addEdge(new FalseEdge(guard, ifFalse));
	}

	private static void loop(
			Statement guard,
			Statement follower,
			Statement... body) {
		CFG cfg = guard.getCFG();
		cfg.getDescriptor().addControlFlowStructure(new Loop(cfg.getNodeList(), guard, follower, Set.of(body)));
	}

	private static ProtectedBlock tryBlock(
			Statement start,
			Statement end,
			Statement... body) {
		return new ProtectedBlock(start, end, Set.of(body));
	}

	private static CatchBlock catchBlock(
			ProtectedBlock block,
			Statement handler,
			Type... types) {
		CFG cfg = handler.getCFG();
		VariableRef var = new VariableRef(cfg, handler.getLocation(), "e");
		// as frontends do, the edge leaves the end of the try block
		cfg.addEdge(new ErrorEdge(block.getEnd(), handler, var, block, types));
		return new CatchBlock(var, new ProtectedBlock(handler, handler, Set.of(handler)), types);
	}

	private static void protection(
			ProtectedBlock block,
			CatchBlock... catches) {
		block.getStart().getCFG().getDescriptor()
				.addProtectionBlock(new ProtectionBlock(block, List.of(catches), null, null, null));
	}

	private static ProgramState<MarkerAbstractLattice> program(
			Identifier... known) {
		return new ProgramState<>(new MarkerAbstractLattice(new HashSet<>(Set.of(known))), new ExpressionSet());
	}

	private static AnalysisState<MarkerAbstractLattice> state(
			ProgramState<MarkerAbstractLattice> execution) {
		return new AnalysisState<>(execution).bottom().withExecution(execution);
	}

	private static AnalysisState<MarkerAbstractLattice> ok() {
		return state(program());
	}

	private static AnalysisState<MarkerAbstractLattice> dead() {
		return ok().bottom();
	}

	private static AnalysisState<MarkerAbstractLattice> guard(
			boolean mayHold,
			boolean mayFail) {
		Set<Identifier> known = new HashSet<>();
		if (mayHold)
			known.add(MAY_HOLD);
		if (mayFail)
			known.add(MAY_FAIL);
		return state(new ProgramState<>(new MarkerAbstractLattice(known), new ExpressionSet(CONDITION)));
	}

	private static AnalysisState<MarkerAbstractLattice> knowing(
			Identifier... known) {
		return state(program(known));
	}

	private static AnalysisState<MarkerAbstractLattice> raising(
			AnalysisState<MarkerAbstractLattice> state,
			Type type,
			Statement thrower)
			throws SemanticException {
		return state.addError(new Error(type, thrower), program());
	}

	private static AnalysisState<MarkerAbstractLattice> halting(
			AnalysisState<MarkerAbstractLattice> state) {
		return state.withHalt(program());
	}

	/**
	 * Adds a result for the given cfg. Statements and entry states that are not
	 * specified in {@code posts} (given as statement-state pairs) are
	 * {@link #ok()}.
	 */
	private AnalyzedCFG<MarkerAbstractLattice> result(
			CFG cfg,
			ScopeId<MarkerAbstractLattice> id,
			Object... posts) {
		return result(cfg, id, ok(), posts);
	}

	private AnalyzedCFG<MarkerAbstractLattice> result(
			CFG cfg,
			ScopeId<MarkerAbstractLattice> id,
			AnalysisState<MarkerAbstractLattice> entry,
			Object... posts) {
		Map<Statement, AnalysisState<MarkerAbstractLattice>> post = new HashMap<>();
		for (Statement st : cfg.getNodes())
			post.put(st, ok());
		for (int i = 0; i < posts.length; i += 2) {
			@SuppressWarnings("unchecked")
			AnalysisState<MarkerAbstractLattice> state = (AnalysisState<MarkerAbstractLattice>) posts[i + 1];
			post.put((Statement) posts[i], state);
		}
		Map<Statement, AnalysisState<MarkerAbstractLattice>> entries = new HashMap<>();
		for (Statement st : cfg.getEntrypoints())
			entries.put(st, entry);
		AnalyzedCFG<MarkerAbstractLattice> res = new AnalyzedCFG<>(cfg, id, ok(), entries, post);
		results.computeIfAbsent(cfg, c -> new ArrayList<>()).add(res);
		return res;
	}

	private Reachability<MarkerAbstractLattice, Domain> reachability()
			throws CallGraphConstructionException {
		CallGraph callgraph = new TestCallGraph();
		callgraph.init(new Application(program), null);
		for (CFGCall call : calls)
			callgraph.registerCall(call);
		return new Reachability<>(results, callgraph, new Analysis<>(new Domain()));
	}

	private static void check(
			Reachability<MarkerAbstractLattice, Domain> reach,
			AnalyzedCFG<MarkerAbstractLattice> result,
			Object... expected)
			throws SemanticException {
		for (int i = 0; i < expected.length; i += 2) {
			Statement st = (Statement) expected[i];
			assertEquals(expected[i + 1], reach.getReachability(result, st),
					"Wrong reachability for the statement at " + st.getLocation());
		}
	}

	@Test
	public void sequence()
			throws Exception {
		CFG main = cfg("main", true);
		Statement a = node(main, true), b = node(main), c = node(main);
		seq(a, b, c);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()));

		check(reachability(), res, a, REACHABLE, b, REACHABLE, c, REACHABLE);
	}

	@Test
	public void unknownCondition()
			throws Exception {
		CFG main = cfg("main", true);
		Statement g = node(main, true), t = node(main), f = node(main), join = node(main);
		branch(g, t, f);
		seq(t, join);
		seq(f, join);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()), g, guard(true, true));

		check(reachability(), res, g, REACHABLE, t, POSSIBLY_REACHABLE, f, POSSIBLY_REACHABLE, join, REACHABLE);
	}

	@Test
	public void decidedCondition()
			throws Exception {
		// the false branch is pruned through satisfies only, even if its
		// post-state is not bottom
		CFG main = cfg("main", true);
		Statement g = node(main, true), t = node(main), f = node(main), join = node(main);
		branch(g, t, f);
		seq(t, join);
		seq(f, join);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()), g, guard(true, false));

		check(reachability(), res, t, REACHABLE, f, UNREACHABLE, join, REACHABLE);
	}

	@Test
	public void followerIsNextCondition()
			throws Exception {
		// the statement where the first branch rejoins is the condition of
		// the second one
		CFG main = cfg("main", true);
		Statement g1 = node(main, true), t1 = node(main), f1 = node(main), g2 = node(main), t2 = node(main),
				f2 = node(main), end = node(main);
		branch(g1, t1, f1);
		seq(t1, g2);
		seq(f1, g2);
		branch(g2, t2, f2);
		seq(t2, end);
		seq(f2, end);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				g1, guard(true, true), g2, guard(true, true));

		check(reachability(), res, t1, POSSIBLY_REACHABLE, f1, POSSIBLY_REACHABLE, g2, REACHABLE, t2,
				POSSIBLY_REACHABLE, f2, POSSIBLY_REACHABLE, end, REACHABLE);
	}

	@Test
	public void followerIsLoopCondition()
			throws Exception {
		// the statement where the branch rejoins is the guard of a loop
		CFG main = cfg("main", true);
		Statement g1 = node(main, true), t1 = node(main), f1 = node(main), g2 = node(main), body = node(main),
				end = node(main);
		branch(g1, t1, f1);
		seq(t1, g2);
		seq(f1, g2);
		branch(g2, body, end);
		seq(body, g2);
		loop(g2, end, body);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				g1, guard(true, true),
				t1, knowing(MAY_HOLD),
				f1, knowing(MAY_HOLD),
				g2, guard(true, true));

		check(reachability(), res, g2, REACHABLE, body, REACHABLE, end, REACHABLE);
	}

	@Test
	public void bottomPostState()
			throws Exception {
		CFG main = cfg("main", true);
		Statement a = node(main, true), b = node(main), c = node(main);
		seq(a, b, c);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()), b, dead(), c, dead());

		check(reachability(), res, a, REACHABLE, b, UNREACHABLE, c, UNREACHABLE);
	}

	@Test
	public void returnInBranch()
			throws Exception {
		CFG main = cfg("main", true);
		Statement g = node(main, true), ret = node(main), a = node(main), b = node(main);
		branch(g, ret, a);
		seq(a, b);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()), g, guard(true, true));

		check(reachability(), res, g, REACHABLE, ret, POSSIBLY_REACHABLE, a, POSSIBLY_REACHABLE, b,
				POSSIBLY_REACHABLE);
	}

	@Test
	public void uncaughtErrors()
			throws Exception {
		CFG main = cfg("main", true);
		Statement sometimes = node(main, true), a = node(main), always = node(main), b = node(main);
		seq(sometimes, a, always, b);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				sometimes, raising(ok(), A, sometimes),
				// b's post-state is not bottom, but no normal execution
				// leaves always
				always, raising(dead(), A, always));

		check(reachability(), res, sometimes, REACHABLE, a, POSSIBLY_REACHABLE, always, POSSIBLY_REACHABLE, b,
				UNREACHABLE);
	}

	@Test
	public void errorsOfInnerExpressions()
			throws Exception {
		// errors thrown by expressions are raised by their root statement
		CFG main = cfg("main", true);
		VariableRef inner = new VariableRef(main, nextLocation(), "x");
		Statement ret = new Return(main, nextLocation(), inner);
		main.addNode(ret, true);
		Statement handler = node(main);
		ProtectedBlock block = tryBlock(ret, ret, ret);
		protection(block, catchBlock(block, handler, A));
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				ret, raising(dead(), A, inner));

		check(reachability(), res, ret, REACHABLE, handler, REACHABLE);
	}

	@Test
	public void caughtErrors()
			throws Exception {
		CFG main = cfg("main", true);
		Statement start = node(main, true), sometimes = node(main), always = node(main), next = node(main);
		Statement handler1 = node(main), handler2 = node(main), join = node(main);
		seq(start, sometimes, always, next, join);
		seq(handler1, always);
		seq(handler2, join);
		ProtectedBlock first = tryBlock(sometimes, sometimes, sometimes);
		protection(first, catchBlock(first, handler1, A));
		ProtectedBlock second = tryBlock(always, next, always, next);
		protection(second, catchBlock(second, handler2, A));
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				sometimes, raising(ok(), A, sometimes),
				always, raising(dead(), A, always));

		// executions rejoin after the catch blocks
		check(reachability(), res, sometimes, REACHABLE, handler1, POSSIBLY_REACHABLE, always, REACHABLE, next,
				UNREACHABLE, handler2, REACHABLE, join, REACHABLE);
	}

	@Test
	public void errorsCaughtBySubtype()
			throws Exception {
		CFG main = cfg("main", true);
		Statement thrower = node(main, true), next = node(main), handler = node(main);
		seq(thrower, next);
		ProtectedBlock block = tryBlock(thrower, thrower, thrower);
		protection(block, catchBlock(block, handler, A));
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				thrower, raising(dead(), B, thrower));

		check(reachability(), res, handler, REACHABLE, next, UNREACHABLE);
	}

	@Test
	public void errorsNotCaughtByType()
			throws Exception {
		CFG main = cfg("main", true);
		Statement thrower = node(main, true), next = node(main), handler = node(main);
		seq(thrower, next);
		ProtectedBlock block = tryBlock(thrower, thrower, thrower);
		protection(block, catchBlock(block, handler, B));
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				thrower, raising(ok(), C, thrower));

		check(reachability(), res, handler, UNREACHABLE, next, POSSIBLY_REACHABLE);
	}

	@Test
	public void errorsCaughtByInnermostBlock()
			throws Exception {
		CFG main = cfg("main", true);
		// try { try { try { thrower } catch (C) { otherHandler } } catch (A) {
		// innerHandler } } catch (A) { outerHandler }
		Statement start = node(main, true), thrower = node(main), outerHandler = node(main),
				innerHandler = node(main), otherHandler = node(main);
		seq(start, thrower);
		ProtectedBlock other = tryBlock(thrower, thrower, thrower);
		protection(other, catchBlock(other, otherHandler, C));
		ProtectedBlock inner = tryBlock(thrower, otherHandler, thrower, otherHandler);
		protection(inner, catchBlock(inner, innerHandler, A));
		ProtectedBlock outer = tryBlock(start, innerHandler, start, thrower, otherHandler, innerHandler);
		protection(outer, catchBlock(outer, outerHandler, A));
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				thrower, raising(dead(), B, thrower));

		check(reachability(), res, innerHandler, REACHABLE, outerHandler, UNREACHABLE, otherHandler, UNREACHABLE);
	}

	@Test
	public void errorsCaughtByMostSpecificCatch()
			throws Exception {
		// as in the fixpoint, a catch block does not catch errors that
		// another catch block of the same protection block catches more
		// specifically
		CFG main = cfg("main", true);
		Statement throwsB = node(main, true), throwsC = node(main), handlerA = node(main), handlerB = node(main),
				join = node(main);
		seq(throwsB, throwsC);
		seq(handlerA, join);
		seq(handlerB, join);
		ProtectedBlock block = tryBlock(throwsB, throwsC, throwsB, throwsC);
		protection(block, catchBlock(block, handlerA, A), catchBlock(block, handlerB, B));
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				throwsB, raising(dead(), B, throwsB));
		check(reachability(), res, handlerB, REACHABLE, handlerA, UNREACHABLE, throwsC, UNREACHABLE);

		results.clear();
		res = result(main, new CallStack(List.of()), throwsB, raising(dead(), C, throwsB));
		check(reachability(), res, handlerA, REACHABLE, handlerB, UNREACHABLE);
	}

	@Test
	public void smashedErrors()
			throws Exception {
		CFG main = cfg("main", true);
		Statement thrower = node(main, true), next = node(main), handler = node(main);
		seq(thrower, next);
		ProtectedBlock block = tryBlock(thrower, thrower, thrower);
		protection(block, catchBlock(block, handler, A));
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				thrower, ok().addSmashedError(new Error(A, thrower), program()));

		check(reachability(), res, handler, POSSIBLY_REACHABLE, next, POSSIBLY_REACHABLE);
	}

	@Test
	public void unreachableHandlers()
			throws Exception {
		// errors whose catch block is unreachable according to the fixpoint
		// leave the cfg
		CFG main = cfg("main", true);
		Statement thrower = node(main, true), next = node(main), handler = node(main);
		seq(thrower, next);
		ProtectedBlock block = tryBlock(thrower, thrower, thrower);
		protection(block, catchBlock(block, handler, A));
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				thrower, raising(ok(), A, thrower), handler, dead());

		check(reachability(), res, handler, UNREACHABLE, next, POSSIBLY_REACHABLE);
	}

	@Test
	public void halting()
			throws Exception {
		CFG main = cfg("main", true);
		Statement sometimes = node(main, true), a = node(main), always = node(main), b = node(main);
		seq(sometimes, a, always, b);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				sometimes, halting(ok()),
				a, halting(ok()),
				always, halting(dead()),
				b, halting(ok()));

		check(reachability(), res, a, POSSIBLY_REACHABLE, always, POSSIBLY_REACHABLE, b, UNREACHABLE);
	}

	@Test
	public void haltingInheritedFromPredecessors()
			throws Exception {
		// statements whose halting state is the one they receive do not halt
		CFG main = cfg("main", true);
		Statement a = node(main, true), b = node(main), c = node(main);
		seq(a, b, c);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()), halting(ok()),
				a, halting(ok()),
				b, halting(ok()),
				c, halting(ok()));

		check(reachability(), res, a, REACHABLE, b, REACHABLE, c, REACHABLE);
	}

	@Test
	public void haltingCalls()
			throws Exception {
		// halting executions of the targets cannot be distinguished from the
		// ones reaching the call
		CFG main = cfg("main", true);
		CFG halts = cfg("halts", false);
		CFG safe = cfg("safe", false);
		Statement h = node(halts, true);
		Statement s = node(safe, true);
		CFGCall safeCall = call(main, safe, true);
		Statement a = node(main);
		CFGCall haltingCall = call(main, halts, false);
		Statement b = node(main);
		seq(safeCall, a, haltingCall, b);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()), halting(ok()),
				safeCall, halting(ok()),
				a, halting(ok()),
				haltingCall, halting(ok()),
				b, halting(ok()));
		result(halts, new CallStack(List.of(haltingCall)), h, halting(ok()));
		result(safe, new CallStack(List.of(safeCall)), s, ok());

		check(reachability(), res, a, REACHABLE, haltingCall, REACHABLE, b, POSSIBLY_REACHABLE);
	}

	@Test
	public void firstIterationOfLoops()
			throws Exception {
		CFG main = cfg("main", true);
		Statement init = node(main, true), g = node(main), body = node(main), after = node(main);
		seq(init, g);
		branch(g, body, after);
		seq(body, g);
		loop(g, after, body);

		// the guard initially holds
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				init, knowing(MAY_HOLD), g, guard(true, true));
		check(reachability(), res, g, REACHABLE, body, REACHABLE, after, REACHABLE);

		// the guard might initially hold
		results.clear();
		res = result(main, new CallStack(List.of()), init, knowing(MAY_HOLD, MAY_FAIL), g, guard(true, true));
		check(reachability(), res, g, REACHABLE, body, POSSIBLY_REACHABLE, after, REACHABLE);

		// the guard initially does not hold
		results.clear();
		res = result(main, new CallStack(List.of()), init, knowing(MAY_FAIL), g, guard(true, true));
		check(reachability(), res, g, REACHABLE, body, UNREACHABLE, after, REACHABLE);
	}

	@Test
	public void escapesFromLoops()
			throws Exception {
		CFG main = cfg("main", true);
		Statement init = node(main, true), g = node(main), inner = node(main), brk = node(main), inc = node(main),
				ret = node(main), after = node(main);
		seq(init, g);
		branch(g, inner, after);
		branch(inner, brk, inc);
		branch(inc, ret, g);
		seq(brk, after);
		loop(g, after, inner, brk, inc, ret);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				init, knowing(MAY_HOLD),
				g, guard(true, true),
				inner, guard(true, true),
				inc, guard(true, true));

		// breaks do not leave the cfg, while returns do
		check(reachability(), res, inner, REACHABLE, brk, POSSIBLY_REACHABLE, inc, POSSIBLY_REACHABLE, ret,
				POSSIBLY_REACHABLE, after, POSSIBLY_REACHABLE);
	}

	@Test
	public void infiniteLoops()
			throws Exception {
		CFG main = cfg("main", true);
		Statement init = node(main, true), g = node(main), body = node(main), after = node(main);
		seq(init, g);
		branch(g, body, after);
		seq(body, g);
		loop(g, after, body);
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()),
				init, knowing(MAY_HOLD), g, guard(true, false));

		// executions that never leave the loop still execute what precedes it
		check(reachability(), res, init, REACHABLE, g, REACHABLE, body, REACHABLE, after, UNREACHABLE);
	}

	@Test
	public void synthesizedStatements()
			throws Exception {
		// statements that are not part of the cfg are mapped to the node
		// containing an expression at the same location
		CFG main = cfg("main", true);
		Statement g = node(main, true), t = node(main), f = node(main);
		branch(g, t, f);
		CFGCall synthetic = new CFGCall(main, (SourceCodeLocation) t.getLocation(), CallType.STATIC, "test", "x",
				List.of());
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, new CallStack(List.of()), g, guard(true, true));

		check(reachability(), res, synthetic, POSSIBLY_REACHABLE);
	}

	@Test
	public void synthesizedCalls()
			throws Exception {
		// calls that are not part of the cfg (e.g., calls to static
		// initializers synthesized by frontends) are linked through the node
		// containing an expression at the same location
		CFG main = cfg("main", true);
		CFG callee = cfg("callee", false);
		Statement body = node(callee, true);
		Statement n = node(main, true);
		CFGCall synthetic = new CFGCall(main, (SourceCodeLocation) n.getLocation(), CallType.STATIC, "test",
				"callee", List.of(callee));
		calls.add(synthetic);
		CallStack start = new CallStack(List.of());
		result(main, start);
		AnalyzedCFG<MarkerAbstractLattice> res = result(callee, start.push(synthetic, null));

		Reachability<MarkerAbstractLattice, Domain> reach = reachability();
		assertEquals(REACHABLE, reach.getEntryReachability(res));
		assertEquals(REACHABLE, reach.getReachability(body));
	}

	@Test
	public void callingContexts()
			throws Exception {
		CFG main = cfg("main", true);
		CFG always = cfg("always", false);
		CFG sometimes = cfg("sometimes", false);
		CFG never = cfg("never", false);
		Statement alwaysBody = node(always, true);
		Statement sometimesBody = node(sometimes, true);
		Statement neverBody = node(never, true);

		CFGCall c1 = call(main, always, true);
		Statement g1 = node(main);
		CFGCall c2 = call(main, sometimes, false);
		Statement g2 = node(main);
		CFGCall c3 = call(main, never, false);
		CFGCall c4 = call(main, always, false);
		Statement end = node(main);
		seq(c1, g1);
		branch(g1, c2, g2);
		seq(c2, g2);
		branch(g2, c3, c4);
		seq(c3, c4, end);
		CallStack start = new CallStack(List.of());
		AnalyzedCFG<MarkerAbstractLattice> res = result(main, start,
				g1, guard(true, true), g2, guard(false, true));

		AnalyzedCFG<MarkerAbstractLattice> always1 = result(always, start.push(c1, null));
		AnalyzedCFG<MarkerAbstractLattice> always4 = result(always, start.push(c4, null));
		AnalyzedCFG<MarkerAbstractLattice> sometimes2 = result(sometimes, start.push(c2, null));
		AnalyzedCFG<MarkerAbstractLattice> never3 = result(never, start.push(c3, null));
		// no call site leads to this context
		AnalyzedCFG<MarkerAbstractLattice> unlinked = result(always, start.push(c2, null));

		Reachability<MarkerAbstractLattice, Domain> reach = reachability();
		check(reach, res, c1, REACHABLE, c2, POSSIBLY_REACHABLE, c3, UNREACHABLE, c4, REACHABLE);
		assertEquals(REACHABLE, reach.getEntryReachability(res));
		assertEquals(REACHABLE, reach.getEntryReachability(always1));
		assertEquals(REACHABLE, reach.getEntryReachability(always4));
		assertEquals(POSSIBLY_REACHABLE, reach.getEntryReachability(sometimes2));
		assertEquals(UNREACHABLE, reach.getEntryReachability(never3));
		assertEquals(POSSIBLY_REACHABLE, reach.getEntryReachability(unlinked));
		check(reach, sometimes2, sometimesBody, POSSIBLY_REACHABLE);
		check(reach, never3, neverBody, UNREACHABLE);
		check(reach, unlinked, alwaysBody, POSSIBLY_REACHABLE);
		assertEquals(REACHABLE, reach.getReachability(alwaysBody));
		assertEquals(POSSIBLY_REACHABLE, reach.getReachability(sometimesBody));
		assertEquals(UNREACHABLE, reach.getReachability(neverBody));
	}

	@Test
	public void callsInBranchesOfCallees()
			throws Exception {
		// the reachability of call sites within callees is composed with the
		// one of the entry of the callee
		CFG main = cfg("main", true);
		CFG middle = cfg("middle", false);
		CFG leaf = cfg("leaf", false);
		Statement leafBody = node(leaf, true);
		CFGCall toMiddle = call(main, middle, true);
		Statement g = node(middle, true);
		CFGCall toLeaf = call(middle, leaf, false);
		Statement end = node(middle);
		branch(g, toLeaf, end);
		seq(toLeaf, end);
		CallStack start = new CallStack(List.of());
		result(main, start);
		ScopeId<MarkerAbstractLattice> middleId = start.push(toMiddle, null);
		result(middle, middleId, g, guard(true, true));
		AnalyzedCFG<MarkerAbstractLattice> leafRes = result(leaf, middleId.push(toLeaf, null));

		Reachability<MarkerAbstractLattice, Domain> reach = reachability();
		assertEquals(POSSIBLY_REACHABLE, reach.getEntryReachability(leafRes));
		assertEquals(POSSIBLY_REACHABLE, reach.getReachability(leafBody));
	}

	@Test
	public void recursion()
			throws Exception {
		// with a single scope, recursive calls are linked to the same result
		ScopeId<MarkerAbstractLattice> unique = new UniqueScope<>();
		CFG main = cfg("main", true);
		CFG rec = cfg("rec", false);
		CFG orphan = cfg("orphan", false);
		CFGCall toRec = call(main, rec, true);
		Statement g = node(rec, true);
		CFGCall self = call(rec, rec, false);
		Statement base = node(rec);
		branch(g, self, base);
		Statement o = node(orphan, true);
		CFGCall orphanSelf = call(orphan, orphan, false);
		seq(o, orphanSelf);
		result(main, unique);
		AnalyzedCFG<MarkerAbstractLattice> recRes = result(rec, unique, g, guard(true, true));
		AnalyzedCFG<MarkerAbstractLattice> orphanRes = result(orphan, unique);

		Reachability<MarkerAbstractLattice, Domain> reach = reachability();
		assertEquals(REACHABLE, reach.getEntryReachability(recRes));
		check(reach, recRes, g, REACHABLE, self, POSSIBLY_REACHABLE, base, POSSIBLY_REACHABLE);
		// orphan is only called by itself
		assertEquals(UNREACHABLE, reach.getEntryReachability(orphanRes));
		check(reach, orphanRes, o, UNREACHABLE);
	}

	@Test
	public void entriesWithoutCallSites()
			throws Exception {
		CFG main = cfg("main", true);
		CFG other = cfg("other", false);
		Statement m = node(main, true);
		Statement o = node(other, true);
		AnalyzedCFG<MarkerAbstractLattice> mainRes = result(main, new CallStack(List.of()));
		AnalyzedCFG<MarkerAbstractLattice> otherRes = result(other, new CallStack(List.of()));

		Reachability<MarkerAbstractLattice, Domain> reach = reachability();
		check(reach, mainRes, m, REACHABLE);
		// not an entrypoint, and with no call sites
		check(reach, otherRes, o, POSSIBLY_REACHABLE);
	}

	@Test
	public void stateDependentScopes()
			throws Exception {
		// scopes are pushed with the state after the evaluation of the
		// parameters of the call
		CFG main = cfg("main", true);
		CFG callee = cfg("callee", false);
		Statement body = node(callee, true);
		VariableRef param = new VariableRef(main, nextLocation(), "p");
		CFGCall call = new CFGCall(main, nextLocation(), CallType.STATIC, "test", "callee", List.of(callee), param);
		main.addNode(call, true);
		calls.add(call);
		StateStack start = new StateStack(List.of());
		result(main, start, param, state(new ProgramState<>(new MarkerAbstractLattice(Set.of(MAY_HOLD)),
				new ExpressionSet(CONDITION))));
		AnalyzedCFG<MarkerAbstractLattice> linked = result(callee,
				start.push(call, CompoundState.of(knowing(MAY_HOLD), null)));
		AnalyzedCFG<MarkerAbstractLattice> unlinked = result(callee,
				start.push(call, CompoundState.of(ok(), null)));

		Reachability<MarkerAbstractLattice, Domain> reach = reachability();
		assertEquals(REACHABLE, reach.getEntryReachability(linked));
		assertEquals(POSSIBLY_REACHABLE, reach.getEntryReachability(unlinked));
		assertEquals(REACHABLE, reach.getReachability(body));
	}

	@Test
	public void scopesPushedWithFixpointPreStates()
			throws Exception {
		// the state reaching a call is the one computed by the fixpoint when
		// traversing the edges reaching it, that forgets, e.g., the variables
		// going out of scope
		CFG main = cfg("main", true);
		CFG callee = cfg("callee", false);
		Statement body = node(callee, true);
		Statement a = node(main, true);
		CFGCall call = call(main, callee, false);
		seq(a, call);
		main.getDescriptor().addVariable(new VariableTableEntry(a.getLocation(), 0, a, a, "x"));
		Identifier x = new VariableRef(main, a.getLocation(), "x").getIdentifier();
		StateStack start = new StateStack(List.of());
		result(main, start, a, knowing(x));
		AnalyzedCFG<MarkerAbstractLattice> linked = result(callee,
				start.push(call, CompoundState.of(ok(), null)));

		Reachability<MarkerAbstractLattice, Domain> reach = reachability();
		assertEquals(REACHABLE, reach.getEntryReachability(linked));
		assertEquals(REACHABLE, reach.getReachability(body));
	}
}
