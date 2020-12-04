package it.unive.lisa.test.checks.syntactic;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.LiSA;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFGDescriptor;
import it.unive.lisa.cfg.statement.Assignment;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Literal;
import it.unive.lisa.cfg.statement.OpenCall;
import it.unive.lisa.cfg.statement.Parameter;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.cfg.statement.Throw;
import it.unive.lisa.cfg.statement.Variable;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import java.util.function.Consumer;
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

	private static void testImpl(Consumer<CFG> cfgFiller, int expectedWarnings) {
		LiSA lisa = new LiSA();
		lisa.addSyntacticCheck(new VariableI());

		CFG cfg = new CFG(new CFGDescriptor("foo", new Parameter[0]));
		cfgFiller.accept(cfg);
		lisa.addCFG(cfg);
		lisa.run();

		assertEquals("Incorrect number of warnings", expectedWarnings, lisa.getWarnings().size());
	}

	@Test
	public void testVariable() {
		System.out.println("Testing variables");
		testImpl(cfg -> {
			cfg.addNode(new Variable(cfg, "i"));
			cfg.addNode(new Variable(cfg, "x"));
		}, 1);
	}

	@Test
	public void testThrow() {
		System.out.println("Testing throws");
		testImpl(cfg -> {
			cfg.addNode(new Throw(cfg, new Variable(cfg, "i")));
			cfg.addNode(new Throw(cfg, new Variable(cfg, "x")));
		}, 1);
	}

	@Test
	public void testReturn() {
		System.out.println("Testing returns");
		testImpl(cfg -> {
			cfg.addNode(new Return(cfg, new Variable(cfg, "i")));
			cfg.addNode(new Return(cfg, new Variable(cfg, "x")));
		}, 1);
	}

	@Test
	public void testAssignment() {
		System.out.println("Testing assignments");
		testImpl(cfg -> {
			cfg.addNode(new Assignment(cfg, new Variable(cfg, "i"), new Literal(cfg, 5)));
			cfg.addNode(new Assignment(cfg, new Variable(cfg, "x"), new Variable(cfg, "i")));
		}, 2);
	}

	@Test
	public void testCall() {
		System.out.println("Testing calls");
		testImpl(cfg -> {
			cfg.addNode(new OpenCall(cfg, "test", new Literal(cfg, 5)));
			cfg.addNode(new OpenCall(cfg, "test", new Variable(cfg, "i"), new Literal(cfg, 5)));
		}, 1);
	}

	@Test
	public void testNestedCall() {
		System.out.println("Testing nested calls");
		testImpl(cfg -> {
			cfg.addNode(new OpenCall(cfg, "test", new Literal(cfg, 5),
					new OpenCall(cfg, "test", new Variable(cfg, "i"), new Literal(cfg, 5))));
			cfg.addNode(new OpenCall(cfg, "test", new Variable(cfg, "x"),
					new OpenCall(cfg, "test", new Variable(cfg, "i"))));
			cfg.addNode(new Return(cfg, new OpenCall(cfg, "test", new Variable(cfg, "i"))));
			cfg.addNode(new Throw(cfg, new OpenCall(cfg, "test", new Variable(cfg, "i"))));
			cfg.addNode(new Assignment(cfg, new Variable(cfg, "x"), new OpenCall(cfg, "test", new Variable(cfg, "i"))));
		}, 5);
	}
}
