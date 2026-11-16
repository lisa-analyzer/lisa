package it.unive.lisa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LiSATest {

	private static LiSAConfiguration mkConfiguration(
			Path workdir) {
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.workdir = workdir.toString();
		conf.interproceduralAnalysis = new TestInterproceduralAnalysis<>();
		conf.callGraph = new TestCallGraph();
		conf.analysis = new TestAbstractDomain();
		return conf;
	}

	private static Program mkProgram() {
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		p.addUnit(new ClassUnit(new SourceCodeLocation("fake", 1, 0), p, "fake", false));
		return p;
	}

	@Test
	public void runReturnsAReportMatchingTheAnalyzedProgram(
			@TempDir Path workdir) {
		LiSAConfiguration conf = mkConfiguration(workdir);
		LiSA lisa = new LiSA(conf);

		LiSAReport report = lisa.run(mkProgram());

		assertNotNull(report);
		assertSame(conf, report.getConfiguration());
		assertEquals(1, report.getRunInfo().programs);
		assertEquals(1, report.getRunInfo().units);
	}

	@Test
	public void runWithoutInfoProviderDoesNotThrow(
			@TempDir Path workdir) {
		LiSAConfiguration conf = mkConfiguration(workdir);
		LiSA lisa = new LiSA(conf);

		assertDoesNotThrow(() -> lisa.run(mkProgram()));
	}

	@Test
	public void infoProviderIsInvokedWithAPopulatedReport(
			@TempDir Path workdir) {
		LiSAConfiguration conf = mkConfiguration(workdir);
		LiSA lisa = new LiSA(conf);

		List<LiSAReport> seen = new ArrayList<>();
		LiSAReport finalReport = lisa.run(seen::add, mkProgram());

		assertTrue(!seen.isEmpty(), "the info provider should have been invoked at least once");
		for (LiSAReport report : seen) {
			assertNotNull(report);
			assertEquals(1, report.getRunInfo().units);
		}
		assertNotNull(finalReport);
		assertEquals(1, finalReport.getRunInfo().units);
	}

}
