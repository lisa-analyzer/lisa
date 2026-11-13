package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import org.junit.jupiter.api.Test;

public class ScopeTokenTest {

	private static final ClassUnit unit = new ClassUnit(
			new SourceCodeLocation("unknown", 0, 0),
			new Program(new TestLanguageFeatures(), new TestTypeSystem()),
			"Testing",
			false);

	private static Statement mkStatement(
			int line) {
		SourceCodeLocation loc = new SourceCodeLocation("unknown", line, 0);
		CFG cfg = new CFG(new CodeMemberDescriptor(loc, unit, false, "m" + line));
		return new Ret(cfg, loc);
	}

	@Test
	public void testGetScoper() {
		Statement st = mkStatement(1);
		ScopeToken token = new ScopeToken(st);
		assertSame(st, token.getScoper());
	}

	@Test
	public void testEqualsAndHashCodeWithStatementScoper() {
		// two distinct Statement instances at the same location are equal
		// according to Statement#equals, and ScopeToken must follow suit
		Statement st1 = mkStatement(1);
		Statement st2 = mkStatement(1);
		Statement other = mkStatement(2);

		ScopeToken t1 = new ScopeToken(st1);
		ScopeToken t2 = new ScopeToken(st2);
		ScopeToken tOther = new ScopeToken(other);

		assertEquals(t1, t1);
		assertEquals(t1, t2);
		assertEquals(t1.hashCode(), t2.hashCode());
		assertNotEquals(t1, tOther);
		assertNotEquals(t1, null);
		assertNotEquals(t1, "not a scope token");
	}

	@Test
	public void testEqualsWithNonStatementScoper() {
		// a CodeElement that is not a Statement falls back to plain equals(),
		// which for a lambda-backed functional interface is reference equality
		CodeElement e1 = () -> new SourceCodeLocation("unknown", 1, 0);
		CodeElement e2 = () -> new SourceCodeLocation("unknown", 1, 0);

		ScopeToken t1 = new ScopeToken(e1);
		ScopeToken t2 = new ScopeToken(e1);
		ScopeToken t3 = new ScopeToken(e2);

		assertEquals(t1, t2);
		assertNotEquals(t1, t3);
	}

	@Test
	public void testEqualsMixingStatementAndNonStatementScopers() {
		Statement st = mkStatement(1);
		CodeElement notAStatement = () -> st.getLocation();

		ScopeToken t1 = new ScopeToken(st);
		ScopeToken t2 = new ScopeToken(notAStatement);

		// even though the two scopers report the same location, they are of
		// different (and mutually non-comparable) kinds, so the tokens differ
		assertFalse(t1.equals(t2));
		assertFalse(t2.equals(t1));
	}

	@Test
	public void testToStringUsesScoperLocation() {
		Statement st = mkStatement(7);
		ScopeToken token = new ScopeToken(st);
		CodeLocation loc = st.getLocation();
		assertEquals("[" + loc + "]", token.toString());
	}

	@Test
	public void testEqualsWithNullScoper() {
		ScopeToken t1 = new ScopeToken(null);
		ScopeToken t2 = new ScopeToken(null);
		Statement st = mkStatement(1);
		ScopeToken t3 = new ScopeToken(st);

		assertTrue(t1.equals(t2));
		assertFalse(t1.equals(t3));
		assertFalse(t3.equals(t1));
	}

}
