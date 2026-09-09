package it.unive.lisa.interprocedural;

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
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FixpointResults}, checking that it correctly dispatches to a
 * per-{@link CFG} {@link CFGResults} and that {@link CFGResults} instances for
 * different cfgs are independent of one another.
 */
public class FixpointResultsTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("unknown", 0, 0);

	private static final ClassUnit UNIT = new ClassUnit(
			LOC,
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"Testing",
			false);

	private static CFG mkCfg(
			String name) {
		return new CFG(new CodeMemberDescriptor(LOC, UNIT, false, name));
	}

	private static Ret mkRet(
			CFG cfg) {
		Ret ret = new Ret(cfg, LOC);
		cfg.addNode(ret, true);
		return ret;
	}

	private static AnalyzedCFG<TestAbstractState> mkResult(
			CFG cfg,
			ScopeId<TestAbstractState> id,
			Ret ret) {
		AnalysisState<TestAbstractState> proto = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		Map<Statement, AnalysisState<TestAbstractState>> results = Map.of(ret, proto);
		return new AnalyzedCFG<>(cfg, id, proto, Collections.emptyMap(), results);
	}

	@Test
	public void emptyFixpointResultsHaveNoEntryForAnyCfg()
			throws SemanticException {
		CFG cfg = mkCfg("m");
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		FixpointResults<TestAbstractState> results = new FixpointResults<>(
				new CFGResults<>(mkResult(cfg, id, ret)));

		assertFalse(results.contains(cfg));
		assertNull(results.get(cfg));
	}

	@Test
	public void putResultLazilyCreatesAPerCfgResultsAndDelegatesToIt()
			throws SemanticException {
		CFG cfg = mkCfg("m");
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		FixpointResults<TestAbstractState> results = new FixpointResults<>(
				new CFGResults<>(mkResult(cfg, id, ret)));

		AnalyzedCFG<TestAbstractState> result = mkResult(cfg, id, ret);
		Pair<Boolean, AnalyzedCFG<TestAbstractState>> outcome = results.putResult(cfg, id, result);

		assertFalse(outcome.getLeft(), "the very first result for a token should not be reported as an update");
		assertSame(result, outcome.getRight());
		assertTrue(results.contains(cfg));
		assertTrue(results.get(cfg).contains(id));
		assertSame(result, results.get(cfg).get(id));
	}

	@Test
	public void resultsForDifferentCfgsAreIndependent()
			throws SemanticException {
		CFG cfg1 = mkCfg("m1");
		Ret ret1 = mkRet(cfg1);
		CFG cfg2 = mkCfg("m2");
		Ret ret2 = mkRet(cfg2);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		FixpointResults<TestAbstractState> results = new FixpointResults<>(
				new CFGResults<>(mkResult(cfg1, id, ret1)));

		AnalyzedCFG<TestAbstractState> r1 = mkResult(cfg1, id, ret1);
		AnalyzedCFG<TestAbstractState> r2 = mkResult(cfg2, id, ret2);
		results.putResult(cfg1, id, r1);
		results.putResult(cfg2, id, r2);

		assertSame(r1, results.get(cfg1).get(id));
		assertSame(r2, results.get(cfg2).get(id));
	}

	@Test
	public void forgetRemovesAllResultsForACfgButKeepsOtherCfgsUntouched()
			throws SemanticException {
		CFG cfg1 = mkCfg("m1");
		Ret ret1 = mkRet(cfg1);
		CFG cfg2 = mkCfg("m2");
		Ret ret2 = mkRet(cfg2);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		FixpointResults<TestAbstractState> results = new FixpointResults<>(
				new CFGResults<>(mkResult(cfg1, id, ret1)));

		results.putResult(cfg1, id, mkResult(cfg1, id, ret1));
		AnalyzedCFG<TestAbstractState> r2 = mkResult(cfg2, id, ret2);
		results.putResult(cfg2, id, r2);

		results.forget(cfg1);

		assertFalse(results.contains(cfg1));
		assertNull(results.get(cfg1));
		assertTrue(results.contains(cfg2));
		assertSame(r2, results.get(cfg2).get(id));
	}

	@Test
	public void forgettingTheOnlyTrackedCfgClearsTheUnderlyingFunction()
			throws SemanticException {
		CFG cfg = mkCfg("m");
		Ret ret = mkRet(cfg);
		ScopeId<TestAbstractState> id = new UniqueScope<>();
		FixpointResults<TestAbstractState> results = new FixpointResults<>(
				new CFGResults<>(mkResult(cfg, id, ret)));

		results.putResult(cfg, id, mkResult(cfg, id, ret));
		results.forget(cfg);

		// forgetting the last tracked cfg must reset the function to null,
		// matching the "empty" representation used throughout this class
		// (see contains()/get(), which check function == null)
		assertNull(results.function);
	}

}
