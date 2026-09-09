package it.unive.lisa.lattices.informationFlow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.trie.PatriciaTrieMap;
import org.junit.jupiter.api.Test;

public class NonInterferenceEnvironmentTest {

	private final CodeLocation loc1 = new SourceCodeLocation("fake", 1, 1);

	private final CodeLocation loc2 = new SourceCodeLocation("fake", 2, 2);

	private final ProgramPoint pp1 = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return loc1;
		}

		@Override
		public CFG getCFG() {
			return TestParameterProvider.cfg;
		}
	};

	private final ProgramPoint pp2 = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return loc2;
		}

		@Override
		public CFG getCFG() {
			return TestParameterProvider.cfg;
		}
	};

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", loc1);

	private final Identifier y = new Variable(Untyped.INSTANCE, "y", loc1);

	private NonInterferenceEnvironment withGuards(
			GenericMapLattice<ProgramPoint, NonInterferenceValue> guards) {
		return new NonInterferenceEnvironment(NonInterferenceValue.HIGH_LOW, PatriciaTrieMap.empty(), guards);
	}

	private GenericMapLattice<ProgramPoint, NonInterferenceValue> emptyGuards() {
		return new GenericMapLattice<ProgramPoint, NonInterferenceValue>(NonInterferenceValue.LOW_HIGH).top();
	}

	@Test
	public void executionStateWithNoGuardsIsTheLowestNonBottomLevel() {
		assertEquals(NonInterferenceValue.LOW_HIGH, new NonInterferenceEnvironment().getExecutionState());
	}

	@Test
	public void executionStateIsTheLubOfAllGuardsStartingFromLowHigh()
			throws SemanticException {
		GenericMapLattice<ProgramPoint, NonInterferenceValue> guards = emptyGuards()
				.putState(pp1, NonInterferenceValue.HIGH_HIGH);
		assertEquals(NonInterferenceValue.HIGH_HIGH, withGuards(guards).getExecutionState());

		guards = guards.putState(pp2, NonInterferenceValue.LOW_LOW);
		// HIGH_HIGH lub LOW_LOW = HIGH_LOW (top of the diamond)
		assertEquals(NonInterferenceValue.HIGH_LOW, withGuards(guards).getExecutionState());
	}

	@Test
	public void topRequiresBothTheMapAndTheGuardsToBeTop() {
		NonInterferenceEnvironment top = new NonInterferenceEnvironment().top();
		assertTrue(top.isTop());

		NonInterferenceEnvironment onlyMapTop = withGuards(emptyGuards().bottom());
		assertFalse(onlyMapTop.isTop());
	}

	@Test
	public void bottomRequiresBothTheMapAndTheGuardsToBeBottom() {
		NonInterferenceEnvironment bottom = new NonInterferenceEnvironment().bottom();
		assertTrue(bottom.isBottom());
	}

	@Test
	public void lubCombinesBothTheMapAndTheGuards()
			throws SemanticException {
		PatriciaTrieMap<Identifier, NonInterferenceValue> f1 = PatriciaTrieMap.empty();
		f1 = f1.put(x, NonInterferenceValue.LOW_LOW);
		GenericMapLattice<ProgramPoint, NonInterferenceValue> g1 = emptyGuards().putState(pp1,
				NonInterferenceValue.LOW_LOW);
		NonInterferenceEnvironment e1 = new NonInterferenceEnvironment(NonInterferenceValue.HIGH_LOW, f1, g1);

		PatriciaTrieMap<Identifier, NonInterferenceValue> f2 = PatriciaTrieMap.empty();
		f2 = f2.put(x, NonInterferenceValue.HIGH_HIGH);
		GenericMapLattice<ProgramPoint, NonInterferenceValue> g2 = emptyGuards().putState(pp1,
				NonInterferenceValue.HIGH_HIGH);
		NonInterferenceEnvironment e2 = new NonInterferenceEnvironment(NonInterferenceValue.HIGH_LOW, f2, g2);

		NonInterferenceEnvironment lub = e1.lub(e2);
		assertEquals(NonInterferenceValue.HIGH_LOW, lub.getState(x));
		assertEquals(NonInterferenceValue.HIGH_LOW, lub.guards.getState(pp1));
	}

	@Test
	public void storeCopiesTheStateOfAKnownSourceIdentifier()
			throws SemanticException {
		PatriciaTrieMap<Identifier, NonInterferenceValue> f = PatriciaTrieMap.empty();
		f = f.put(x, NonInterferenceValue.LOW_LOW);
		NonInterferenceEnvironment env = new NonInterferenceEnvironment(NonInterferenceValue.HIGH_LOW, f,
				emptyGuards());

		NonInterferenceEnvironment stored = env.store(y, x);
		assertEquals(NonInterferenceValue.LOW_LOW, stored.getState(y));
	}

	@Test
	public void storeIsANoOpForAnUnknownSourceIdentifier()
			throws SemanticException {
		NonInterferenceEnvironment env = new NonInterferenceEnvironment(NonInterferenceValue.HIGH_LOW,
				PatriciaTrieMap.empty(),
				emptyGuards());
		assertSame(env, env.store(y, x));
	}

}
