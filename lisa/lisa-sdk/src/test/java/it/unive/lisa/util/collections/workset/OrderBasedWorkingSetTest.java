package it.unive.lisa.util.collections.workset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Statement;
import org.junit.jupiter.api.Test;

public class OrderBasedWorkingSetTest {

	private static final ClassUnit unit = new ClassUnit(
			new SourceCodeLocation("fake", 1, 0),
			new Program(new it.unive.lisa.TestLanguageFeatures(), new it.unive.lisa.TestTypeSystem()),
			"fake",
			false);

	private static final CFG cfg = new CFG(
			new CodeMemberDescriptor(new SourceCodeLocation("fake", 1, 0), unit, false, "foo"));

	private static Statement at(
			int line) {
		return new NoOp(cfg, new SourceCodeLocation("fake", line, 0));
	}

	@Test
	public void popReturnsStatementsInNaturalOrderRegardlessOfPushOrder() {
		OrderBasedWorkingSet ws = new OrderBasedWorkingSet();
		Statement first = at(1);
		Statement second = at(2);
		Statement third = at(3);

		ws.push(third);
		ws.push(first);
		ws.push(second);

		assertEquals(3, ws.size());
		assertSame(first, ws.peek());
		assertSame(first, ws.pop());
		assertSame(second, ws.pop());
		assertSame(third, ws.pop());
		assertTrue(ws.isEmpty());
	}

	@Test
	public void popRemovesTheReturnedElement() {
		OrderBasedWorkingSet ws = new OrderBasedWorkingSet();
		Statement s = at(1);
		ws.push(s);
		assertEquals(1, ws.size());
		ws.pop();
		assertEquals(0, ws.size());
	}

	@Test
	public void mkYieldsAFreshEmptyWorkingSet() {
		OrderBasedWorkingSet ws = new OrderBasedWorkingSet();
		ws.push(at(1));
		OrderBasedWorkingSet fresh = ws.mk();
		assertTrue(fresh.isEmpty());
		assertEquals(1, ws.size(), "mk() must not affect the original working set");
	}

}
