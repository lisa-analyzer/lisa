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
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InterfaceTypeTest {

	// i: root interface; j extends i; k is unrelated; d implements j
	private static final String SOURCE = "interface i { a(x); }\n"
			+ "interface j extends i { b(x); }\n"
			+ "interface k { c(x); }\n"
			+ "class d implements j {\n"
			+ "	~d() { }\n"
			+ "	a(x) { return x; }\n"
			+ "	b(x) { return x; }\n"
			+ "}\n";

	private InterfaceType i;

	private InterfaceType j;

	private InterfaceType k;

	private ClassType d;

	@BeforeEach
	public void setup()
			throws ParsingException,
			ProgramValidationException {
		InterfaceType.clearAll();
		ClassType.clearAll();
		Program prog = IMPFrontend.processText(SOURCE);
		prog.getFeatures().getProgramValidationLogic().validateAndFinalize(prog);
		i = InterfaceType.lookup("i");
		j = InterfaceType.lookup("j");
		k = InterfaceType.lookup("k");
		d = ClassType.lookup("d");
	}

	@Test
	public void anInterfaceIsAssignableToItself() {
		assertTrue(i.canBeAssignedTo(i));
	}

	@Test
	public void anExtendingInterfaceIsAssignableToItsAncestorButNotViceVersa() {
		assertTrue(j.canBeAssignedTo(i));
		assertFalse(i.canBeAssignedTo(j));
	}

	@Test
	public void unrelatedInterfacesAreNotAssignableToEachOther() {
		assertFalse(i.canBeAssignedTo(k));
		assertFalse(k.canBeAssignedTo(i));
	}

	@Test
	public void anInterfaceIsNeverAssignableToAClassType() {
		// per its javadoc/implementation, InterfaceType.canBeAssignedTo only
		// ever returns true against another InterfaceType
		assertFalse(i.canBeAssignedTo(d));
	}

	@Test
	public void commonSupertypeOfAnInterfaceWithItselfIsItself() {
		assertEquals(i, i.commonSupertype(i));
	}

	@Test
	public void commonSupertypeOfAnExtendingInterfaceAndItsAncestorIsTheAncestor() {
		assertEquals(i, j.commonSupertype(i));
		assertEquals(i, i.commonSupertype(j));
	}

	@Test
	public void commonSupertypeOfUnrelatedInterfacesIsUntyped() {
		// i and k share no common ancestor interface at all
		assertEquals(Untyped.INSTANCE, i.commonSupertype(k));
	}

	@Test
	public void commonSupertypeWithNullIsTheInterfaceItself() {
		assertEquals(i, i.commonSupertype(NullType.INSTANCE));
	}

	@Test
	public void commonSupertypeWithANonUnitTypeIsUntyped() {
		assertEquals(Untyped.INSTANCE, i.commonSupertype(it.unive.lisa.program.type.Int32Type.INSTANCE));
	}

	@Test
	public void equalsAndHashCodeConsiderBothNameAndUnit() {
		InterfaceType again = InterfaceType.lookup("i");
		assertEquals(i, again);
		assertEquals(i.hashCode(), again.hashCode());
		assertNotEquals(i, j);
	}

	@Test
	public void toStringIsTheInterfaceName() {
		assertEquals("i", i.toString());
	}

	@Test
	public void allInstancesIncludesImplementingClassesAndExtendingInterfaces() {
		java.util.Set<it.unive.lisa.type.Type> instances = i.allInstances(null);
		assertTrue(instances.contains(i));
		assertTrue(instances.contains(j), "j extends i, so j is an instance of i");
		assertTrue(instances.contains(d), "d implements j (which extends i), so d is an instance of i");
		assertFalse(instances.contains(k), "k is unrelated to i");
	}

}
