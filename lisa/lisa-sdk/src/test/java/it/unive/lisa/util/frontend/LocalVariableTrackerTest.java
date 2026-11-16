package it.unive.lisa.util.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.util.frontend.LocalVariableTracker.LocalVariable;
import org.junit.jupiter.api.Test;

public class LocalVariableTrackerTest {

	private static CodeMemberDescriptor mkDescriptor(
			Parameter... formals) {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("fake", 1, 0), program, "fake", false);
		return new CodeMemberDescriptor(new SourceCodeLocation("fake", 1, 0), unit, false, "foo", formals);
	}

	@Test
	public void testConstructorRegistersFormalsInRootScope() {
		CodeMemberDescriptor descriptor = mkDescriptor(new Parameter(new SourceCodeLocation("fake", 1, 0), "p"));
		CFG cfg = new CFG(descriptor);
		LocalVariableTracker tracker = new LocalVariableTracker(cfg, descriptor);

		assertTrue(tracker.hasVariable("p"));
		assertFalse(tracker.hasVariable("q"));
	}

	@Test
	public void testAddVariableMakesItVisibleInCurrentScope() {
		CodeMemberDescriptor descriptor = mkDescriptor();
		CFG cfg = new CFG(descriptor);
		LocalVariableTracker tracker = new LocalVariableTracker(cfg, descriptor);
		NoOp def = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));

		tracker.addVariable("x", def, new Annotations());

		assertTrue(tracker.hasVariable("x"));
		LocalVariable v = tracker.getLocalVariable("x");
		assertNotNull(v);
		assertEquals(def, v.getScopeStart());
	}

	@Test
	public void testGetLocalVariableReturnsNullWhenNotFound() {
		CodeMemberDescriptor descriptor = mkDescriptor();
		CFG cfg = new CFG(descriptor);
		LocalVariableTracker tracker = new LocalVariableTracker(cfg, descriptor);
		assertNull(tracker.getLocalVariable("unknown"));
	}

	@Test
	public void testNestedScopeShadowsOuterVariable() {
		CodeMemberDescriptor descriptor = mkDescriptor();
		CFG cfg = new CFG(descriptor);
		LocalVariableTracker tracker = new LocalVariableTracker(cfg, descriptor);
		NoOp outerDef = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		NoOp innerDef = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));

		tracker.addVariable("x", outerDef, new Annotations());
		tracker.enterScope();
		tracker.addVariable("x", innerDef, new Annotations());

		assertEquals(innerDef, tracker.getLocalVariable("x").getScopeStart());

		tracker.exitScope(new NoOp(cfg, new SourceCodeLocation("fake", 4, 0)));

		assertEquals(outerDef, tracker.getLocalVariable("x").getScopeStart());
	}

	@Test
	public void testExitScopeAddsVariablesToDescriptorWithCorrectBounds() {
		CodeMemberDescriptor descriptor = mkDescriptor();
		CFG cfg = new CFG(descriptor);
		LocalVariableTracker tracker = new LocalVariableTracker(cfg, descriptor);
		NoOp def = new NoOp(cfg, new SourceCodeLocation("fake", 2, 0));
		NoOp closing = new NoOp(cfg, new SourceCodeLocation("fake", 3, 0));

		tracker.enterScope();
		tracker.addVariable("x", def, new Annotations());
		tracker.exitScope(closing);

		VariableTableEntry entry = descriptor.getVariables()
				.stream()
				.filter(e -> e.getName().equals("x"))
				.findFirst()
				.orElse(null);
		assertNotNull(entry, "the variable was not added to the descriptor upon scope exit");
		assertEquals(def, entry.getScopeStart());
		assertEquals(closing, entry.getScopeEnd());
	}

	@Test
	public void testVariableIsNoLongerVisibleAfterItsScopeIsExited() {
		CodeMemberDescriptor descriptor = mkDescriptor();
		CFG cfg = new CFG(descriptor);
		LocalVariableTracker tracker = new LocalVariableTracker(cfg, descriptor);

		tracker.enterScope();
		tracker.addVariable("x", new NoOp(cfg, new SourceCodeLocation("fake", 2, 0)), new Annotations());
		tracker.exitScope(new NoOp(cfg, new SourceCodeLocation("fake", 3, 0)));

		assertFalse(tracker.hasVariable("x"));
	}

	@Test
	public void testExitingTheRootScopeThrows() {
		CodeMemberDescriptor descriptor = mkDescriptor();
		CFG cfg = new CFG(descriptor);
		LocalVariableTracker tracker = new LocalVariableTracker(cfg, descriptor);

		assertThrows(
				IllegalStateException.class,
				() -> tracker.exitScope(new NoOp(cfg, new SourceCodeLocation("fake", 2, 0))));
	}

	@Test
	public void testExitingPastTheRootScopeAlwaysThrows() {
		CodeMemberDescriptor descriptor = mkDescriptor();
		CFG cfg = new CFG(descriptor);
		LocalVariableTracker tracker = new LocalVariableTracker(cfg, descriptor);

		tracker.enterScope();
		tracker.exitScope(new NoOp(cfg, new SourceCodeLocation("fake", 2, 0)));

		assertThrows(
				IllegalStateException.class,
				() -> tracker.exitScope(new NoOp(cfg, new SourceCodeLocation("fake", 3, 0))));
	}

	@Test
	public void testGetLatestScopeReturnsACopy() {
		CodeMemberDescriptor descriptor = mkDescriptor();
		CFG cfg = new CFG(descriptor);
		LocalVariableTracker tracker = new LocalVariableTracker(cfg, descriptor);
		tracker.addVariable("x", new NoOp(cfg, new SourceCodeLocation("fake", 2, 0)), new Annotations());

		tracker.getLatestScope().remove("x");

		assertTrue(tracker.hasVariable("x"), "mutating the returned map should not affect the tracker's state");
	}

}
