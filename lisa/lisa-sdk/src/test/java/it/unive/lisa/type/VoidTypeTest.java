package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class VoidTypeTest {

	@Test
	public void toStringIsVoid() {
		assertEquals("void", VoidType.INSTANCE.toString());
	}

	@Test
	public void equalsAcceptsAnyVoidTypeInstance() {
		assertTrue(VoidType.INSTANCE.equals(VoidType.INSTANCE));
		assertFalse(VoidType.INSTANCE.equals(null));
		assertFalse(VoidType.INSTANCE.equals(Untyped.INSTANCE));
		assertEquals(VoidType.class.hashCode(), VoidType.INSTANCE.hashCode());
	}

	@Test
	public void nothingCanBeAssignedToVoidNotEvenItself() {
		// void is not a value a variable can hold, so it accepts no
		// assignment, unlike every other Type in this package
		assertFalse(VoidType.INSTANCE.canBeAssignedTo(VoidType.INSTANCE));
		assertFalse(VoidType.INSTANCE.canBeAssignedTo(Untyped.INSTANCE));
	}

	@Test
	public void commonSupertypeWithAnotherVoidTypeIsVoid() {
		assertSame(VoidType.INSTANCE, VoidType.INSTANCE.commonSupertype(VoidType.INSTANCE));
	}

	@Test
	public void commonSupertypeWithAnythingElseIsUntyped() {
		assertSame(Untyped.INSTANCE, VoidType.INSTANCE.commonSupertype(NullType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(VoidType.INSTANCE), VoidType.INSTANCE.allInstances(new MinimalTypeSystem()));
	}

}
