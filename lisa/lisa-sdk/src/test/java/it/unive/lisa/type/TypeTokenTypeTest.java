package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TypeTokenTypeTest {

	@Test
	public void getTypesReturnsTheConstructorArgument() {
		Set<Type> types = Collections.singleton(VoidType.INSTANCE);
		assertEquals(types, new TypeTokenType(types).getTypes());
	}

	@Test
	public void toStringIsTokenPrefixedSortedTypeNames() {
		Set<Type> types = new HashSet<>(Arrays.asList(VoidType.INSTANCE, Untyped.INSTANCE));
		// "untyped" < "void" alphabetically
		assertEquals("token::[untyped, void]", new TypeTokenType(types).toString());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnTheTypesSetByValue() {
		Set<Type> types1 = new HashSet<>(Arrays.asList(VoidType.INSTANCE, Untyped.INSTANCE));
		Set<Type> types2 = new HashSet<>(Arrays.asList(Untyped.INSTANCE, VoidType.INSTANCE));
		TypeTokenType a = new TypeTokenType(types1);
		TypeTokenType b = new TypeTokenType(types2);

		assertNotSame(a, b);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		TypeTokenType c = new TypeTokenType(Collections.singleton(VoidType.INSTANCE));
		assertFalse(a.equals(c));
	}

	@Test
	public void canBeAssignedToAnotherTypeTokenTypeOrUntyped() {
		TypeTokenType a = new TypeTokenType(Collections.singleton(VoidType.INSTANCE));
		TypeTokenType b = new TypeTokenType(Collections.singleton(NullType.INSTANCE));
		assertTrue(a.canBeAssignedTo(b));
		assertTrue(a.canBeAssignedTo(Untyped.INSTANCE));
		assertFalse(a.canBeAssignedTo(VoidType.INSTANCE));
	}

	@Test
	public void commonSupertypeRecognizesDistinctButEqualTokensAsTheSameType() {
		// regression test: commonSupertype used to compare tokens by
		// reference (==), so two independently-built TypeTokenTypes wrapping
		// the same set of types were incorrectly treated as unrelated,
		// despite being equal(); TypeTokenType is not a singleton, so this
		// case is not hypothetical
		Set<Type> types1 = new HashSet<>(Arrays.asList(VoidType.INSTANCE, Untyped.INSTANCE));
		Set<Type> types2 = new HashSet<>(Arrays.asList(Untyped.INSTANCE, VoidType.INSTANCE));
		TypeTokenType a = new TypeTokenType(types1);
		TypeTokenType b = new TypeTokenType(types2);

		assertNotSame(a, b);
		assertSame(a, a.commonSupertype(b));
	}

	@Test
	public void commonSupertypeWithADifferentTokenIsUntyped() {
		TypeTokenType a = new TypeTokenType(Collections.singleton(VoidType.INSTANCE));
		TypeTokenType b = new TypeTokenType(Collections.singleton(NullType.INSTANCE));
		assertSame(Untyped.INSTANCE, a.commonSupertype(b));
	}

	@Test
	public void allInstancesIsJustItself() {
		TypeTokenType t = new TypeTokenType(Collections.singleton(VoidType.INSTANCE));
		assertEquals(Collections.singleton(t), t.allInstances(new MinimalTypeSystem()));
	}

}
