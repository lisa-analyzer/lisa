package it.unive.lisa.analysis.nonrelational.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.lattices.SingleHeapLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link HeapEnvironment}, and in particular the {@link HeapReplacement}
 * lists that {@link HeapEnvironment#pushScope(ScopeToken, ProgramPoint)},
 * {@link HeapEnvironment#popScope(ScopeToken, ProgramPoint)} and the various
 * {@code forgetIdentifier*} methods produce.
 */
public class HeapEnvironmentTest {

	private static final ProgramPoint fake = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	private static final ScopeToken token = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private final Variable x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final GlobalVariable g = new GlobalVariable(Untyped.INSTANCE, "g", SyntheticLocation.INSTANCE);

	private HeapEnvironment<SingleHeapLattice> mkEnv(
			Identifier... ids) {
		HeapEnvironment<SingleHeapLattice> env = new HeapEnvironment<>(SingleHeapLattice.BOTTOM);
		for (Identifier id : ids)
			env = env.putState(id, SingleHeapLattice.SINGLETON);
		return env;
	}

	@Test
	public void testPushScopeTracksTheRenamingOfScopableIdentifiers()
			throws SemanticException {
		HeapEnvironment<SingleHeapLattice> env = mkEnv(x);
		Identifier lifted = (Identifier) x.pushScope(token, fake);

		Pair<HeapEnvironment<SingleHeapLattice>, List<HeapReplacement>> result = env.pushScope(token, fake);

		assertTrue(result.getLeft().knowsIdentifier(lifted));
		assertFalse(result.getLeft().knowsIdentifier(x));

		// the identifier actually changed (Variable -> OutOfScopeIdentifier),
		// so
		// a replacement mapping the old identifier onto the new one must be
		// generated: other domains (e.g. value/type environments) rely on this
		// to keep referring to the correct identifier after the scope change
		assertEquals(1, result.getRight().size());
		HeapReplacement repl = result.getRight().get(0);
		assertEquals(Set.of(x), repl.getSources());
		assertEquals(Set.of(lifted), repl.getTargets());
	}

	@Test
	public void testPopScopeTracksTheRenamingWhenUnwrapping()
			throws SemanticException {
		HeapEnvironment<SingleHeapLattice> env = mkEnv(x);
		Pair<HeapEnvironment<SingleHeapLattice>, List<HeapReplacement>> pushed = env.pushScope(token, fake);
		Identifier lifted = pushed.getLeft().getKeys().iterator().next();

		Pair<HeapEnvironment<SingleHeapLattice>, List<HeapReplacement>> popped = pushed.getLeft().popScope(token, fake);

		assertTrue(popped.getLeft().knowsIdentifier(x));
		assertFalse(popped.getLeft().knowsIdentifier(lifted));

		assertEquals(1, popped.getRight().size());
		HeapReplacement repl = popped.getRight().get(0);
		assertEquals(Set.of(lifted), repl.getSources());
		assertEquals(Set.of(x), repl.getTargets());
	}

	@Test
	public void testPushScopeOfUnscopableIdentifierProducesNoReplacement()
			throws SemanticException {
		// a GlobalVariable's pushScope returns the very same identifier: there
		// is nothing to rename, so no replacement should be generated
		HeapEnvironment<SingleHeapLattice> env = mkEnv(g);

		Pair<HeapEnvironment<SingleHeapLattice>, List<HeapReplacement>> result = env.pushScope(token, fake);

		assertTrue(result.getLeft().knowsIdentifier(g));
		assertTrue(result.getRight().isEmpty());
	}

	@Test
	public void testPopScopeOfPlainVariableRemovesItAndProducesAReplacement()
			throws SemanticException {
		// a plain Variable cannot survive a popScope (it must have been
		// introduced by the callee); this is a removal, not a renaming, and is
		// reported through HeapValue#reachableOnlyFrom (here: identity)
		HeapEnvironment<SingleHeapLattice> env = mkEnv(x);

		Pair<HeapEnvironment<SingleHeapLattice>, List<HeapReplacement>> result = env.popScope(token, fake);

		assertFalse(result.getLeft().knowsIdentifier(x));
		assertEquals(1, result.getRight().size());
		assertEquals(Set.of(x), result.getRight().get(0).getSources());
		assertTrue(result.getRight().get(0).getTargets().isEmpty());
	}

	@Test
	public void testForgetIdentifierProducesAnExpandedReplacement()
			throws SemanticException {
		HeapEnvironment<SingleHeapLattice> env = mkEnv(x);

		Pair<HeapEnvironment<SingleHeapLattice>, List<HeapReplacement>> result = env.forgetIdentifier(x, fake);

		assertFalse(result.getLeft().knowsIdentifier(x));
		assertEquals(1, result.getRight().size());
		assertEquals(Set.of(x), result.getRight().get(0).getSources());
		assertTrue(result.getRight().get(0).getTargets().isEmpty());
	}

	@Test
	public void testForgetIdentifierOnTopOrBottomIsNoOp()
			throws SemanticException {
		HeapEnvironment<SingleHeapLattice> top = mkEnv().top();
		HeapEnvironment<SingleHeapLattice> bottom = mkEnv().bottom();

		assertTrue(top.forgetIdentifier(x, fake).getLeft().isTop());
		assertTrue(top.forgetIdentifier(x, fake).getRight().isEmpty());
		assertTrue(bottom.forgetIdentifier(x, fake).getLeft().isBottom());
	}

	@Test
	public void testKnowsIdentifier() {
		HeapEnvironment<SingleHeapLattice> env = mkEnv(x);

		assertTrue(env.knowsIdentifier(x));
		assertFalse(env.knowsIdentifier(g));
	}

}
