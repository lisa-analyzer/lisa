package it.unive.lisa.test.numeric;

import static it.unive.lisa.LiSAFactory.getDefaultFor;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.impl.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.analysis.impl.numeric.Parity;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import org.junit.Test;

public class NumericAnalysesTest {

	private final String filePath = "imp-testcases/numeric/program.imp";

	@Test
	public void testSign() throws IOException, ParsingException, AnalysisSetupException {
		System.out.println("Testing sign analysis...");
		LiSA lisa = new LiSA();

		Collection<CFG> cfgs = IMPFrontend.processFile(filePath);
		cfgs.forEach(lisa::addCFG);
		lisa.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Sign()));
		lisa.setDumpAnalysis(true);
		lisa.setJsonOutput(true);
		lisa.setWorkdir("test-outputs/numeric/sign");

		try {
			lisa.run();
		} catch (AnalysisException e) {
			System.err.println(e);
			fail("Analysis terminated with errors");
		}

		File actFile = new File("test-outputs/numeric/sign/report.json");
		File expFile = new File("imp-testcases/numeric/sign/report.json");
		JsonReport expected = JsonReport.read(new FileReader(expFile));
		JsonReport actual = JsonReport.read(new FileReader(actFile));

		assertTrue("Results are different",
				JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
	}

	@Test
	public void testParity() throws IOException, ParsingException, AnalysisSetupException {
		System.out.println("Testing parity analysis...");
		LiSA lisa = new LiSA();

		Collection<CFG> cfgs = IMPFrontend.processFile(filePath);
		cfgs.forEach(lisa::addCFG);
		lisa.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Parity()));
		lisa.setDumpAnalysis(true);
		lisa.setJsonOutput(true);
		lisa.setWorkdir("test-outputs/numeric/parity");

		try {
			lisa.run();
		} catch (AnalysisException e) {
			System.err.println(e);
			fail("Analysis terminated with errors");
		}

		File actFile = new File("test-outputs/numeric/parity/report.json");
		File expFile = new File("imp-testcases/numeric/parity/report.json");
		JsonReport expected = JsonReport.read(new FileReader(expFile));
		JsonReport actual = JsonReport.read(new FileReader(actFile));

		assertTrue("Results are different",
				JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
	}

	@Test
	public void testInterval() throws IOException, ParsingException, AnalysisSetupException {
		System.out.println("Testing interval analysis...");
		LiSA lisa = new LiSA();

		Collection<CFG> cfgs = IMPFrontend.processFile(filePath);
		cfgs.forEach(lisa::addCFG);
		lisa.setAbstractState(getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new Interval()));
		lisa.setDumpAnalysis(true);
		lisa.setJsonOutput(true);
		lisa.setWorkdir("test-outputs/numeric/interval");

		try {
			lisa.run();
		} catch (AnalysisException e) {
			System.err.println(e);
			fail("Analysis terminated with errors");
		}

		File actFile = new File("test-outputs/numeric/interval/report.json");
		File expFile = new File("imp-testcases/numeric/interval/report.json");
		JsonReport expected = JsonReport.read(new FileReader(expFile));
		JsonReport actual = JsonReport.read(new FileReader(actFile));

		assertTrue("Results are different",
				JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
	}

	@Test
	public void testIntegerConstantPropagation() throws IOException, ParsingException, AnalysisSetupException {
		System.out.println("Testing integer constant propagation...");
		LiSA lisa = new LiSA();

		Collection<CFG> cfgs = IMPFrontend.processFile(filePath);
		cfgs.forEach(lisa::addCFG);
		lisa.setAbstractState(
				getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new IntegerConstantPropagation()));
		lisa.setDumpAnalysis(true);
		lisa.setJsonOutput(true);
		lisa.setWorkdir("test-outputs/numeric/int-const");

		try {
			lisa.run();
		} catch (AnalysisException e) {
			System.err.println(e);
			fail("Analysis terminated with errors");
		}

		File actFile = new File("test-outputs/numeric/int-const/report.json");
		File expFile = new File("imp-testcases/numeric/int-const/report.json");
		JsonReport expected = JsonReport.read(new FileReader(expFile));
		JsonReport actual = JsonReport.read(new FileReader(actFile));

		assertTrue("Results are different",
				JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
	}
}
