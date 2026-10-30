package it.unive.lisa.checks.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.MarkerAbstractDomain;
import it.unive.lisa.analysis.MarkerAbstractLattice;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.file.FileManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class VerificationTest {

	private static class StubVerifier
			implements
			WarningVerifier<MarkerAbstractLattice, MarkerAbstractDomain> {

		private final Map<AnalyzedCFG<MarkerAbstractLattice>, VerificationResult> answers = new IdentityHashMap<>();

		private int inits = 0;

		@Override
		public void init(
				SemanticTool<MarkerAbstractLattice, MarkerAbstractDomain> tool) {
			inits++;
		}

		@Override
		public VerificationResult verify(
				AnalyzedCFG<MarkerAbstractLattice> result,
				Statement st,
				Violation violation) {
			return answers.get(result);
		}

	}

	private static AnalysisState<MarkerAbstractLattice> state() {
		ProgramState<MarkerAbstractLattice> execution = new ProgramState<>(
				new MarkerAbstractLattice(new HashSet<>()),
				new ExpressionSet());
		return new AnalysisState<>(execution).bottom().withExecution(execution);
	}

	private static AnalyzedCFG<MarkerAbstractLattice> result(
			CFG cfg) {
		return new AnalyzedCFG<>(cfg, new UniqueScope<>(), state(), new HashMap<>(), new HashMap<>());
	}

	private static SemanticTool<MarkerAbstractLattice, MarkerAbstractDomain> tool(
			StubVerifier verifier,
			Map<CFG, Collection<AnalyzedCFG<MarkerAbstractLattice>>> results) {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.warningVerifier = verifier;
		return new SemanticTool<>(conf, new FileManager("verification-test"), results, null, null);
	}

	@Test
	public void testOutcomeLub() {
		VerificationOutcome[] outcomes = VerificationOutcome.values();
		for (VerificationOutcome first : outcomes)
			for (VerificationOutcome second : outcomes) {
				VerificationOutcome expected;
				if (first == VerificationOutcome.CONFIRMED || second == VerificationOutcome.CONFIRMED)
					expected = VerificationOutcome.CONFIRMED;
				else if (first == VerificationOutcome.REFUTED && second == VerificationOutcome.REFUTED)
					expected = VerificationOutcome.REFUTED;
				else
					expected = VerificationOutcome.POSSIBLE;
				assertEquals(expected, first.lub(second), first + " lub " + second);
			}
	}

	@Test
	public void testResultLub() {
		VerificationResult confirmed = new VerificationResult(VerificationOutcome.CONFIRMED, "a");
		VerificationResult possible = new VerificationResult(VerificationOutcome.POSSIBLE, "b");
		VerificationResult possible2 = new VerificationResult(VerificationOutcome.POSSIBLE, "c");

		assertSame(confirmed, confirmed.lub(possible));
		assertSame(confirmed, possible.lub(confirmed));
		assertSame(possible, VerificationResult.REFUTED.lub(possible));
		assertSame(possible, possible.lub(VerificationResult.REFUTED));
		assertSame(possible, possible.lub(VerificationResult.POSSIBLE));
		assertSame(possible, VerificationResult.POSSIBLE.lub(possible));
		assertEquals(new VerificationResult(VerificationOutcome.POSSIBLE, "b\nc"), possible.lub(possible2));
		assertEquals(VerificationResult.REFUTED, VerificationResult.REFUTED.lub(VerificationResult.REFUTED));
	}

	@Test
	public void testResultInputs() {
		ReproducingInput input = new ReproducingInput(new SourceCodeLocation("test", 1, 0), "x", null, 0, "42");
		VerificationResult withInputs = new VerificationResult(VerificationOutcome.CONFIRMED, null, List.of(input));
		VerificationResult withExplanation = new VerificationResult(VerificationOutcome.CONFIRMED, "a");

		assertEquals(List.of(input), withInputs.getReproducingInputs());
		assertSame(withInputs, withInputs.lub(VerificationResult.CONFIRMED));
		assertSame(withInputs, VerificationResult.CONFIRMED.lub(withInputs));
		assertEquals(new VerificationResult(VerificationOutcome.CONFIRMED, "a", List.of(input)),
				withExplanation.lub(withInputs));
		assertSame(withInputs, withInputs.lub(VerificationResult.POSSIBLE));
		assertThrows(IllegalArgumentException.class,
				() -> new VerificationResult(VerificationOutcome.POSSIBLE, null, List.of(input)));
	}

	@Test
	public void testNoVerifier() throws Exception {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		CFG cfg = new CFG(new CodeMemberDescriptor(new SourceCodeLocation("test", 1, 0), program, false, "f"));
		Statement st = new NoOp(cfg, new SourceCodeLocation("test", 2, 0));
		cfg.addNode(st, true);
		AnalyzedCFG<MarkerAbstractLattice> res = result(cfg);
		Map<CFG, Collection<AnalyzedCFG<MarkerAbstractLattice>>> results = new HashMap<>();
		results.put(cfg, List.of(res));

		SemanticTool<MarkerAbstractLattice, MarkerAbstractDomain> tool = tool(null, results);
		assertEquals(VerificationResult.POSSIBLE, tool.verify(res, st, Violation.reach()));
		assertEquals(VerificationResult.POSSIBLE, tool.verify(st, Violation.reach()));
	}

	@Test
	public void testDelegation() throws Exception {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		CFG cfg = new CFG(new CodeMemberDescriptor(new SourceCodeLocation("test", 1, 0), program, false, "f"));
		Statement st = new NoOp(cfg, new SourceCodeLocation("test", 2, 0));
		cfg.addNode(st, true);
		AnalyzedCFG<MarkerAbstractLattice> first = result(cfg);
		AnalyzedCFG<MarkerAbstractLattice> second = result(cfg);
		List<AnalyzedCFG<MarkerAbstractLattice>> list = new ArrayList<>();
		list.add(first);
		list.add(second);
		Map<CFG, Collection<AnalyzedCFG<MarkerAbstractLattice>>> results = new HashMap<>();
		results.put(cfg, list);

		StubVerifier verifier = new StubVerifier();
		SemanticTool<MarkerAbstractLattice, MarkerAbstractDomain> tool = tool(verifier, results);

		verifier.answers.put(first, VerificationResult.REFUTED);
		verifier.answers.put(second, VerificationResult.REFUTED);
		assertEquals(VerificationResult.REFUTED, tool.verify(first, st, Violation.reach()));
		assertEquals(VerificationResult.REFUTED, tool.verify(st, Violation.reach()));

		verifier.answers.put(second, VerificationResult.POSSIBLE);
		assertEquals(VerificationResult.POSSIBLE, tool.verify(st, Violation.error()));

		verifier.answers.put(first, VerificationResult.CONFIRMED);
		assertEquals(VerificationResult.CONFIRMED, tool.verify(st, Violation.error()));

		// the verifier is initialized only once per tool
		assertEquals(1, verifier.inits);
	}
}
