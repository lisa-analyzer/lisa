package it.unive.lisa.lattices.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class SupertypeTest {

	private static final TypeSystem types = new IMPTypeSystem();

	@Test
	public void degenerateInstanceHasNoRuntimeTypes() {
		Supertype degenerate = new Supertype();
		assertTrue(degenerate.getRuntimeTypes().isEmpty());
	}

	@Test
	public void getRuntimeTypesOfATrackedTypeIsItsInstances() {
		Supertype supertype = new Supertype(types, Int32Type.INSTANCE);
		assertEquals(Int32Type.INSTANCE.allInstances(types), supertype.getRuntimeTypes());
	}

	@Test
	public void bottomHasNoRuntimeTypes() {
		assertTrue(Supertype.BOTTOM.getRuntimeTypes().isEmpty());
	}

	@Test
	public void untypedIsTop() {
		Supertype top = new Supertype(types, Untyped.INSTANCE);
		assertTrue(top.isTop());
	}

	@Test
	public void aConcreteTypeIsNotTop() {
		Supertype in = new Supertype(types, Int32Type.INSTANCE);
		assertFalse(in.isTop());
	}

	@Test
	public void topOfAnInstanceIsUntyped() {
		Supertype in = new Supertype(types, Int32Type.INSTANCE);
		assertEquals(Untyped.INSTANCE, in.top().type);
	}

	@Test
	public void lubOfTheSameTypeIsThatType()
			throws SemanticException {
		Supertype in = new Supertype(types, Int32Type.INSTANCE);
		assertEquals(Int32Type.INSTANCE, in.lubAux(in).type);
	}

	@Test
	public void lubOfUnrelatedTypesIsUntyped()
			throws SemanticException {
		Supertype in = new Supertype(types, Int32Type.INSTANCE);
		Supertype str = new Supertype(types, StringType.INSTANCE);
		assertTrue(in.lubAux(str).isTop());
	}

	@Test
	public void aTypeCanBeAssignedToItself()
			throws SemanticException {
		Supertype in = new Supertype(types, Int32Type.INSTANCE);
		assertTrue(in.lessOrEqualAux(in));
	}

	@Test
	public void unrelatedTypesAreNotAssignable()
			throws SemanticException {
		Supertype in = new Supertype(types, Int32Type.INSTANCE);
		Supertype str = new Supertype(types, StringType.INSTANCE);
		assertFalse(in.lessOrEqualAux(str));
	}

	@Test
	public void glbOfTheSameTypeIsThatType()
			throws SemanticException {
		Supertype in = new Supertype(types, Int32Type.INSTANCE);
		assertEquals(Int32Type.INSTANCE, in.glbAux(in).type);
	}

	@Test
	public void glbOfUnrelatedTypesIsBottom()
			throws SemanticException {
		Supertype in = new Supertype(types, Int32Type.INSTANCE);
		Supertype str = new Supertype(types, StringType.INSTANCE);
		assertEquals(Supertype.BOTTOM, in.glbAux(str));
	}

	@Test
	public void equalityIsBasedOnTheTrackedType() {
		assertEquals(new Supertype(types, Int32Type.INSTANCE), new Supertype(types, Int32Type.INSTANCE));
		assertFalse(new Supertype(types, Int32Type.INSTANCE).equals(new Supertype(types, StringType.INSTANCE)));
	}

	@Test
	public void bottomHasNoType() {
		assertEquals(null, Supertype.BOTTOM.type);
	}

}
