package it.unive.lisa.program.cfg.protection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

public class CatchBlockTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG cfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "u", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	@Test
	public void constructorRejectsNoExceptions() {
		ProtectedBlock body = new ProtectedBlock(null, null, Collections.emptyList());
		assertThrows(IllegalArgumentException.class, () -> new CatchBlock(null, body));
	}

	@Test
	public void simplifyDelegatesToTheBodysStatements() {
		CFG cfg = cfg();
		Statement kept = new NoOp(cfg, new SourceCodeLocation("fake", 0, 0));
		Statement removed = new NoOp(cfg, new SourceCodeLocation("fake", 0, 1));
		ProtectedBlock body = new ProtectedBlock(kept, kept, new HashSet<>(java.util.Arrays.asList(kept, removed)));
		CatchBlock cb = new CatchBlock(null, body, Untyped.INSTANCE);

		cb.simplify(new HashSet<>(Collections.singleton(removed)));

		assertTrue(body.getBody().contains(kept));
		assertFalse(body.getBody().contains(removed));
	}

	@Test
	public void equalsAndHashCodeAreBasedOnExceptionsIdentifierAndBody() {
		ProtectedBlock body1 = new ProtectedBlock(null, null, Collections.emptyList());
		ProtectedBlock body2 = new ProtectedBlock(null, null, Collections.emptyList());

		CatchBlock a = new CatchBlock(null, body1, Untyped.INSTANCE);
		CatchBlock b = new CatchBlock(null, body2, Untyped.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

}
