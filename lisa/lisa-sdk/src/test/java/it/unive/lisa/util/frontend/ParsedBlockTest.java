package it.unive.lisa.util.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.junit.jupiter.api.Test;

public class ParsedBlockTest {

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
	public void testCanBeContinuedWithOrdinaryEnd() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> body = cfg.getNodeList();
		NoOp begin = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		body.addNode(begin);
		ParsedBlock block = new ParsedBlock(begin, body, begin);
		assertTrue(block.canBeContinued());
	}

	@Test
	public void testCanBeContinuedIsFalseWithNullEnd() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> body = cfg.getNodeList();
		NoOp begin = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		body.addNode(begin);
		ParsedBlock block = new ParsedBlock(begin, body, null);
		assertFalse(block.canBeContinued());
	}

	@Test
	public void testCanBeContinuedIsFalseWhenEndStopsExecution() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> body = cfg.getNodeList();
		Ret end = new Ret(cfg, new SourceCodeLocation("fake", 2, 0));
		body.addNode(end);
		ParsedBlock block = new ParsedBlock(end, body, end);
		assertFalse(block.canBeContinued());
	}

	@Test
	public void testCanBeContinuedIsFalseWhenEndBreaksControlFlow() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> body = cfg.getNodeList();
		BreakLike end = new BreakLike(cfg, new SourceCodeLocation("fake", 2, 0));
		body.addNode(end);
		ParsedBlock block = new ParsedBlock(end, body, end);
		assertFalse(block.canBeContinued());
	}

	@Test
	public void testAlwaysContinuesIsFalseWhenAnInnerStatementStopsExecution() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> body = cfg.getNodeList();
		NoOp begin = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		Ret inner = new Ret(cfg, new SourceCodeLocation("fake", 3, 0));
		NoOp end = new NoOp(cfg, new SourceCodeLocation("fake", 4, 0));
		body.addNode(begin);
		body.addNode(inner);
		body.addNode(end);
		ParsedBlock block = new ParsedBlock(begin, body, end);

		assertTrue(block.canBeContinued());
		assertFalse(block.alwaysContinues());
	}

	@Test
	public void testAlwaysContinuesIsTrueWithNoStoppingStatements() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> body = cfg.getNodeList();
		NoOp begin = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		body.addNode(begin);
		ParsedBlock block = new ParsedBlock(begin, body, begin);
		assertTrue(block.alwaysContinues());
	}

	@Test
	public void testEqualsAndHashCode() {
		CFG cfg = mkCfg();
		NodeList<CFG, Statement, Edge> body = cfg.getNodeList();
		NoOp begin = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		body.addNode(begin);

		ParsedBlock block1 = new ParsedBlock(begin, body, begin);
		ParsedBlock block2 = new ParsedBlock(begin, body, begin);
		NoOp otherEnd = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));
		ParsedBlock block3 = new ParsedBlock(begin, body, otherEnd);

		assertEquals(block1, block2);
		assertEquals(block1.hashCode(), block2.hashCode());
		assertNotEquals(block1, block3);
	}

}
