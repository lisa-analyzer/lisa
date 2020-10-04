package it.unive.lisa.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import it.unive.lisa.LiSA;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFGDescriptor;
import it.unive.lisa.cfg.SequentialEdge;
import it.unive.lisa.cfg.statement.Assignment;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Literal;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.cfg.statement.Variable;
import it.unive.lisa.checks.CheckTool;
import it.unive.lisa.checks.syntactic.SyntacticCheck;
import it.unive.lisa.checks.warnings.Warning;

public class EmptyParametersTest {

	@Test
	public void testEmptyParameter() {
		LiSA lisa = new LiSA();
		lisa.addSyntacticCheck(new EmptyParameters());

		// x = 1; y = 2; z = 3; return x;
		CFG cfg = new CFG(new CFGDescriptor("f1", new String[0]));

		Assignment xAsg = new Assignment(cfg, new Variable(cfg, "x"), new Literal(cfg, 1));
		cfg.addNode(xAsg, true);

		Assignment yAsg = new Assignment(cfg, new Variable(cfg, "y"), new Literal(cfg, 2));
		cfg.addNode(yAsg);

		Assignment zAsg = new Assignment(cfg, new Variable(cfg, "z"), new Literal(cfg, 3));
		cfg.addNode(zAsg);

		Return ret = new Return(cfg, new Variable(cfg, "x")); 
		cfg.addNode(ret);

		cfg.addEdge(new SequentialEdge(xAsg, yAsg));
		cfg.addEdge(new SequentialEdge(yAsg, zAsg));
		cfg.addEdge(new SequentialEdge(zAsg, ret));

		// Check CFG node and edges sizes of f1
		assertEquals("Incorrect node size", 4, cfg.getNodes().size());	
		assertEquals("Incorrect edges size", 3, cfg.getEdges().size());
		
		lisa.addCFG(cfg);

		// x = 1; y = 2;
		cfg = new CFG(new CFGDescriptor("f2", new String[0]));

		xAsg = new Assignment(cfg, new Variable(cfg, "a"), new Literal(cfg, 1));
		cfg.addNode(xAsg, true);

		yAsg = new Assignment(cfg, new Variable(cfg, "y"), new Literal(cfg, 2));
		cfg.addNode(yAsg);

		cfg.addEdge(new SequentialEdge(xAsg, yAsg));

		lisa.addCFG(cfg);

		lisa.run();	
		
		// Check CFG node and edges sizes of f2
		assertEquals("Incorrect node size", 2, cfg.getNodes().size());	
		assertEquals("Incorrect edges size", 1, cfg.getEdges().size());

		for (Warning w : lisa.getWarnings())
			System.err.println(w);

		assertEquals("Incorrect number of warnings", 2, lisa.getWarnings().size());
	}

	/** 
	 * Syntactic check on CFG parameters: for each @CFG, a warning is raised on 
	 * @CFGDescriptor if it has no parameter.
	 * 
	 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
	 */
	private class EmptyParameters implements SyntacticCheck {
		
		@Override
		public void visitStatement(CheckTool tool, Statement statement) {}
		
		@Override
		public void visitCFGDescriptor(CheckTool tool, CFGDescriptor descriptor) {
			if (descriptor.getArgNames().length == 0)
				tool.warnOn(descriptor, descriptor.getName() + " has no arguments!");
		}
		
		@Override
		public void beforeExecution(CheckTool tool) {}
		
		@Override
		public void afterExecution(CheckTool tool) {}
		
		@Override
		public void visitExpression(CheckTool tool, Expression expression) {}
	}
}

