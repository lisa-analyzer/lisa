package it.unive.lisa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.util.file.FileManager;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LiSARunnerTest {

	// records a warning for every unit it visits
	private static class RecordingSyntacticCheck
			implements
			SyntacticCheck {

		@Override
		public boolean visitUnit(
				ReportingTool tool,
				Unit unit) {
			tool.warn("syntactic:" + unit.getName());
			return true;
		}

	}

	// records a notice for every unit it visits
	private static class RecordingSemanticCheck
			implements
			SemanticCheck<TestAbstractState, TestAbstractDomain> {

		@Override
		public boolean visitUnit(
				SemanticTool<TestAbstractState, TestAbstractDomain> tool,
				Unit unit) {
			tool.notice("semantic:" + unit.getName());
			return true;
		}

	}

	// TestInterproceduralAnalysis always answers false to needsCallGraph();
	// this wrapper lets tests exercise the branch where a call graph is
	// mandatory
	private static class CallGraphMandatoryInterproceduralAnalysis
			extends
			TestInterproceduralAnalysis<TestAbstractState, TestAbstractDomain> {

		@Override
		public boolean needsCallGraph() {
			return true;
		}

	}

	private static Application mkApplication() {
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("fake", 1, 0), p, "fake", false);
		p.addUnit(unit);
		return new Application(p);
	}

	private static LiSARunner<TestAbstractState, TestAbstractDomain> mkRunner(
			LiSAConfiguration conf,
			Path workdir,
			InterproceduralAnalysis<TestAbstractState, TestAbstractDomain> interproc,
			CallGraph callGraph,
			Analysis<TestAbstractState, TestAbstractDomain> analysis) {
		return new LiSARunner<>(conf, new FileManager(workdir.toString()), interproc, callGraph, analysis);
	}

	@Test
	public void noInterproceduralAnalysisSkipsSemanticChecksButRunsSyntacticOnes(
			@TempDir Path workdir) {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.syntacticChecks.add(new RecordingSyntacticCheck());
		conf.semanticChecks.add(new RecordingSemanticCheck());

		LiSARunner<TestAbstractState, TestAbstractDomain> runner = mkRunner(conf, workdir, null, null, null);
		ReportingTool result = runner.run(mkApplication());

		assertTrue(result.getWarnings().stream().anyMatch(m -> m.getMessage().equals("syntactic:fake")));
		assertTrue(result.getNotices().isEmpty(), "semantic checks should have been skipped");
	}

	@Test
	public void noAnalysisSkipsSemanticChecksButRunsSyntacticOnes(
			@TempDir Path workdir) {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.syntacticChecks.add(new RecordingSyntacticCheck());
		conf.semanticChecks.add(new RecordingSemanticCheck());

		LiSARunner<TestAbstractState, TestAbstractDomain> runner = mkRunner(
				conf,
				workdir,
				new TestInterproceduralAnalysis<>(),
				new TestCallGraph(),
				null);
		ReportingTool result = runner.run(mkApplication());

		assertTrue(result.getWarnings().stream().anyMatch(m -> m.getMessage().equals("syntactic:fake")));
		assertTrue(result.getNotices().isEmpty(), "semantic checks should have been skipped");
	}

	@Test
	public void missingCallGraphThrowsWhenTheInterproceduralAnalysisNeedsOne(
			@TempDir Path workdir) {
		LiSAConfiguration conf = new LiSAConfiguration();

		LiSARunner<TestAbstractState, TestAbstractDomain> runner = mkRunner(
				conf,
				workdir,
				new CallGraphMandatoryInterproceduralAnalysis(),
				null,
				new Analysis<>(new TestAbstractDomain()));

		assertThrows(AnalysisSetupException.class, () -> runner.run(mkApplication()));
	}

	// regression test: a null callGraph must be tolerated whenever the
	// interprocedural analysis declares (via needsCallGraph()) that it does
	// not need one - this used to NPE unconditionally in LiSARunner.init()
	@Test
	public void missingCallGraphIsToleratedWhenTheInterproceduralAnalysisDoesNotNeedOne(
			@TempDir Path workdir) {
		LiSAConfiguration conf = new LiSAConfiguration();

		LiSARunner<TestAbstractState, TestAbstractDomain> runner = mkRunner(
				conf,
				workdir,
				new TestInterproceduralAnalysis<>(),
				null,
				new Analysis<>(new TestAbstractDomain()));

		ReportingTool result = assertDoesNotThrow(() -> runner.run(mkApplication()));
		assertTrue(result instanceof SemanticTool, "the analysis should have actually run");
	}

	@Test
	public void happyPathRunsBothSyntacticAndSemanticChecks(
			@TempDir Path workdir) {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.syntacticChecks.add(new RecordingSyntacticCheck());
		conf.semanticChecks.add(new RecordingSemanticCheck());

		LiSARunner<TestAbstractState, TestAbstractDomain> runner = mkRunner(
				conf,
				workdir,
				new TestInterproceduralAnalysis<>(),
				new TestCallGraph(),
				new Analysis<>(new TestAbstractDomain()));

		ReportingTool result = runner.run(mkApplication());

		assertEquals(1, result.getWarnings().size());
		assertTrue(result.getWarnings().stream().anyMatch(m -> m.getMessage().equals("syntactic:fake")));
		assertEquals(1, result.getNotices().size());
		assertTrue(result.getNotices().stream().anyMatch(m -> m.getMessage().equals("semantic:fake")));
	}

}
