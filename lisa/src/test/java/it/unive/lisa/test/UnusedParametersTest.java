package it.unive.lisa.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

public class UnusedParametersTest {

	@Test
	public void testUnusedParametersWithoutFlows() {
		LiSA lisa = new LiSA();
		lisa.addSyntacticCheck(new UnusedParameterSyntacticCheck());

		// f1(x,y,z) {x = 1; y = 2; z = 3; return x;}
		CFG cfg = new CFG(new CFGDescriptor("f1", new String[]{"x", "y", "z"}));

		Assignment xAsg = new Assignment(cfg, new Variable(cfg, "x"), new Literal(cfg, 1));
		cfg.addNode(xAsg);

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

		// f2(x,y,z) { x = 1; y = 2; }
		cfg = new CFG(new CFGDescriptor("f2", new String[]{"x", "y", "z"}));

		xAsg = new Assignment(cfg, new Variable(cfg, "x"), new Literal(cfg, 1));
		cfg.addNode(xAsg);

		yAsg = new Assignment(cfg, new Variable(cfg, "y"), new Literal(cfg, 2));
		cfg.addNode(yAsg);

		cfg.addEdge(new SequentialEdge(xAsg, yAsg));

		lisa.addCFG(cfg);

		lisa.run();	

		// Check CFG node and edges sizes of f2
		assertEquals("Incorrect node size", 2, cfg.getNodes().size());	
		assertEquals("Incorrect edges size", 1, cfg.getEdges().size());

		// f3() {}, empty function
		cfg = new CFG(new CFGDescriptor("f3", new String[0]));
		lisa.addCFG(cfg);

		lisa.run();	

		// Check CFG node and edges sizes of f3
		assertEquals("Incorrect node size", 0, cfg.getNodes().size());	
		assertEquals("Incorrect edges size", 0, cfg.getEdges().size());

		for (Warning w : lisa.getWarnings())
			System.err.println(w);

		assertEquals("Incorrect number of warnings", 1, lisa.getWarnings().size());
	}

	/** 
	 * Syntactic check on CFG parameters: for each @CFG, a warning is raised on 
	 * @CFGDescriptor for each parameter that is not used inside the body
	 * of the @CFG.
	 * 
	 * Examples:
	 * 	f1(x, y, z) { x = 1; y = 2; z = z + x + y;} has no warning
	 * 	f2(x, y, z) { x = 1; y = 2; z = 3;} has no warning
	 * 	f3(x, y, z) { x = 1; y = 2;} raises a warning (z not used)
	 * 
	 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
	 */
	private class UnusedParameterSyntacticCheck implements SyntacticCheck {

		private Map<CFGDescriptor, Set<String>> usedVariables;

		@Override
		public void visitCFGDescriptor(CheckTool tool, CFGDescriptor descriptor) {
			usedVariables.put(descriptor, new HashSet<>());
		}

		@Override
		public void visitStatement(CheckTool tool, Statement statement) {}


		@Override
		public void beforeExecution(CheckTool tool) {
			usedVariables = new HashMap<>();
		}

		@Override
		public void afterExecution(CheckTool tool) {
			for (CFGDescriptor descriptor : usedVariables.keySet())
				for (String formalPar : descriptor.getArgNames())
					if (!usedVariables.get(descriptor).contains(formalPar))
						tool.warnOn(descriptor, "Formal parameter " + formalPar + " not used inside body of " + descriptor.getName());
			usedVariables.clear();
		}

		@Override
		public void visitExpression(CheckTool tool, Expression expression) {
			if (expression instanceof Variable)
				usedVariables.get(expression.getCFG().getDescriptor()).add(((Variable) expression).getName());
		}
	}
}
