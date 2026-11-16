package it.unive.lisa.program.cfg.controlFlow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

public class ControlFlowStructureTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG newCfg() {
		ClassUnit unit = new ClassUnit(
				LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "unit", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "foo"));
	}

	// Statement#equals() is based on class + location only, and NoOp does not
	// add any further discriminating state: distinct NoOp nodes used together
	// (e.g. as different entries of the same NodeList/Set) must therefore be
	// built at distinct locations, or they collapse into a single node
	private static SourceCodeLocation loc(
			int col) {
		return new SourceCodeLocation("fake", 0, col);
	}

	@Test
	public void ifThenElseDistanceIsZeroAtConditionAndMinusOneOutsideTheStructure() {
		CFG cfg = newCfg();
		Statement condition = new NoOp(cfg, loc(0));
		Statement trueBody = new NoOp(cfg, loc(1));
		Statement falseBody = new NoOp(cfg, loc(2));
		Statement follower = new NoOp(cfg, loc(3));
		Statement outsider = new NoOp(cfg, loc(4));
		cfg.addNode(condition, true);
		cfg.addNode(trueBody);
		cfg.addNode(falseBody);
		cfg.addNode(follower);
		cfg.addEdge(new SequentialEdge(condition, trueBody));
		cfg.addEdge(new SequentialEdge(condition, falseBody));
		cfg.addEdge(new SequentialEdge(trueBody, follower));
		cfg.addEdge(new SequentialEdge(falseBody, follower));

		IfThenElse ith = new IfThenElse(
				cfg.getNodeList(), condition, follower, set(trueBody), set(falseBody));

		assertEquals(0, ith.distance(condition));
		assertEquals(1, ith.distance(trueBody));
		assertEquals(1, ith.distance(falseBody));
		// the follower is not in bodyStatements(), but distance() special-cases
		// it
		assertEquals(2, ith.distance(follower));
		// a statement that is neither the condition, the follower, nor in the
		// body must be reported as unresolvable, not confused with a real
		// distance
		assertEquals(-1, ith.distance(outsider));
	}

	@Test
	public void ifThenElseGetTargetedStatementsIncludesBranchEntriesAndFollower() {
		CFG cfg = newCfg();
		Statement condition = new NoOp(cfg, loc(0));
		Statement trueBody = new NoOp(cfg, loc(1));
		Statement falseBody = new NoOp(cfg, loc(2));
		Statement follower = new NoOp(cfg, loc(3));
		cfg.addNode(condition, true);
		cfg.addNode(trueBody);
		cfg.addNode(falseBody);
		cfg.addNode(follower);
		cfg.addEdge(new SequentialEdge(condition, trueBody));
		cfg.addEdge(new SequentialEdge(condition, falseBody));

		IfThenElse ith = new IfThenElse(
				cfg.getNodeList(), condition, follower, set(trueBody), set(falseBody));

		Collection<Statement> targeted = ith.getTargetedStatements();
		assertTrue(targeted.contains(trueBody));
		assertTrue(targeted.contains(falseBody));
		assertTrue(targeted.contains(follower));
	}

	@Test
	public void ifThenElseAddWithOnlyAffectsTheBranchContainingTheReference() {
		CFG cfg = newCfg();
		Statement condition = new NoOp(cfg, loc(0));
		Statement trueBody = new NoOp(cfg, loc(1));
		Statement falseBody = new NoOp(cfg, loc(2));
		Statement added = new NoOp(cfg, loc(3));

		IfThenElse ith = new IfThenElse(
				cfg.getNodeList(), condition, null, set(trueBody), set(falseBody));

		ith.addWith(added, trueBody);

		assertTrue(ith.getTrueBranch().contains(added));
		assertFalse(ith.getFalseBranch().contains(added));
	}

	@Test
	public void ifThenElseReplaceSwapsInWhicheverBranchHoldsTheOriginal() {
		CFG cfg = newCfg();
		Statement condition = new NoOp(cfg, loc(0));
		Statement trueBody = new NoOp(cfg, loc(1));
		Statement falseBody = new NoOp(cfg, loc(2));
		Statement replacement = new NoOp(cfg, loc(3));

		IfThenElse ith = new IfThenElse(
				cfg.getNodeList(), condition, null, set(trueBody), set(falseBody));

		ith.replace(falseBody, replacement);

		assertFalse(ith.getFalseBranch().contains(falseBody));
		assertTrue(ith.getFalseBranch().contains(replacement));
		assertTrue(ith.getTrueBranch().contains(trueBody));
	}

	@Test
	public void loopTargetedStatementsIncludeTheConditionItself() {
		CFG cfg = newCfg();
		Statement condition = new NoOp(cfg, loc(0));
		Statement body = new NoOp(cfg, loc(1));
		cfg.addNode(condition, true);
		cfg.addNode(body);
		cfg.addEdge(new SequentialEdge(condition, body));
		cfg.addEdge(new SequentialEdge(body, condition));

		Loop loop = new Loop(cfg.getNodeList(), condition, null, set(body));

		// a loop's condition is re-targeted by its own back-edge, unlike an
		// if-then-else's condition
		assertTrue(loop.getTargetedStatements().contains(condition));
		assertTrue(loop.getTargetedStatements().contains(body));
	}

	@Test
	public void sameConditionDifferentSubclassAreNotEqual() {
		CFG cfg = newCfg();
		Statement condition = new NoOp(cfg, loc(0));
		Statement body = new NoOp(cfg, loc(1));

		IfThenElse ith = new IfThenElse(cfg.getNodeList(), condition, null, set(body), set());
		Loop loop = new Loop(cfg.getNodeList(), condition, null, set(body));

		assertFalse(ith.equals(loop));
		assertFalse(loop.equals(ith));
	}

	@Test
	public void equalsAndHashCodeAreBasedOnConditionFollowerAndBody() {
		CFG cfg = newCfg();
		Statement condition = new NoOp(cfg, loc(0));
		Statement body = new NoOp(cfg, loc(1));
		Statement follower = new NoOp(cfg, loc(2));

		Loop a = new Loop(cfg.getNodeList(), condition, follower, set(body));
		Loop b = new Loop(cfg.getNodeList(), condition, follower, set(body));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		Loop differentFollower = new Loop(cfg.getNodeList(), condition, null, set(body));
		assertFalse(a.equals(differentFollower));
	}

	private static Collection<Statement> set(
			Statement... statements) {
		return new HashSet<>(Arrays.asList(statements));
	}

}
