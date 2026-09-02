package it.unive.lisa.interprocedural.callgraph;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestCallGraph;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class BaseCallGraphTest {

	private final class StrType
			implements
			StringType {

		@Override
		public Type commonSupertype(
				Type other) {
			return canBeAssignedTo(other) ? this : Untyped.INSTANCE;
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other.getClass() == getClass();
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

	}

	private final class BoolType
			implements
			BooleanType {

		@Override
		public Type commonSupertype(
				Type other) {
			return canBeAssignedTo(other) ? this : Untyped.INSTANCE;
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other.getClass() == getClass();
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

	}

	/**
	 * @see <a href="https://github.com/lisa-analyzer/lisa/issues/145">#145</a>
	 */
	@Test
	public void issue145()
			throws CallResolutionException,
			ProgramValidationException,
			CallGraphConstructionException {
		CallGraph cg = new TestCallGraph();

		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());

		CFG cfg1 = new CFG(new CodeMemberDescriptor(new SourceCodeLocation("fake1", 0, 0), p, false, "cfg1"));
		UnresolvedCall call = new UnresolvedCall(
				cfg1,
				new SourceCodeLocation("fake1", 1, 0),
				CallType.STATIC,
				p.getName(),
				"cfg2");
		cfg1.addNode(call, true);
		Ret ret = new Ret(cfg1, new SourceCodeLocation("fake1", 2, 0));
		cfg1.addNode(ret, false);
		cfg1.addEdge(new SequentialEdge(call, ret));

		CFG cfg2 = new CFG(new CodeMemberDescriptor(new SourceCodeLocation("fake2", 0, 0), p, false, "cfg2"));
		cfg2.addNode(new Ret(cfg2, new SourceCodeLocation("fake2", 1, 0)), true);

		p.addCodeMember(cfg2);
		p.addCodeMember(cfg1);
		p.getFeatures().getProgramValidationLogic().validateAndFinalize(p);

		Application app = new Application(p);
		cg.init(app, null);
		@SuppressWarnings("unchecked")
		CFGCall resolved = (CFGCall) cg.resolve(call, new Set[0], new SymbolAliasing());
		cg.registerCall(resolved);

		Collection<CodeMember> callees = cg.getCallees(cfg1);
		assertEquals(1, callees.size());
		assertSame(cfg2, callees.iterator().next());
		assertTrue(cg.getCallees(cfg2).isEmpty());

		Collection<CodeMember> callers = cg.getCallers(cfg2);
		assertEquals(1, callers.size());
		assertSame(cfg1, callers.iterator().next());
		assertTrue(cg.getCallers(cfg1).isEmpty());

		Collection<Call> callSites = cg.getCallSites(cfg2);
		assertEquals(1, callSites.size());
		assertSame(call, callSites.iterator().next());
		assertTrue(cg.getCallSites(cfg1).isEmpty());
	}

	/**
	 * @see <a href="https://github.com/lisa-analyzer/lisa/issues/252">#252</a>
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void issue252()
			throws CallResolutionException,
			ProgramValidationException,
			CallGraphConstructionException {
		CallGraph cg = new TestCallGraph();

		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());

		CFG cfg1 = new CFG(new CodeMemberDescriptor(new SourceCodeLocation("fake1", 0, 0), p, false, "cfg1"));
		UnresolvedCall call = new UnresolvedCall(
				cfg1,
				new SourceCodeLocation("fake1", 1, 0),
				CallType.STATIC,
				p.getName(),
				"cfg2",
				new VariableRef(cfg1, new SourceCodeLocation("fake1", 1, 1), "x"));
		cfg1.addNode(call, true);
		Ret ret = new Ret(cfg1, new SourceCodeLocation("fake1", 2, 0));
		cfg1.addNode(ret, false);
		cfg1.addEdge(new SequentialEdge(call, ret));

		CFG cfg2_1 = new CFG(
				new CodeMemberDescriptor(
						new SourceCodeLocation("fake2", 0, 0),
						p,
						false,
						"cfg2",
						new Parameter(new SourceCodeLocation("fake2", 0, 1), "x", new StrType())));
		cfg2_1.addNode(new Ret(cfg2_1, new SourceCodeLocation("fake2", 1, 0)), true);
		CFG cfg2_2 = new CFG(
				new CodeMemberDescriptor(
						new SourceCodeLocation("fake2", 2, 0),
						p,
						false,
						"cfg2",
						new Parameter(new SourceCodeLocation("fake2", 2, 1), "x", new BoolType())));
		cfg2_2.addNode(new Ret(cfg2_2, new SourceCodeLocation("fake2", 3, 0)), true);

		p.addCodeMember(cfg1);
		p.addCodeMember(cfg2_1);
		p.addCodeMember(cfg2_2);
		p.getFeatures().getProgramValidationLogic().validateAndFinalize(p);

		Application app = new Application(p);
		cg.init(app, null);

		CFGCall resolved = (CFGCall) cg
				.resolve(call, new Set[] { Collections.singleton(new StrType()) }, new SymbolAliasing());

		Collection<CodeMember> callees = resolved.getTargets();
		assertEquals(1, callees.size());
		assertSame(cfg2_1, callees.iterator().next());

		resolved = (CFGCall) cg
				.resolve(call, new Set[] { Collections.singleton(new BoolType()) }, new SymbolAliasing());

		callees = resolved.getTargets();
		assertEquals(1, callees.size());
		assertSame(cfg2_2, callees.iterator().next());
	}

	/**
	 * Builds a static, parameterless {@link CFG} named {@code name} that
	 * immediately returns, and adds it to {@code p}.
	 */
	private static CFG mkCfg(
			Program p,
			String name) {
		CFG cfg = new CFG(new CodeMemberDescriptor(new SourceCodeLocation(name, 0, 0), p, false, name));
		cfg.addNode(new Ret(cfg, new SourceCodeLocation(name, 1, 0)), true);
		p.addCodeMember(cfg);
		return cfg;
	}

	/**
	 * Adds a static call from {@code caller} to the code member named
	 * {@code targetName} (looked up in {@code p}), followed by a {@link Ret},
	 * to {@code caller}, and returns the call node.
	 */
	private static UnresolvedCall mkCall(
			Program p,
			CFG caller,
			String targetName) {
		return mkCall(caller, p.getName(), targetName);
	}

	/**
	 * Same as {@link #mkCall(Program, CFG, String)}, but using an explicit
	 * qualifier instead of the program's name.
	 */
	private static UnresolvedCall mkCall(
			CFG caller,
			String qualifier,
			String targetName) {
		UnresolvedCall call = new UnresolvedCall(
				caller,
				new SourceCodeLocation(caller.getDescriptor().getName(), 1, 0),
				CallType.STATIC,
				qualifier,
				targetName);
		caller.addNode(call, true);
		Ret ret = new Ret(caller, new SourceCodeLocation(caller.getDescriptor().getName(), 2, 0));
		caller.addNode(ret, false);
		caller.addEdge(new SequentialEdge(call, ret));
		return call;
	}

	@Test
	public void resolveMarksTheTargetAsEntrypointWhenItIsOneEvenIfTheCallerIsNot()
			throws CallResolutionException,
			ProgramValidationException,
			CallGraphConstructionException {
		// the target of a call can be an application entry point even when
		// the caller is not (e.g., a library exposing multiple independent
		// entry points that also call each other); the callgraph node
		// created for the target must reflect the target's own entrypoint
		// status, not the caller's
		CallGraph cg = new TestCallGraph();
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());

		CFG caller = mkCfg(p, "caller");
		CFG target = mkCfg(p, "target");
		UnresolvedCall call = mkCall(p, caller, "target");

		p.addEntryPoint(target);
		// caller is deliberately NOT registered as an entry point

		p.getFeatures().getProgramValidationLogic().validateAndFinalize(p);
		Application app = new Application(p);
		cg.init(app, null);

		cg.resolve(call, new Set[0], new SymbolAliasing());

		Collection<CodeMember> entrypoints = cg.getEntrypoints().stream().map(CallGraphNode::getCodeMember).toList();
		assertTrue(entrypoints.contains(target), "the target of the call should be marked as an entrypoint");
		assertFalse(entrypoints.contains(caller), "the caller should not be marked as an entrypoint");
	}

	@Test
	public void registerCallMarksTheTargetAsEntrypointWhenItIsOneEvenIfTheCallerIsNot()
			throws ProgramValidationException,
			CallGraphConstructionException {
		// same as above, but exercising registerCall (used to register
		// CFGCalls that were not produced through resolve(), e.g. built
		// directly by a language frontend) instead of resolve()
		CallGraph cg = new TestCallGraph();
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());

		CFG caller = mkCfg(p, "caller");
		CFG target = mkCfg(p, "target");

		p.addEntryPoint(target);

		p.getFeatures().getProgramValidationLogic().validateAndFinalize(p);
		Application app = new Application(p);
		cg.init(app, null);

		CFGCall call = new CFGCall(
				caller,
				new SourceCodeLocation("caller", 1, 0),
				CallType.STATIC,
				p.getName(),
				"target",
				List.of(target));
		// getSource() is null here, since this call was not produced by
		// resolve(): this is exactly the case registerCall is meant to handle
		cg.registerCall(call);

		Collection<CodeMember> entrypoints = cg.getEntrypoints().stream().map(CallGraphNode::getCodeMember).toList();
		assertTrue(entrypoints.contains(target), "the target of the call should be marked as an entrypoint");
		assertFalse(entrypoints.contains(caller), "the caller should not be marked as an entrypoint");
	}

	@Test
	public void resolvingACallWithNoAliasingInformationAndAnUnknownQualifierYieldsAnOpenCallInsteadOfCrashing()
			throws CallResolutionException,
			ProgramValidationException,
			CallGraphConstructionException {
		// CallGraph#resolve documents aliasing as possibly null; a static
		// call whose qualifier does not name any known unit relies on
		// aliasing to try to find a match, and must gracefully find no
		// target (instead of throwing a NullPointerException) when aliasing
		// is unavailable
		CallGraph cg = new TestCallGraph();
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());

		CFG caller = mkCfg(p, "caller");
		UnresolvedCall call = mkCall(caller, "SomeUnknownQualifier", "target");

		p.getFeatures().getProgramValidationLogic().validateAndFinalize(p);
		Application app = new Application(p);
		cg.init(app, null);

		Call resolved = assertDoesNotThrow(() -> cg.resolve(call, new Set[0], null));
		assertTrue(resolved instanceof OpenCall);
	}

	@Test
	public void resolvingWithNullTypesThrowsCallResolutionExceptionInsteadOfCrashing()
			throws CallResolutionException,
			ProgramValidationException,
			CallGraphConstructionException {
		// FIXME: CallGraph#resolve documents that types may be null "for
		// calls that we already resolved", implying that a null types array
		// should be able to hit the resolution cache and return the
		// previously computed result. However, since #252 the cache is keyed
		// on the specific type combination used for resolution (to support
		// polymorphic call sites), so there is no way to look up "the"
		// cached result for a call independently of types: a null types
		// array can never hit the cache and therefore always results in a
		// CallResolutionException, even for calls that have already been
		// resolved. This test documents that current (degraded, but at least
		// not crashing) behavior; see BaseCallGraph#resolve for details.
		CallGraph cg = new TestCallGraph();
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());

		CFG cfg1 = mkCfg(p, "cfg1");
		CFG cfg2 = mkCfg(p, "cfg2");
		UnresolvedCall call = mkCall(p, cfg1, "cfg2");

		p.getFeatures().getProgramValidationLogic().validateAndFinalize(p);
		Application app = new Application(p);
		cg.init(app, null);

		// a first, successful resolution with actual types
		cg.resolve(call, new Set[0], new SymbolAliasing());

		assertThrows(CallResolutionException.class, () -> cg.resolve(call, null, new SymbolAliasing()));
	}

	@Test
	public void recursionsAndTransitiveClosuresAreComputedCorrectly()
			throws CallResolutionException,
			ProgramValidationException,
			CallGraphConstructionException {
		// builds a callgraph with a 3-node cycle (a -> b -> c -> a) and an
		// additional caller (d) into the cycle that also calls a leaf node
		// (e) outside of it, then checks that recursions and transitive
		// callers/callees are computed according to the graph's actual
		// reachability, cycles included
		CallGraph cg = new TestCallGraph();
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());

		CFG a = mkCfg(p, "a");
		CFG b = mkCfg(p, "b");
		CFG c = mkCfg(p, "c");
		CFG e = mkCfg(p, "e");

		UnresolvedCall aCallsB = mkCall(p, a, "b");
		UnresolvedCall bCallsC = mkCall(p, b, "c");
		UnresolvedCall cCallsA = mkCall(p, c, "a");

		// d calls both a (closing a second path into the cycle) and e (a
		// leaf outside of it), one after the other
		CFG d = new CFG(new CodeMemberDescriptor(new SourceCodeLocation("d", 0, 0), p, false, "d"));
		UnresolvedCall dCallsA = new UnresolvedCall(
				d,
				new SourceCodeLocation("d", 1, 0),
				CallType.STATIC,
				p.getName(),
				"a");
		d.addNode(dCallsA, true);
		UnresolvedCall dCallsE = new UnresolvedCall(
				d,
				new SourceCodeLocation("d", 2, 0),
				CallType.STATIC,
				p.getName(),
				"e");
		d.addNode(dCallsE, false);
		d.addEdge(new SequentialEdge(dCallsA, dCallsE));
		Ret dRet = new Ret(d, new SourceCodeLocation("d", 3, 0));
		d.addNode(dRet, false);
		d.addEdge(new SequentialEdge(dCallsE, dRet));
		p.addCodeMember(d);

		p.getFeatures().getProgramValidationLogic().validateAndFinalize(p);
		Application app = new Application(p);
		cg.init(app, null);

		SymbolAliasing aliasing = new SymbolAliasing();
		cg.resolve(aCallsB, new Set[0], aliasing);
		cg.resolve(bCallsC, new Set[0], aliasing);
		cg.resolve(cCallsA, new Set[0], aliasing);
		cg.resolve(dCallsA, new Set[0], aliasing);
		cg.resolve(dCallsE, new Set[0], aliasing);

		// direct callers/callees
		assertEquals(Set.of(b), Set.copyOf(cg.getCallees(a)));
		assertEquals(Set.of(c, d), Set.copyOf(cg.getCallers(a)));
		assertEquals(Set.of(), Set.copyOf(cg.getCallees(e)));
		assertEquals(Set.of(d), Set.copyOf(cg.getCallers(e)));

		// collection-based overloads union the results of each member
		assertEquals(Set.of(b, a), Set.copyOf(cg.getCallees(List.of(a, c))));
		assertEquals(Set.of(c, d, b), Set.copyOf(cg.getCallers(List.of(a, c))));
		assertEquals(Set.of(aCallsB, cCallsA, dCallsA), Set.copyOf(cg.getCallSites(List.of(b, a))));

		// transitive closures follow cycles back to their starting point
		assertEquals(Set.of(a, b, c, e), Set.copyOf(cg.getCalleesTransitively(d)));
		assertEquals(Set.of(a, b, c, d), Set.copyOf(cg.getCallersTransitively(c)));
		assertEquals(Set.of(), Set.copyOf(cg.getCalleesTransitively(e)));

		// recursions: only the {a, b, c} cycle is one, d and e are not part
		// of any recursion
		Collection<Collection<CodeMember>> recursions = cg.getRecursions();
		assertEquals(1, recursions.size());
		assertEquals(Set.of(a, b, c), Set.copyOf(recursions.iterator().next()));

		assertEquals(Set.of(Set.of(a, b, c)), toSetOfSets(cg.getRecursionsContaining(b)));
		assertTrue(cg.getRecursionsContaining(d).isEmpty());
		assertTrue(cg.getRecursionsContaining(e).isEmpty());
	}

	private static Set<Set<CodeMember>> toSetOfSets(
			Collection<Collection<CodeMember>> recursions) {
		Set<Set<CodeMember>> result = new HashSet<>();
		for (Collection<CodeMember> r : recursions)
			result.add(Set.copyOf(r));
		return result;
	}

}
