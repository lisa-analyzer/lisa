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
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import org.junit.jupiter.api.Test;

public class CFGTweakerTest {

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
	public void testAddReturnsOnEmptyCfgAddsRetAsEntrypoint() {
		CFG cfg = mkCfg();
		CFGTweaker.addReturns(cfg, IllegalStateException::new);
		assertEquals(1, cfg.getNodesCount());
		Statement onlyNode = cfg.getNodes().iterator().next();
		assertTrue(onlyNode instanceof Ret);
		assertTrue(cfg.getEntrypoints().contains(onlyNode));
	}

	@Test
	public void testAddReturnsConnectsDanglingStatementsWithoutFollowers() {
		CFG cfg = mkCfg();
		NoOp noop = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		cfg.addNode(noop, true);

		CFGTweaker.addReturns(cfg, IllegalStateException::new);

		assertEquals(2, cfg.getNodesCount());
		assertTrue(cfg.getNodes().stream().anyMatch(Ret.class::isInstance));
		Statement ret = cfg.getNodes().stream().filter(Ret.class::isInstance).findFirst().get();
		assertTrue(cfg.getIngoingEdges(ret).stream().anyMatch(e -> e.getSource() == noop));
	}

	@Test
	public void testAddReturnsDoesNothingWhenAllPathsAlreadyExitExplicitly() {
		CFG cfg = mkCfg();
		Ret ret = new Ret(cfg, new SourceCodeLocation("fake", 2, 0));
		cfg.addNode(ret, true);

		CFGTweaker.addReturns(cfg, IllegalStateException::new);

		assertEquals(1, cfg.getNodesCount());
	}

	@Test
	public void testAddReturnsThrowsWhenNormalExitsDisagreeOnReturningAValue() {
		// regression test: the check that all normal exits either uniformly
		// return a value or uniformly don't must not depend on the iteration
		// order of getNormalExitpoints() - both flavors are collected in full
		// before comparing them, regardless of which one is added to the cfg
		// first; a dangling statement is also needed so that addReturns()
		// does not bail out early with nothing to do
		CFG cfg = mkCfg();
		NoOp dangling = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		cfg.addNode(dangling, true);

		Ret bareReturn = new Ret(cfg, new SourceCodeLocation("fake", 3, 0));
		cfg.addNode(bareReturn, true);

		Return valuedReturn = new Return(cfg, new SourceCodeLocation("fake", 4, 0), new VariableRef(
				cfg,
				new SourceCodeLocation("fake", 4, 0),
				"x"));
		cfg.addNode(valuedReturn, true);

		assertThrows(
				IllegalStateException.class,
				() -> CFGTweaker.addReturns(cfg, IllegalStateException::new));
	}

}
