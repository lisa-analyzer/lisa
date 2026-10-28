package it.unive.lisa.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import org.junit.jupiter.api.Test;

public class CompilationUnitSearchTest {

	private static Program program() {
		return new Program(new TestLanguageFeatures(), new TestTypeSystem());
	}

	private static CFG member(
			ClassUnit unit,
			String name) {
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(new SourceCodeLocation("f", 1, 0), unit, true,
				name);
		return new CFG(descriptor);
	}

	@Test
	public void withoutTraversingTheHierarchyOnlyOwnMembersAreReturned() {
		Program p = program();
		ClassUnit sup = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "sup", false);
		ClassUnit sub = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "sub", false);
		sub.addSuperclass(sup);
		CFG supMethod = member(sup, "m");
		sup.addInstanceCodeMember(supMethod);

		assertTrue(sub.getInstanceCodeMembers(false).isEmpty());
		assertEquals(1, sup.getInstanceCodeMembers(false).size());
	}

	@Test
	public void traversingTheHierarchyPullsInInheritedMembers() {
		Program p = program();
		ClassUnit sup = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "sup", false);
		ClassUnit sub = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "sub", false);
		sub.addSuperclass(sup);
		CFG supMethod = member(sup, "m");
		sup.addInstanceCodeMember(supMethod);

		assertTrue(sub.getInstanceCodeMembers(true).contains(supMethod));
	}

	@Test
	public void anOverridingMemberHidesTheOverriddenOneFromTheSubunitPointOfView() {
		Program p = program();
		ClassUnit sup = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "sup", false);
		ClassUnit sub = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "sub", false);
		sub.addSuperclass(sup);

		CFG supMethod = member(sup, "m");
		sup.addInstanceCodeMember(supMethod);
		CFG subMethod = member(sub, "m");
		sub.addInstanceCodeMember(subMethod);
		// wires the override relationship that BaseValidationLogic would
		// normally establish during Program#validateAndFinalize
		supMethod.getDescriptor().overriddenBy().add(subMethod);

		assertEquals(1, sub.getInstanceCodeMembers(true).size());
		assertTrue(sub.getInstanceCodeMembers(true).contains(subMethod));
		assertFalse(sub.getInstanceCodeMembers(true).contains(supMethod));
	}

	@Test
	public void anOverrideThreeLevelsDeepStillHidesTheRootDefinition() {
		Program p = program();
		ClassUnit grandparent = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "gp", false);
		ClassUnit parent = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "parent", false);
		ClassUnit child = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "child", false);
		parent.addSuperclass(grandparent);
		child.addSuperclass(parent);

		CFG gpMethod = member(grandparent, "m");
		grandparent.addInstanceCodeMember(gpMethod);
		CFG parentMethod = member(parent, "m");
		parent.addInstanceCodeMember(parentMethod);
		gpMethod.getDescriptor().overriddenBy().add(parentMethod);

		assertEquals(1, child.getInstanceCodeMembers(true).size());
		assertTrue(child.getInstanceCodeMembers(true).contains(parentMethod));
	}

	@Test
	public void searchGlobalsFollowsTheSamePolicyByName() {
		Program p = program();
		ClassUnit sup = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "sup", false);
		ClassUnit sub = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "sub", false);
		sub.addSuperclass(sup);

		Global supGlobal = new Global(new SourceCodeLocation("f", 1, 0), sup, "x", true);
		sup.addInstanceGlobal(supGlobal);
		Global subGlobal = new Global(new SourceCodeLocation("f", 2, 0), sub, "x", true);
		sub.addInstanceGlobal(subGlobal);

		assertEquals(1, sub.getInstanceGlobals(true).size());
		assertTrue(sub.getInstanceGlobals(true).contains(subGlobal));
	}

	@Test
	public void addInstanceCodeMemberDiscardsDuplicateSignaturesAndSealingDisablesOverriding() {
		Program p = program();
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "sealedUnit", true);
		CFG first = member(unit, "m");
		CFG second = member(unit, "m");

		assertTrue(unit.addInstanceCodeMember(first));
		assertFalse(unit.addInstanceCodeMember(second));
		assertFalse(first.getDescriptor().isOverridable());
	}

	@Test
	public void getInstanceCodeMemberReturnsNullWhenNoneMatches() {
		Program p = program();
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "u", false);
		assertNull(unit.getInstanceCodeMember("nope()", true));
	}

	@Test
	public void getMatchingInstanceCodeMembersUsesTheDescriptorSignatureMatch() {
		Program p = program();
		ClassUnit unit = new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "u", false);
		CFG m = member(unit, "m");
		unit.addInstanceCodeMember(m);
		assertSame(m, unit.getMatchingInstanceCodeMembers(m.getDescriptor(), true).iterator().next());
	}

}
