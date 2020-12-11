package it.unive.lisa.test.checks.syntactic;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.LiSA;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFGDescriptor;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.cfg.statement.Variable;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.outputs.compare.JsonReportComparer;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import org.junit.Test;

public class SyntacticCheckTest {

	private static class VariableI implements SyntacticCheck {

		@Override
		public void visitStatement(CheckTool tool, Statement statement) {
			if (statement instanceof Expression)
				visitExpression(tool, (Expression) statement);
		}

		@Override
		public void visitCFGDescriptor(CheckTool tool, CFGDescriptor descriptor) {
		}

		@Override
		public void beforeExecution(CheckTool tool) {
		}

		@Override
		public void afterExecution(CheckTool tool) {
		}

		@Override
		public void visitExpression(CheckTool tool, Expression expression) {
			if (expression instanceof Variable && ((Variable) expression).getName().equals("i"))
				tool.warnOn(expression, "Found variable i");
		}
	}

	@Test
	public void testSyntacticChecks() throws IOException, ParsingException {
		System.out.println("Testing syntactic checks...");
		LiSA lisa = new LiSA();
		lisa.addSyntacticCheck(new VariableI());

		Collection<CFG> cfgs = IMPFrontend.processFile("imp-testcases/syntactic/expressions.imp");
		cfgs.forEach(lisa::addCFG);
		lisa.setWorkdir("test-outputs/syntactic");
		lisa.setJsonOutput(true);
		try {
			lisa.run();
		} catch (AnalysisException e) {
			System.err.println(e);
			fail("Analysis terminated with errors");
		}

		File expFile = new File("imp-testcases/syntactic/report.json");
		File actFile = new File("test-outputs/syntactic/report.json");
		JsonReport expected = JsonReport.read(new FileReader(expFile));
		JsonReport actual = JsonReport.read(new FileReader(actFile));

		assertTrue("Results are different",
				JsonReportComparer.compare(expected, actual, expFile.getParentFile(), actFile.getParentFile()));
	}
}
