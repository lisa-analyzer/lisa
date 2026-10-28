package it.unive.lisa.program.cfg.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Test;

public class ProtectedBlockTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG cfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "u", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	@Test
	public void canBeContinuedIsFalseWithoutAnEnd() {
		ProtectedBlock block = new ProtectedBlock(null, null, body());
		assertFalse(block.canBeContinued());
	}

	@Test
	public void canBeContinuedIsFalseWhenTheEndStopsExecution() {
		CFG cfg = cfg();
		Statement end = new Return(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		ProtectedBlock block = new ProtectedBlock(end, end, body(end));
		assertFalse(block.canBeContinued());
	}

	@Test
	public void canBeContinuedIsTrueWhenTheEndDoesNotStopExecution() {
		CFG cfg = cfg();
		Statement start = new NoOp(cfg, new SourceCodeLocation("fake", 0, 0));
		Statement end = new NoOp(cfg, new SourceCodeLocation("fake", 0, 1));
		ProtectedBlock block = new ProtectedBlock(start, end, body(start, end));
		assertTrue(block.canBeContinued());
	}

	@Test
	public void alwaysContinuesIsFalseIfAnyBodyStatementStopsExecutionEvenIfTheEndDoesNot() {
		CFG cfg = cfg();
		Statement start = new NoOp(cfg, new SourceCodeLocation("fake", 0, 0));
		Statement stopper = new Return(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		Statement end = new NoOp(cfg, new SourceCodeLocation("fake", 0, 1));
		ProtectedBlock block = new ProtectedBlock(start, end, body(start, stopper, end));
		// canBeContinued() only looks at the (unique) end statement, so it is
		// true here, but alwaysContinues() must also scan the whole body
		assertTrue(block.canBeContinued());
		assertFalse(block.alwaysContinues());
	}

	@Test
	public void alwaysContinuesIsTrueWhenNoBodyStatementInterruptsFlow() {
		CFG cfg = cfg();
		Statement start = new NoOp(cfg, new SourceCodeLocation("fake", 0, 0));
		Statement end = new NoOp(cfg, new SourceCodeLocation("fake", 0, 1));
		ProtectedBlock block = new ProtectedBlock(start, end, body(start, end));
		assertTrue(block.alwaysContinues());
	}

	private static Collection<Statement> body(
			Statement... statements) {
		return Arrays.asList(statements);
	}

}
