package it.unive.lisa.imp.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClassTypeTest {

	// a: root; b, c: siblings extending a; d: extends b (grandchild of a)
	private static final String SOURCE = "class a { ~a() { } }\n"
			+ "class b extends a { ~b() { } }\n"
			+ "class c extends a { ~c() { } }\n"
			+ "class d extends b { ~d() { } }\n";

	private ClassType a;

	private ClassType b;

	private ClassType c;

	private ClassType d;

	@BeforeEach
	public void setup()
			throws ParsingException,
			ProgramValidationException {
		ClassType.clearAll();
		Program prog = IMPFrontend.processText(SOURCE);
		prog.getFeatures().getProgramValidationLogic().validateAndFinalize(prog);
		a = ClassType.lookup("a");
		b = ClassType.lookup("b");
		c = ClassType.lookup("c");
		d = ClassType.lookup("d");
	}

	@Test
	public void aClassIsAssignableToItself() {
		assertTrue(a.canBeAssignedTo(a));
		assertTrue(b.canBeAssignedTo(b));
	}

	@Test
	public void aSubclassIsAssignableToItsAncestorsButNotViceVersa() {
		assertTrue(b.canBeAssignedTo(a));
		assertFalse(a.canBeAssignedTo(b));
		assertTrue(d.canBeAssignedTo(a), "d extends b extends a, so d must be assignable to a transitively");
		assertTrue(d.canBeAssignedTo(b));
		assertFalse(a.canBeAssignedTo(d));
	}

	@Test
	public void siblingClassesAreNotAssignableToEachOther() {
		assertFalse(b.canBeAssignedTo(c));
		assertFalse(c.canBeAssignedTo(b));
	}

	@Test
	public void commonSupertypeOfAClassWithItselfIsItself() {
		assertEquals(a, a.commonSupertype(a));
	}

	@Test
	public void commonSupertypeOfAncestorAndDescendantIsTheAncestor() {
		assertEquals(a, b.commonSupertype(a));
		assertEquals(a, a.commonSupertype(b));
		assertEquals(a, d.commonSupertype(a));
	}

	@Test
	public void commonSupertypeOfSiblingsIsTheirNearestCommonAncestor() {
		assertEquals(a, b.commonSupertype(c));
		assertEquals(a, c.commonSupertype(b));
	}

	@Test
	public void commonSupertypeOfAGrandchildAndAnUnrelatedSiblingIsTheirNearestCommonAncestor() {
		// d extends b extends a; c extends a directly: the nearest common
		// ancestor of d and c must be a, not b (b is not an ancestor of c)
		assertEquals(a, d.commonSupertype(c));
		assertEquals(a, c.commonSupertype(d));
	}

	@Test
	public void commonSupertypeWithNullIsTheClassItself() {
		assertEquals(a, a.commonSupertype(NullType.INSTANCE));
	}

	@Test
	public void commonSupertypeWithANonUnitTypeIsUntyped() {
		assertEquals(Untyped.INSTANCE, a.commonSupertype(it.unive.lisa.program.type.Int32Type.INSTANCE));
	}

	@Test
	public void equalsAndHashCodeConsiderBothNameAndUnit() {
		ClassType again = ClassType.lookup("a");
		assertEquals(a, again);
		assertEquals(a.hashCode(), again.hashCode());
		assertNotEquals(a, b);
	}

	@Test
	public void toStringIsTheClassName() {
		assertEquals("a", a.toString());
	}

	@Test
	public void allInstancesIncludesTheClassItselfAndAllItsSubclasses() {
		java.util.Set<Type> instances = a.allInstances(null);
		assertTrue(instances.contains(a));
		assertTrue(instances.contains(b));
		assertTrue(instances.contains(c));
		assertTrue(instances.contains(d));
	}

	@Test
	public void allInstancesOfALeafClassIsOnlyItself() {
		java.util.Set<Type> instances = d.allInstances(null);
		assertEquals(java.util.Set.of(d), instances);
	}

}
