package it.unive.lisa.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ChecksExecutorTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 1, 0);

	// records what was visited, in order, so tests can assert on both
	// occurrence and (lack of) duplication
	private static class RecordingCheck
			implements
			Check<List<Object>> {

		boolean visitUnitReturns = true;

		@Override
		public void beforeExecution(
				List<Object> tool) {
			tool.add("before");
		}

		@Override
		public void afterExecution(
				List<Object> tool) {
			tool.add("after");
		}

		@Override
		public boolean visitUnit(
				List<Object> tool,
				Unit unit) {
			tool.add(unit);
			return visitUnitReturns;
		}

		@Override
		public void visitGlobal(
				List<Object> tool,
				Unit unit,
				Global global,
				boolean instance) {
			tool.add(List.of(global, instance));
		}

		@Override
		public boolean visit(
				List<Object> tool,
				CFG graph,
				Statement node) {
			tool.add(graph);
			return true;
		}

		@Override
		public boolean visit(
				List<Object> tool,
				CFG graph,
				Edge edge) {
			return true;
		}

	}

	@Test
	public void executeAllInvokesBeforeAndAfterExecutionExactlyOnce() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		Application app = new Application(program);
		RecordingCheck check = new RecordingCheck();
		List<Object> tool = new ArrayList<>();

		ChecksExecutor.executeAll(tool, app, List.of(check));

		assertEquals("before", tool.get(0));
		assertEquals("after", tool.get(tool.size() - 1));
		assertEquals(1, tool.stream().filter("before"::equals).count());
		assertEquals(1, tool.stream().filter("after"::equals).count());
	}

	@Test
	public void allGlobalsAndCfgsAreVisitedExactlyOnceIncludingInstanceMembers() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());

		Global progGlobal = new Global(LOC, program, "progGlobal", false);
		program.addGlobal(progGlobal);
		CFG progCfg = new CFG(new CodeMemberDescriptor(LOC, program, false, "progCfg"));
		progCfg.addNode(new NoOp(progCfg, LOC), true);
		program.addCodeMember(progCfg);

		ClassUnit unit = new ClassUnit(LOC, program, "unit", false);
		program.addUnit(unit);

		Global unitGlobal = new Global(LOC, unit, "unitGlobal", false);
		unit.addGlobal(unitGlobal);
		CFG unitCfg = new CFG(new CodeMemberDescriptor(LOC, unit, false, "unitCfg"));
		unitCfg.addNode(new NoOp(unitCfg, LOC), true);
		unit.addCodeMember(unitCfg);

		Global instGlobal = new Global(LOC, unit, "instGlobal", false);
		unit.addInstanceGlobal(instGlobal);
		CFG instCfg = new CFG(new CodeMemberDescriptor(LOC, unit, true, "instCfg"));
		instCfg.addNode(new NoOp(instCfg, LOC), true);
		unit.addInstanceCodeMember(instCfg);

		Application app = new Application(program);
		RecordingCheck check = new RecordingCheck();
		List<Object> tool = new ArrayList<>();

		ChecksExecutor.executeAll(tool, app, List.of(check));

		assertTrue(tool.contains(unit), "the unit itself was not visited");

		assertTrue(tool.contains(List.of(progGlobal, false)));
		assertTrue(tool.contains(List.of(unitGlobal, false)));
		assertTrue(tool.contains(List.of(instGlobal, true)));

		// each CFG's single NoOp node must be visited exactly once: no
		// double-visiting of instance members through both the base
		// getCodeMembers()/getGlobals() traversal and the dedicated
		// getInstanceCFGs()/getInstanceGlobals() traversal
		for (CFG cfg : Set.of(progCfg, unitCfg, instCfg))
			assertEquals(1, tool.stream().filter(cfg::equals).count(), "CFG " + cfg + " was not visited exactly once");
	}

	@Test
	public void visitUnitReturningFalseSkipsItsMembers() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		ClassUnit unit = new ClassUnit(LOC, program, "unit", false);
		program.addUnit(unit);

		Global unitGlobal = new Global(LOC, unit, "unitGlobal", false);
		unit.addGlobal(unitGlobal);
		CFG unitCfg = new CFG(new CodeMemberDescriptor(LOC, unit, false, "unitCfg"));
		unitCfg.addNode(new NoOp(unitCfg, LOC), true);
		unit.addCodeMember(unitCfg);

		Application app = new Application(program);
		RecordingCheck check = new RecordingCheck();
		check.visitUnitReturns = false;
		List<Object> tool = new ArrayList<>();

		ChecksExecutor.executeAll(tool, app, List.of(check));

		assertTrue(tool.contains(unit));
		assertFalse(tool.contains(List.of(unitGlobal, false)), "members of a rejected unit should not be visited");
		assertFalse(tool.contains(unitCfg), "CFGs of a rejected unit should not be visited");
	}

	@Test
	public void multipleChecksAreAllInvoked() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		Global progGlobal = new Global(LOC, program, "progGlobal", false);
		program.addGlobal(progGlobal);

		Application app = new Application(program);
		RecordingCheck first = new RecordingCheck();
		RecordingCheck second = new RecordingCheck();
		List<Object> tool = new ArrayList<>();

		ChecksExecutor.executeAll(tool, app, List.of(first, second));

		// both checks share the same tool instance, so "before"/"after" and
		// each global visit must show up twice, once per check
		assertEquals(2, tool.stream().filter("before"::equals).count());
		assertEquals(2, tool.stream().filter("after"::equals).count());
		assertEquals(2, tool.stream().filter(List.of(progGlobal, false)::equals).count());
	}

}
