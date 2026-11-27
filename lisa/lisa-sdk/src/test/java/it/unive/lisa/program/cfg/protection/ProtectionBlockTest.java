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
import java.util.List;
import org.junit.jupiter.api.Test;

public class ProtectionBlockTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG cfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "u", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	private static ProtectedBlock emptyBlock() {
		return new ProtectedBlock(null, null, new HashSet<>());
	}

	// Statement#equals() is based on class + location only, and NoOp does not
	// add any further discriminating state: distinct NoOp nodes used together
	// in the same Set/body must therefore be built at distinct locations, or
	// they collapse into a single entry
	private static SourceCodeLocation loc(
			int col) {
		return new SourceCodeLocation("fake", 0, col);
	}

	@Test
	public void constructorRequiresAtLeastACatchBlockOrAFinallyBlock() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new ProtectionBlock(emptyBlock(), Collections.emptyList(), null, null, null));
	}

	@Test
	public void constructorAcceptsOnlyAFinallyBlockWithNoCatches() {
		ProtectionBlock block = new ProtectionBlock(emptyBlock(), Collections.emptyList(), null, emptyBlock(), null);
		assertTrue(block.getCatchBlocks().isEmpty());
	}

	@Test
	public void getFullBodyMergesTryCatchAndElseButOnlyIncludesFinallyWhenRequested() {
		CFG cfg = cfg();
		Statement tryStmt = new NoOp(cfg, loc(0));
		Statement catchStmt = new NoOp(cfg, loc(1));
		Statement elseStmt = new NoOp(cfg, loc(2));
		Statement finallyStmt = new NoOp(cfg, loc(3));

		ProtectedBlock tryBlock = new ProtectedBlock(tryStmt, tryStmt, new HashSet<>(List.of(tryStmt)));
		ProtectedBlock catchBody = new ProtectedBlock(catchStmt, catchStmt, new HashSet<>(List.of(catchStmt)));
		CatchBlock catchBlock = new CatchBlock(null, catchBody, Untyped.INSTANCE);
		ProtectedBlock elseBlock = new ProtectedBlock(elseStmt, elseStmt, new HashSet<>(List.of(elseStmt)));
		ProtectedBlock finallyBlock = new ProtectedBlock(finallyStmt, finallyStmt, new HashSet<>(List.of(finallyStmt)));

		ProtectionBlock block = new ProtectionBlock(
				tryBlock, List.of(catchBlock), elseBlock, finallyBlock, null);

		var withoutFinally = block.getFullBody(false);
		assertTrue(withoutFinally.containsAll(List.of(tryStmt, catchStmt, elseStmt)));
		assertFalse(withoutFinally.contains(finallyStmt));

		var withFinally = block.getFullBody(true);
		assertTrue(withFinally.containsAll(List.of(tryStmt, catchStmt, elseStmt, finallyStmt)));
	}

	@Test
	public void simplifyRemovesTargetsFromEveryComponent() {
		CFG cfg = cfg();
		Statement kept = new NoOp(cfg, loc(0));
		Statement removedFromTry = new NoOp(cfg, loc(1));
		Statement removedFromCatch = new NoOp(cfg, loc(2));

		ProtectedBlock tryBlock = new ProtectedBlock(kept, kept, new HashSet<>(List.of(kept, removedFromTry)));
		ProtectedBlock catchBody = new ProtectedBlock(kept, kept, new HashSet<>(List.of(removedFromCatch)));
		CatchBlock catchBlock = new CatchBlock(null, catchBody, Untyped.INSTANCE);

		ProtectionBlock block = new ProtectionBlock(tryBlock, List.of(catchBlock), null, null, null);

		block.simplify(new HashSet<>(List.of(removedFromTry, removedFromCatch)));

		assertTrue(tryBlock.getBody().contains(kept));
		assertFalse(tryBlock.getBody().contains(removedFromTry));
		assertFalse(catchBody.getBody().contains(removedFromCatch));
	}

	@Test
	public void equalsAndHashCodeAreBasedOnAllComponents() {
		ProtectedBlock tryBlock = emptyBlock();
		ProtectionBlock a = new ProtectionBlock(tryBlock, Collections.emptyList(), null, emptyBlock(), null);
		ProtectionBlock b = new ProtectionBlock(tryBlock, Collections.emptyList(), null, emptyBlock(), null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

}
