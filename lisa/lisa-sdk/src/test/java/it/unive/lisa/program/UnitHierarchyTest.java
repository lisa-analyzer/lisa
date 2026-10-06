package it.unive.lisa.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import org.junit.jupiter.api.Test;

public class UnitHierarchyTest {

	private static Program program() {
		return new Program(new TestLanguageFeatures(), new TestTypeSystem());
	}

	private static ClassUnit classUnit(
			Program p,
			String name) {
		return new ClassUnit(new SourceCodeLocation("f", 1, 0), p, name, false);
	}

	private static InterfaceUnit interfaceUnit(
			Program p,
			String name) {
		return new InterfaceUnit(new SourceCodeLocation("f", 1, 0), p, name, false);
	}

	@Test
	public void codeUnitAndAbstractClassUnitCannotBeInstantiated() {
		Program p = program();
		assertFalse(new CodeUnit(new SourceCodeLocation("f", 1, 0), p, "code").canBeInstantiated());
		assertFalse(new AbstractClassUnit(new SourceCodeLocation("f", 1, 0), p, "abs", false).canBeInstantiated());
	}

	@Test
	public void classUnitCanBeInstantiatedButInterfacesCannot() {
		Program p = program();
		assertTrue(classUnit(p, "c").canBeInstantiated());
		assertFalse(interfaceUnit(p, "i").canBeInstantiated());
	}

	@Test
	public void immediateAncestorsOfAClassUnitAreTheUnionOfSuperclassesAndInterfaces() {
		Program p = program();
		ClassUnit sup = classUnit(p, "sup");
		InterfaceUnit itf = interfaceUnit(p, "itf");
		ClassUnit c = classUnit(p, "c");
		c.addSuperclass(sup);
		c.addInterface(itf);
		assertTrue(c.getImmediateAncestors().contains(sup));
		assertTrue(c.getImmediateAncestors().contains(itf));
	}

	@Test
	public void addAncestorDispatchesByTypeOnAClassUnit() {
		Program p = program();
		ClassUnit sup = classUnit(p, "sup");
		InterfaceUnit itf = interfaceUnit(p, "itf");
		ClassUnit c = classUnit(p, "c");
		c.addAncestor(sup);
		c.addAncestor(itf);
		assertTrue(c.getSuperclasses().contains(sup));
		assertTrue(c.getInterfaces().contains(itf));
	}

	@Test
	public void isInstanceOfHoldsForSelfAndTransitiveAncestors() {
		Program p = program();
		ClassUnit grandparent = classUnit(p, "gp");
		ClassUnit parent = classUnit(p, "parent");
		ClassUnit child = classUnit(p, "child");
		parent.addSuperclass(grandparent);
		child.addSuperclass(parent);

		assertTrue(child.isInstanceOf(child));
		assertTrue(child.isInstanceOf(parent));
		assertTrue(child.isInstanceOf(grandparent));
		assertFalse(grandparent.isInstanceOf(child));
	}

	@Test
	public void addInstancePropagatesToAllTransitiveAncestors() throws ProgramValidationException {
		Program p = program();
		ClassUnit grandparent = classUnit(p, "gp");
		ClassUnit parent = classUnit(p, "parent");
		ClassUnit child = classUnit(p, "child");
		parent.addSuperclass(grandparent);
		child.addSuperclass(parent);

		child.addInstance(child);

		assertTrue(parent.getInstances().contains(child));
		assertTrue(grandparent.getInstances().contains(child));
	}

	@Test
	public void addInstanceRejectsCyclesBetweenAncestorsAndInstances() {
		Program p = program();
		ClassUnit sup = classUnit(p, "sup");
		ClassUnit sub = classUnit(p, "sub");
		sub.addSuperclass(sup);

		// sup is already an ancestor of sub, so marking it as an instance of
		// sub too would make it both an ancestor and an instance of the same
		// unit
		assertThrows(ProgramValidationException.class, () -> sub.addInstance(sup));
	}

	@Test
	public void interfaceUnitHierarchyMirrorsClassUnitForSuperinterfaces() {
		Program p = program();
		InterfaceUnit grandparent = interfaceUnit(p, "gp");
		InterfaceUnit child = interfaceUnit(p, "child");
		child.addSuperinterface(grandparent);

		assertTrue(child.getImmediateAncestors().contains(grandparent));
		assertTrue(child.isInstanceOf(grandparent));
		assertEquals(1, child.getImmediateAncestors().size());
	}

}
