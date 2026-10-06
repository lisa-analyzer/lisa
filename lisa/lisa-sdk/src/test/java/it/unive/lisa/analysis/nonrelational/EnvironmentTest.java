package it.unive.lisa.analysis.nonrelational;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.SingleValueLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Environment}, exercised through {@link ValueEnvironment} since
 * {@link Environment} is abstract.
 */
public class EnvironmentTest {

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

	private final Variable y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

	private final GlobalVariable g = new GlobalVariable(Untyped.INSTANCE, "g", SyntheticLocation.INSTANCE);

	private ValueEnvironment<SingleValueLattice> mkEnv(
			Identifier... ids) {
		ValueEnvironment<SingleValueLattice> env = new ValueEnvironment<>(SingleValueLattice.BOTTOM);
		for (Identifier id : ids)
			env = env.putState(id, SingleValueLattice.SINGLETON);
		return env;
	}

	@Test
	public void testPushScopeRenamesScopableIdentifiers()
			throws SemanticException {
		ValueEnvironment<SingleValueLattice> env = mkEnv(x);
		ValueEnvironment<SingleValueLattice> pushed = env.pushScope(token, fake);

		assertFalse(pushed.knowsIdentifier(x), "the original identifier should no longer be tracked");
		Identifier lifted = (Identifier) x.pushScope(token, fake);
		assertTrue(pushed.knowsIdentifier(lifted), "the scoped identifier should be tracked instead");
		assertEquals(SingleValueLattice.SINGLETON, pushed.getState(lifted));
	}

	@Test
	public void testPushPopScopeRoundtrip()
			throws SemanticException {
		ValueEnvironment<SingleValueLattice> env = mkEnv(x);
		ValueEnvironment<SingleValueLattice> back = env.pushScope(token, fake).popScope(token, fake);

		assertTrue(back.knowsIdentifier(x), "the identifier should be restored after the roundtrip");
		assertEquals(SingleValueLattice.SINGLETON, back.getState(x));
	}

	@Test
	public void testPushScopeIsNoOpForUnscopableIdentifiers()
			throws SemanticException {
		ValueEnvironment<SingleValueLattice> env = mkEnv(g);
		ValueEnvironment<SingleValueLattice> pushed = env.pushScope(token, fake);

		assertTrue(pushed.knowsIdentifier(g), "global variables cannot be scoped, so they must survive as-is");
	}

	@Test
	public void testPopScopeDropsIdentifiersFromOtherScopes()
			throws SemanticException {
		// a plain (not-yet-scoped) local variable cannot survive a popScope: it
		// must have been introduced by the callee and is out of scope for the
		// caller once execution returns to it
		ValueEnvironment<SingleValueLattice> env = mkEnv(x);
		ValueEnvironment<SingleValueLattice> popped = env.popScope(token, fake);

		assertFalse(popped.knowsIdentifier(x));
		assertTrue(popped.getKeys().isEmpty());
	}

	@Test
	public void testForgetIdentifier()
			throws SemanticException {
		ValueEnvironment<SingleValueLattice> env = mkEnv(x, y);
		ValueEnvironment<SingleValueLattice> forgot = env.forgetIdentifier(x, fake);

		assertFalse(forgot.knowsIdentifier(x));
		assertTrue(forgot.knowsIdentifier(y));
	}

	@Test
	public void testForgetIdentifiers()
			throws SemanticException {
		ValueEnvironment<SingleValueLattice> env = mkEnv(x, y, g);
		ValueEnvironment<SingleValueLattice> forgot = env.forgetIdentifiers(List.of(x, y), fake);

		assertFalse(forgot.knowsIdentifier(x));
		assertFalse(forgot.knowsIdentifier(y));
		assertTrue(forgot.knowsIdentifier(g));
	}

	@Test
	public void testForgetIdentifiersIf()
			throws SemanticException {
		ValueEnvironment<SingleValueLattice> env = mkEnv(x, y, g);
		ValueEnvironment<SingleValueLattice> forgot = env.forgetIdentifiersIf(id -> id.getName().equals("x"), fake);

		assertFalse(forgot.knowsIdentifier(x));
		assertTrue(forgot.knowsIdentifier(y));
		assertTrue(forgot.knowsIdentifier(g));
	}

	@Test
	public void testForgetOnTopAndBottomIsNoOp()
			throws SemanticException {
		ValueEnvironment<
				SingleValueLattice> top = new ValueEnvironment<SingleValueLattice>(SingleValueLattice.SINGLETON)
						.top();
		ValueEnvironment<SingleValueLattice> bottom = new ValueEnvironment<SingleValueLattice>(
				SingleValueLattice.SINGLETON).bottom();

		assertTrue(top.forgetIdentifier(x, fake).isTop());
		assertTrue(bottom.forgetIdentifier(x, fake).isBottom());
	}

	@Test
	public void testLubKeysMergesIdenticalIdentifiers()
			throws SemanticException {
		Environment<SingleValueLattice, ValueEnvironment<SingleValueLattice>> env = mkEnv();
		Set<Identifier> k1 = new HashSet<>(Set.of(x));
		Set<Identifier> k2 = new HashSet<>(Set.of(x, y));

		Set<Identifier> lub = env.lubKeys(k1, k2);

		assertEquals(Set.of(x, y), lub);
	}

	@Test
	public void testLubKeysOfDifferentKindsWithSameNameFails() {
		// Identifier#lub requires the two operands to be equal (same class and
		// name); a name-only match across different identifier kinds is thus
		// not mergeable
		Environment<SingleValueLattice, ValueEnvironment<SingleValueLattice>> env = mkEnv();
		Set<Identifier> k1 = new HashSet<>(Set.of(x));
		Set<Identifier> k2 = new HashSet<>(
				Set.of(new GlobalVariable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE)));

		assertThrows(SemanticException.class, () -> env.lubKeys(k1, k2));
	}

	@Test
	public void testKnowsIdentifierAndStateOfUnknown() {
		ValueEnvironment<SingleValueLattice> env = mkEnv(x);

		assertTrue(env.knowsIdentifier(x));
		assertFalse(env.knowsIdentifier(y));
		// an environment with a non-top, non-bottom domain instance falls back
		// to the domain's #unknownValue for identifiers it does not track
		assertEquals(SingleValueLattice.SINGLETON, env.stateOfUnknown(y));
	}

	@Test
	public void testOutOfScopeIdentifierWrapping()
			throws SemanticException {
		ValueEnvironment<SingleValueLattice> env = mkEnv(x);
		ValueEnvironment<SingleValueLattice> pushed = env.pushScope(token, fake);

		Identifier lifted = pushed.getKeys().iterator().next();
		assertTrue(lifted instanceof OutOfScopeIdentifier);
		assertEquals(token, ((OutOfScopeIdentifier) lifted).getScope());
	}

}
