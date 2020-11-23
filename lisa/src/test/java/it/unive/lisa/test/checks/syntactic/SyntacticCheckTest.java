package it.unive.lisa.test.checks.syntactic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.LiSA;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFGDescriptor;
import it.unive.lisa.cfg.Parameter;
import it.unive.lisa.cfg.statement.Assignment;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Literal;
import it.unive.lisa.cfg.statement.OpenCall;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.cfg.statement.Throw;
import it.unive.lisa.cfg.statement.Variable;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.test.imp.IMPFrontend;

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
	public void testSyntacticChecks() throws IOException {
		LiSA lisa = new LiSA();
		lisa.addSyntacticCheck(new VariableI());

		Collection<CFG> cfgs = IMPFrontend.processFile("imp-testcases/syntactic/expressions.imp");
		cfgs.forEach(lisa::addCFG);
		try {
			lisa.run();
		} catch (AnalysisException e) {
			System.err.println(e);
			fail("Analysis terminated with errors");
		}

		assertEquals("Incorrect number of warnings", 9, lisa.getWarnings().size());
	}
}
