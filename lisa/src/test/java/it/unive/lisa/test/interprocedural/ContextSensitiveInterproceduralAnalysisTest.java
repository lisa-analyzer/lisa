package it.unive.lisa.test.interprocedural;

import static it.unive.lisa.LiSAFactory.getDefaultFor;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.interprocedural.callgraph.impl.RTACallGraph;
import it.unive.lisa.interprocedural.impl.CallPointContextSensitiveToken;
import it.unive.lisa.interprocedural.impl.ContextSensitiveInterproceduralAnalysis;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.program.Program;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.junit.Test;

public class ContextSensitiveInterproceduralAnalysisTest {

	private final String filePath1 = "imp-testcases/interprocedural/programContextSensitive1.imp";

	private final String filePath2 = "imp-testcases/interprocedural/programContextSensitive2.imp";

	private final String filePath3 = "imp-testcases/interprocedural/programContextSensitive3.imp";

	@Test
	public void testRTAContextSensitive1() throws IOException, ParsingException, AnalysisSetupException {
		System.out.println("Testing context sensitive interprocedural analysis with RTA call graph...");
		LiSA lisa = new LiSA();

		Program program = IMPFrontend.processFile(filePath1, true);
		lisa.setProgram(program);
		lisa.setInferTypes(true);
		lisa.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()));
		lisa.setDumpAnalysis(true);
		lisa.setJsonOutput(true);
		lisa.setInterproceduralAnalysis(
				new ContextSensitiveInterproceduralAnalysis(CallPointContextSensitiveToken.getSingleton()));
		lisa.setCallGraph(new RTACallGraph());
		lisa.setWorkdir("test-outputs/interprocedural/RTAContextSensitive1");

		try {
			lisa.run();
		} catch (AnalysisException e) {
			e.printStackTrace(System.err);
			fail("Analysis terminated with errors");
		}

		File actFile = new File("test-outputs/interprocedural/RTAContextSensitive1/report.json");
		File expFile = new File("imp-testcases/interprocedural/RTAContextSensitive1/report.json");
		JsonReport expected = JsonReport.read(new FileReader(expFile));
		JsonReport actual = JsonReport.read(new FileReader(actFile));

		assertTrue("Results are different",
				JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
	}

	@Test
	public void testRTAContextSensitive2() throws IOException, ParsingException, AnalysisSetupException {
		System.out.println("Testing context sensitive interprocedural analysis with RTA call graph...");
		LiSA lisa = new LiSA();

		Program program = IMPFrontend.processFile(filePath2, true);
		lisa.setProgram(program);
		lisa.setInferTypes(true);
		lisa.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()));
		lisa.setDumpAnalysis(true);
		lisa.setJsonOutput(true);
		lisa.setInterproceduralAnalysis(
				new ContextSensitiveInterproceduralAnalysis(CallPointContextSensitiveToken.getSingleton()));
		lisa.setCallGraph(new RTACallGraph());
		lisa.setWorkdir("test-outputs/interprocedural/RTAContextSensitive2");

		try {
			lisa.run();
		} catch (AnalysisException e) {
			e.printStackTrace(System.err);
			fail("Analysis terminated with errors");
		}

		File actFile = new File("test-outputs/interprocedural/RTAContextSensitive2/report.json");
		File expFile = new File("imp-testcases/interprocedural/RTAContextSensitive2/report.json");
		JsonReport expected = JsonReport.read(new FileReader(expFile));
		JsonReport actual = JsonReport.read(new FileReader(actFile));

		assertTrue("Results are different",
				JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
	}

	@Test
	public void testRTAContextSensitive3() throws IOException, ParsingException, AnalysisSetupException {
		System.out.println("Testing context sensitive interprocedural analysis with RTA call graph...");
		LiSA lisa = new LiSA();

		Program program = IMPFrontend.processFile(filePath3, true);
		lisa.setProgram(program);
		lisa.setInferTypes(true);
		lisa.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()));
		lisa.setDumpAnalysis(true);
		lisa.setJsonOutput(true);
		lisa.setInterproceduralAnalysis(
				new ContextSensitiveInterproceduralAnalysis(CallPointContextSensitiveToken.getSingleton()));
		lisa.setCallGraph(new RTACallGraph());
		lisa.setWorkdir("test-outputs/interprocedural/RTAContextSensitive3");

		try {
			lisa.run();
		} catch (AnalysisException e) {
			e.printStackTrace(System.err);
			fail("Analysis terminated with errors");
		}

		File actFile = new File("test-outputs/interprocedural/RTAContextSensitive3/report.json");
		File expFile = new File("imp-testcases/interprocedural/RTAContextSensitive3/report.json");
		JsonReport expected = JsonReport.read(new FileReader(expFile));
		JsonReport actual = JsonReport.read(new FileReader(actFile));

		assertTrue("Results are different",
				JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
	}

}
