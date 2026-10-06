package it.unive.lisa.imp.constructs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.imp.constructs.StringIndexOf.IMPStringIndexOf;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.string.IndexOf;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import org.junit.jupiter.api.Test;

public class StringIndexOfTest {

	// SUSPECTED BUG: the descriptor declares StringType.INSTANCE as the
	// return type, but IndexOf's own javadoc/constructor (which sets its
	// static type via cfg...getTypes().getIntegerType()) says the result is
	// a NumericType (Int32Type in this fixture's TestTypeSystem) - not a
	// string. Every other similarly-shaped construct (StringLength) declares
	// the correct Int32Type. Asserting the documented/actual type here, not
	// the currently-declared one.
	@Test
	public void descriptorMatchesTheDocumentedSignature() {
		StringIndexOf construct = new StringIndexOf(ConstructTestFixtures.LOCATION, ConstructTestFixtures.UNIT);
		var descriptor = construct.getDescriptor();

		assertEquals("indexOf", descriptor.getName());
		assertEquals(Int32Type.INSTANCE, descriptor.getReturnType());
		assertTrue(descriptor.isInstance(), "indexOf() must be an instance method invoked on a string");
		assertEquals(2, descriptor.getFormals().length);
		assertEquals("this", descriptor.getFormals()[0].getName());
		assertEquals(StringType.INSTANCE, descriptor.getFormals()[0].getStaticType());
		assertEquals("search", descriptor.getFormals()[1].getName());
		assertEquals(StringType.INSTANCE, descriptor.getFormals()[1].getStaticType());
	}

	@Test
	public void buildAssignsParametersToTheCorrectRoles() {
		VariableRef receiver = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "receiver");
		VariableRef arg = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "arg");

		IMPStringIndexOf built = IMPStringIndexOf.build(
				ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, receiver, arg);

		assertTrue(built instanceof IndexOf,
				"the construct must build an instance of the underlying IndexOf statement");
		assertEquals(receiver, built.getLeft());
		assertEquals(arg, built.getRight());
	}

}
