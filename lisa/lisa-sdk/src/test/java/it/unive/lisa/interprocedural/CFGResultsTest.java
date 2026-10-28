package it.unive.lisa.interprocedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CFGResults#putResult(ScopeId, AnalyzedCFG)}, exercising the
 * four cases documented in its javadoc using genuinely comparable and
 * incomparable {@link AnalyzedCFG}s (built by varying the set of pending
 * expressions computed at the same statement, whose subset ordering is a real
 * partial order, unlike {@link TestAbstractState} which considers everything
 * mutually less-or-equal).
 */
public class CFGResultsTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("unknown", 0, 0);

	private static final ClassUnit UNIT = new ClassUnit(
			LOC,
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"Testing",
			false);

	private static Constant constant(
			int value) {
		return new Constant(Untyped.INSTANCE, value, LOC);
	}

	/**
	 * Builds an {@link AnalyzedCFG} over {@code cfg} and {@code id}, with an
	 * empty entry state and, as the only result (at {@code ret}), a state whose
	 * pending expressions are exactly {@code values}.
	 */
	private static AnalyzedCFG<TestAbstractState> mkResult(
			CFG cfg,
			ScopeId<TestAbstractState> id,
			Ret ret,
			int... values) {
		AnalysisState<TestAbstractState> proto = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		Set<SymbolicExpression> constants = new HashSet<>();
		for (int v : values)
			constants.add(constant(v));
		AnalysisState<TestAbstractState> state = proto.withExecutionExpressions(new ExpressionSet(constants));
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(ret, state);
		return new AnalyzedCFG<>(cfg, id, proto, Collections.emptyMap(), results);
	}

	private static CFG mkCfg() {
		return new CFG(new CodeMemberDescriptor(LOC, UNIT, false, "m"));
	}

	private static Ret mkRet(
			CFG cfg) {
		Ret ret = new Ret(cfg, LOC);
		cfg.addNode(ret, true);
		return ret;
	}

	@Test
	public void emptyResultsHaveNoEntryForAnyToken()
			throws SemanticException {
		CFG cfg = mkCfg();
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		CFGResults<TestAbstractState> results = new CFGResults<>(mkResult(cfg, id, ret));

		assertFalse(results.contains(id));
		assertNull(results.get(id));
		assertTrue(results.getAll().isEmpty());
	}

	@Test
	public void firstResultForATokenIsStoredAsIsAndReportedAsNotUpdated()
			throws SemanticException {
		CFG cfg = mkCfg();
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		CFGResults<TestAbstractState> results = new CFGResults<>(mkResult(cfg, id, ret));

		AnalyzedCFG<TestAbstractState> first = mkResult(cfg, id, ret);
		Pair<Boolean, AnalyzedCFG<TestAbstractState>> outcome = results.putResult(id, first);

		assertFalse(outcome.getLeft(), "storing the very first result should not be reported as an update");
		assertSame(first, outcome.getRight());
		assertTrue(results.contains(id));
		assertSame(first, results.get(id));
	}

	@Test
	public void storingAStrictlyBiggerResultUpdatesTheMappingAndReportsAnUpdate()
			throws SemanticException {
		CFG cfg = mkCfg();
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		CFGResults<TestAbstractState> results = new CFGResults<>(mkResult(cfg, id, ret));

		// {}
		results.putResult(id, mkResult(cfg, id, ret));
		// {1} >= {}
		AnalyzedCFG<TestAbstractState> bigger = mkResult(cfg, id, ret, 1);
		Pair<Boolean, AnalyzedCFG<TestAbstractState>> outcome = results.putResult(id, bigger);

		assertTrue(outcome.getLeft(), "a strictly bigger result should be reported as an update");
		assertSame(bigger, outcome.getRight());
		assertSame(bigger, results.get(id));
	}

	@Test
	public void storingAResultSmallerThanTheStoredOneLeavesTheMappingUntouched()
			throws SemanticException {
		CFG cfg = mkCfg();
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		CFGResults<TestAbstractState> results = new CFGResults<>(mkResult(cfg, id, ret));

		// {1}
		AnalyzedCFG<TestAbstractState> bigger = mkResult(cfg, id, ret, 1);
		results.putResult(id, bigger);
		// {} <= {1}
		AnalyzedCFG<TestAbstractState> smaller = mkResult(cfg, id, ret);
		Pair<Boolean, AnalyzedCFG<TestAbstractState>> outcome = results.putResult(id, smaller);

		assertFalse(outcome.getLeft(), "a smaller result should not overwrite a bigger stored one");
		assertSame(bigger, outcome.getRight());
		assertSame(bigger, results.get(id));
	}

	@Test
	public void storingAnEquivalentResultIsNotReportedAsAnUpdateAndKeepsTheStoredInstance()
			throws SemanticException {
		CFG cfg = mkCfg();
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		CFGResults<TestAbstractState> results = new CFGResults<>(mkResult(cfg, id, ret));

		AnalyzedCFG<TestAbstractState> stored = mkResult(cfg, id, ret, 1);
		results.putResult(id, stored);
		// same content, different instance
		AnalyzedCFG<TestAbstractState> equivalent = mkResult(cfg, id, ret, 1);
		Pair<Boolean, AnalyzedCFG<TestAbstractState>> outcome = results.putResult(id, equivalent);

		assertFalse(outcome.getLeft());
		// the previously stored instance is returned, not the new (albeit
		// equivalent) one
		assertSame(stored, outcome.getRight());
		assertSame(stored, results.get(id));
	}

	@Test
	public void storingAnIncomparableResultReplacesTheMappingWithTheirLubAndReportsAnUpdate()
			throws SemanticException {
		CFG cfg = mkCfg();
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		CFGResults<TestAbstractState> results = new CFGResults<>(mkResult(cfg, id, ret));

		AnalyzedCFG<TestAbstractState> withOne = mkResult(cfg, id, ret, 1);
		results.putResult(id, withOne);
		// neither {1} <= {2} nor {2} <= {1}
		AnalyzedCFG<TestAbstractState> withTwo = mkResult(cfg, id, ret, 2);
		Pair<Boolean, AnalyzedCFG<TestAbstractState>> outcome = results.putResult(id, withTwo);

		assertTrue(outcome.getLeft(), "incomparable results should be lubbed and reported as an update");
		AnalyzedCFG<TestAbstractState> stored = results.get(id);
		assertSame(stored, outcome.getRight());
		assertEquals(
				Set.of(constant(1), constant(2)),
				stored.getAnalysisStateAfter(ret).getExecutionExpressions().elements());
	}

	@Test
	public void resultsForDifferentTokensAreIndependent()
			throws SemanticException {
		CFG cfg = mkCfg();
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id1 = new UniqueScope<>();
		ScopeId<TestAbstractState> id2 = new UniqueScope<>();
		CFGResults<TestAbstractState> results = new CFGResults<>(mkResult(cfg, id1, ret));

		AnalyzedCFG<TestAbstractState> r1 = mkResult(cfg, id1, ret, 1);
		AnalyzedCFG<TestAbstractState> r2 = mkResult(cfg, id2, ret, 2);
		results.putResult(id1, r1);
		results.putResult(id2, r2);

		assertSame(r1, results.get(id1));
		assertSame(r2, results.get(id2));
		assertEquals(2, results.getAll().size());
	}

}
