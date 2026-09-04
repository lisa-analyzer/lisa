package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class StatementStoreTest {

	private static final ClassUnit unit = new ClassUnit(
			new SourceCodeLocation("unknown", 0, 0),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"Testing",
			false);

	private static Statement mkStatement(
			int line) {
		SourceCodeLocation loc = new SourceCodeLocation("unknown", line, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(loc, unit, false, "m" + line));
		return new Ret(cfg, loc);
	}

	private static AnalysisState<TestAbstractState> mkState() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	@Test
	public void testPutOnFreshStoreInitializesFunctionAndReturnsNull() {
		StatementStore<TestAbstractState> store = new StatementStore<>(mkState());
		Statement s1 = mkStatement(1);
		AnalysisState<TestAbstractState> value = mkState();

		AnalysisState<TestAbstractState> previous = store.put(s1, value);

		assertNull(previous);
		assertEquals(value, store.getState(s1));
	}

	@Test
	public void testPutIsAForcedUpdateThatIgnoresLatticeOperations() {
		StatementStore<TestAbstractState> store = new StatementStore<>(mkState());
		Statement s1 = mkStatement(1);
		AnalysisState<TestAbstractState> first = mkState();
		AnalysisState<TestAbstractState> second = mkState();

		store.put(s1, first);
		AnalysisState<TestAbstractState> previous = store.put(s1, second);

		// put() overwrites unconditionally: the previous mapping is returned,
		// but no lub/merge is performed
		assertEquals(first, previous);
		assertEquals(second, store.getState(s1));
	}

	@Test
	public void testForgetRemovesTheMappingAndNormalizesToNullWhenEmpty() {
		StatementStore<TestAbstractState> store = new StatementStore<>(mkState());
		Statement s1 = mkStatement(1);
		store.put(s1, mkState());

		store.forget(s1);

		assertNull(store.function);
	}

	@Test
	public void testForgetOnEmptyStoreIsANoOp() {
		StatementStore<TestAbstractState> store = new StatementStore<>(mkState());
		store.forget(mkStatement(1));
		assertNull(store.function);
	}

	@Test
	public void testForgetKeepsFunctionWhenOtherMappingsRemain() {
		StatementStore<TestAbstractState> store = new StatementStore<>(mkState());
		Statement s1 = mkStatement(1);
		Statement s2 = mkStatement(2);
		store.put(s1, mkState());
		store.put(s2, mkState());

		store.forget(s1);

		assertTrue(store.function != null);
		assertTrue(store.function.containsKey(s2));
		assertFalse(store.function.containsKey(s1));
	}

	@Test
	public void testStateOfUnknownIsLatticeBottom() {
		StatementStore<TestAbstractState> store = new StatementStore<>(mkState());
		Statement mapped = mkStatement(1);
		Statement unmapped = mkStatement(2);
		// as soon as at least one mapping exists, the store is no longer
		// considered top (see FunctionalLattice#isTop()), so looking up a key
		// that was never put falls back to stateOfUnknown(), which
		// StatementStore defines as the underlying lattice's bottom
		store.put(mapped, mkState());

		assertEquals(store.lattice.bottom(), store.getState(unmapped));
		assertEquals(store.lattice.bottom(), store.stateOfUnknown(unmapped));
	}

	@Test
	public void testTopAndBottomAreRecognizedAsSuch() {
		StatementStore<TestAbstractState> store = new StatementStore<>(mkState());

		StatementStore<TestAbstractState> top = store.top();
		StatementStore<TestAbstractState> bottom = store.bottom();

		assertTrue(top.isTop());
		assertTrue(bottom.isBottom());
		assertFalse(top.isBottom());
		assertFalse(bottom.isTop());
	}

	@Test
	public void testMkBuildsAStoreWithTheGivenFunction() {
		StatementStore<TestAbstractState> store = new StatementStore<>(mkState());
		Statement s1 = mkStatement(1);
		AnalysisState<TestAbstractState> value = mkState();

		StatementStore<TestAbstractState> built = store.mk(store.lattice, Map.of(s1, value));

		assertEquals(value, built.getState(s1));
	}

}
