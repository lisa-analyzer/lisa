package it.unive.lisa.checks.syntactic;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.LiSA;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;

public class SyntacticCheckTest {

	private static class VariableI implements SyntacticCheck {

		@Override
		public void beforeExecution(CheckTool tool) {
		}

		@Override
		public void afterExecution(CheckTool tool) {
		}

		@Override
		public boolean visit(CheckTool tool, CFG graph, Statement node) {
			if (node instanceof VariableRef && ((VariableRef) node).getName().equals("i"))
				tool.warnOn(node, "Found variable i");
			return true;
		}

		@Override
		public boolean visit(CheckTool tool, CFG g) {
			return true;
		}

		@Override
		public boolean visit(CheckTool tool, CFG graph, Edge edge) {
			return true;
		}
	}

	@Test
	public void testSyntacticChecks() throws IOException, ParsingException {
		System.out.println("Testing syntactic checks...");
		LiSA lisa = new LiSA();
		lisa.addSyntacticCheck(new VariableI());

		Program program = IMPFrontend.processFile("imp-testcases/syntactic/expressions.imp");
		lisa.setProgram(program);
		lisa.setWorkdir("test-outputs/syntactic");
		lisa.setJsonOutput(true);
		try {
			lisa.run();
		} catch (AnalysisException e) {
			e.printStackTrace(System.err);
			fail("Analysis terminated with errors");
		}

		File expFile = new File("imp-testcases/syntactic/report.json");
		File actFile = new File("test-outputs/syntactic/report.json");
		try (FileReader l = new FileReader(expFile); FileReader r = new FileReader(actFile)) {
			JsonReport expected = JsonReport.read(l);
			JsonReport actual = JsonReport.read(r);
			assertTrue("Results are different",
					JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
			fail("Unable to find report file");
		} catch (IOException e) {
			e.printStackTrace(System.err);
			fail("Unable to compare reports");
		}
	}
}
