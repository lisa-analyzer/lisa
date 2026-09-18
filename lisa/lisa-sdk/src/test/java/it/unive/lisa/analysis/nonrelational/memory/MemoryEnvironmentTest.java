package it.unive.lisa.analysis.nonrelational.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.lattices.SingleMemoryLattice;
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
 * Tests {@link MemoryEnvironment}, and in particular the
 * {@link MemoryReplacement} lists that
 * {@link MemoryEnvironment#pushScope(ScopeToken, ProgramPoint)},
 * {@link MemoryEnvironment#popScope(ScopeToken, ProgramPoint)} and the various
 * {@code forgetIdentifier*} methods produce.
 */
public class MemoryEnvironmentTest {

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

	private MemoryEnvironment<SingleMemoryLattice> mkEnv(
			Identifier... ids) {
		MemoryEnvironment<SingleMemoryLattice> env = new MemoryEnvironment<>(SingleMemoryLattice.BOTTOM);
		for (Identifier id : ids)
			env = env.putState(id, SingleMemoryLattice.SINGLETON);
		return env;
	}

	@Test
	public void testPushScopeOfAScopableIdentifierProducesNoReplacement()
			throws SemanticException {
		MemoryEnvironment<SingleMemoryLattice> env = mkEnv(x);
		Identifier lifted = (Identifier) x.pushScope(token, fake);

		Pair<MemoryEnvironment<SingleMemoryLattice>, List<MemoryReplacement>> result = env.pushScope(token, fake);

		assertTrue(result.getLeft().knowsIdentifier(lifted));
		assertFalse(result.getLeft().knowsIdentifier(x));
		assertTrue(result.getRight().isEmpty());
	}

	@Test
	public void testPopScopeOfAScopableIdentifierProducesNoReplacementWhenUnwrapping()
			throws SemanticException {
		MemoryEnvironment<SingleMemoryLattice> env = mkEnv(x);
		Pair<MemoryEnvironment<SingleMemoryLattice>, List<MemoryReplacement>> pushed = env.pushScope(token, fake);
		Identifier lifted = pushed.getLeft().getKeys().iterator().next();

		Pair<MemoryEnvironment<SingleMemoryLattice>,
				List<MemoryReplacement>> popped = pushed.getLeft().popScope(token, fake);

		assertTrue(popped.getLeft().knowsIdentifier(x));
		assertFalse(popped.getLeft().knowsIdentifier(lifted));
		assertTrue(popped.getRight().isEmpty());
	}

	@Test
	public void testPushScopeOfUnscopableIdentifierProducesASelfReplacement()
			throws SemanticException {
		MemoryEnvironment<SingleMemoryLattice> env = mkEnv(g);

		Pair<MemoryEnvironment<SingleMemoryLattice>, List<MemoryReplacement>> result = env.pushScope(token, fake);

		assertTrue(result.getLeft().knowsIdentifier(g));
		assertEquals(1, result.getRight().size());
		assertEquals(Set.of(g), result.getRight().get(0).getSources());
		assertEquals(Set.of(g), result.getRight().get(0).getTargets());
	}

	@Test
	public void testPopScopeOfPlainVariableRemovesItAndProducesAReplacement()
			throws SemanticException {
		// a plain Variable cannot survive a popScope (it must have been
		// introduced by the callee); this is a removal, not a renaming, and is
		// reported through MemoryValue#reachableOnlyFrom (here: identity)
		MemoryEnvironment<SingleMemoryLattice> env = mkEnv(x);

		Pair<MemoryEnvironment<SingleMemoryLattice>, List<MemoryReplacement>> result = env.popScope(token, fake);

		assertFalse(result.getLeft().knowsIdentifier(x));
		assertEquals(1, result.getRight().size());
		assertEquals(Set.of(x), result.getRight().get(0).getSources());
		assertTrue(result.getRight().get(0).getTargets().isEmpty());
	}

	@Test
	public void testForgetIdentifierProducesAnExpandedReplacement()
			throws SemanticException {
		MemoryEnvironment<SingleMemoryLattice> env = mkEnv(x);

		Pair<MemoryEnvironment<SingleMemoryLattice>, List<MemoryReplacement>> result = env.forgetIdentifier(x, fake);

		assertFalse(result.getLeft().knowsIdentifier(x));
		assertEquals(1, result.getRight().size());
		assertEquals(Set.of(x), result.getRight().get(0).getSources());
		assertTrue(result.getRight().get(0).getTargets().isEmpty());
	}

	@Test
	public void testForgetIdentifierOnTopOrBottomIsNoOp()
			throws SemanticException {
		MemoryEnvironment<SingleMemoryLattice> top = mkEnv().top();
		MemoryEnvironment<SingleMemoryLattice> bottom = mkEnv().bottom();

		assertTrue(top.forgetIdentifier(x, fake).getLeft().isTop());
		assertTrue(top.forgetIdentifier(x, fake).getRight().isEmpty());
		assertTrue(bottom.forgetIdentifier(x, fake).getLeft().isBottom());
	}

	@Test
	public void testKnowsIdentifier() {
		MemoryEnvironment<SingleMemoryLattice> env = mkEnv(x);

		assertTrue(env.knowsIdentifier(x));
		assertFalse(env.knowsIdentifier(g));
	}

}
