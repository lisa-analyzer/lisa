package it.unive.lisa.util.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.junit.jupiter.api.Test;

public class ControlFlowTrackerTest {

	private static class BreakLike
			extends
			NoOp {
		BreakLike(
				CFG cfg,
				CodeLocation location) {
			super(cfg, location);
		}

		@Override
		public boolean breaksControlFlow() {
			return true;
		}
	}

	private static class ContinueLike
			extends
			NoOp {
		ContinueLike(
				CFG cfg,
				CodeLocation location) {
			super(cfg, location);
		}

		@Override
		public boolean continuesControlFlow() {
			return true;
		}
	}

	private static CFG mkCfg() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("fake", 1, 0), program, "fake", false);
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(
				new SourceCodeLocation("fake", 1, 0),
				unit,
				false,
				"foo");
		return new CFG(descriptor);
	}

	@Test
	public void testAddModifierWithoutLabel() {
		ControlFlowTracker tracker = new ControlFlowTracker();
		CFG cfg = mkCfg();
		Statement modifier = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		tracker.addModifier(modifier);
		assertEquals(1, tracker.getModifiers().size());
		assertEquals(modifier, tracker.getModifiers().get(0).getLeft());
		assertEquals(null, tracker.getModifiers().get(0).getRight());
	}

	@Test
	public void testAddModifierWithLabel() {
		ControlFlowTracker tracker = new ControlFlowTracker();
		CFG cfg = mkCfg();
		Statement modifier = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		tracker.addModifier(modifier, "outer");
		assertEquals("outer", tracker.getModifiers().get(0).getRight());
	}

	@Test
	public void testEndControlFlowConnectsBreaksToBreakTargetAndRemovesModifier() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> list = cfg.getNodeList();
		BreakLike modifier = new BreakLike(cfg, new SourceCodeLocation("fake", 2, 0));
		Statement condition = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));
		Statement breakTarget = new NoOp(cfg, new SourceCodeLocation("fake", 4, 0));
		list.addNode(modifier);
		list.addNode(condition);
		list.addNode(breakTarget);

		ControlFlowTracker tracker = new ControlFlowTracker();
		tracker.addModifier(modifier);
		tracker.endControlFlowOf(list, condition, breakTarget, null, null);

		assertTrue(tracker.getModifiers().isEmpty());
		assertTrue(list.getOutgoingEdges(modifier).stream().anyMatch(e -> e.getDestination() == breakTarget));
	}

	@Test
	public void testEndControlFlowConnectsContinuesToContinueTarget() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> list = cfg.getNodeList();
		ContinueLike modifier = new ContinueLike(cfg, new SourceCodeLocation("fake", 2, 0));
		Statement condition = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));
		Statement continueTarget = new NoOp(cfg, new SourceCodeLocation("fake", 4, 0));
		list.addNode(modifier);
		list.addNode(condition);
		list.addNode(continueTarget);

		ControlFlowTracker tracker = new ControlFlowTracker();
		tracker.addModifier(modifier);
		tracker.endControlFlowOf(list, condition, null, continueTarget, null);

		assertTrue(list.getOutgoingEdges(modifier).stream().anyMatch(e -> e.getDestination() == continueTarget));
	}

	@Test
	public void testEndControlFlowOnlyAffectsModifiersMatchingTheGivenLabel() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> list = cfg.getNodeList();
		BreakLike labelled = new BreakLike(cfg, new SourceCodeLocation("fake", 2, 0));
		BreakLike unlabelled = new BreakLike(cfg, new SourceCodeLocation("fake", 3, 0));
		Statement condition = new NoOp(cfg, new SourceCodeLocation("fake", 4, 0));
		Statement breakTarget = new NoOp(cfg, new SourceCodeLocation("fake", 5, 0));
		list.addNode(labelled);
		list.addNode(unlabelled);
		list.addNode(condition);
		list.addNode(breakTarget);

		ControlFlowTracker tracker = new ControlFlowTracker();
		tracker.addModifier(labelled, "outer");
		tracker.addModifier(unlabelled);

		// unlabelled modifiers are always considered, together with those
		// matching the given label
		tracker.endControlFlowOf(list, condition, breakTarget, null, "somethingElse");

		assertEquals(1, tracker.getModifiers().size());
		assertEquals("outer", tracker.getModifiers().get(0).getRight());
	}

	@Test
	public void testEndControlFlowThrowsWhenModifierIsNotSupported() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> list = cfg.getNodeList();
		// a plain NoOp neither breaks nor continues control flow, so it
		// cannot be redirected to any target
		NoOp modifier = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		Statement condition = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));
		Statement target = new NoOp(cfg, new SourceCodeLocation("fake", 4, 0));
		list.addNode(modifier);
		list.addNode(condition);
		list.addNode(target);

		ControlFlowTracker tracker = new ControlFlowTracker();
		tracker.addModifier(modifier);

		assertThrows(
				IllegalStateException.class,
				() -> tracker.endControlFlowOf(list, condition, target, target, null));
	}

	@Test
	public void testEndControlFlowRemovesExistingOutgoingEdgesBeforeRedirecting() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> list = cfg.getNodeList();
		BreakLike modifier = new BreakLike(cfg, new SourceCodeLocation("fake", 2, 0));
		Statement staleTarget = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));
		Statement condition = new NoOp(cfg, new SourceCodeLocation("fake", 4, 0));
		Statement breakTarget = new NoOp(cfg, new SourceCodeLocation("fake", 5, 0));
		list.addNode(modifier);
		list.addNode(staleTarget);
		list.addNode(condition);
		list.addNode(breakTarget);
		list.addEdge(new SequentialEdge(modifier, staleTarget));

		ControlFlowTracker tracker = new ControlFlowTracker();
		tracker.addModifier(modifier);
		tracker.endControlFlowOf(list, condition, breakTarget, null, null);

		assertEquals(1, list.getOutgoingEdges(modifier).size());
		assertTrue(list.getOutgoingEdges(modifier).stream().anyMatch(e -> e.getDestination() == breakTarget));
	}

}
