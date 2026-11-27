package it.unive.lisa.imp.constructs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.imp.constructs.StringSubstring.IMPStringSubstring;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.string.Substring;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import org.junit.jupiter.api.Test;

public class StringSubstringTest {

	// SUSPECTED BUG: the descriptor declares BoolType.INSTANCE as the return
	// type, but this class's own javadoc says "the type of this expression
	// is the StringType" and the underlying Substring statement's
	// constructor sets its static type via getTypes().getStringType() -
	// same copy-paste shape as StringReplace. Asserting the
	// documented/actual type here, not the currently-declared one.
	@Test
	public void descriptorMatchesTheDocumentedSignature() {
		StringSubstring construct = new StringSubstring(ConstructTestFixtures.LOCATION, ConstructTestFixtures.UNIT);
		var descriptor = construct.getDescriptor();

		assertEquals("substring", descriptor.getName());
		assertEquals(StringType.INSTANCE, descriptor.getReturnType());
		assertTrue(descriptor.isInstance(), "substring() must be an instance method invoked on a string");
		assertEquals(3, descriptor.getFormals().length);
		assertEquals("this", descriptor.getFormals()[0].getName());
		assertEquals(StringType.INSTANCE, descriptor.getFormals()[0].getStaticType());
		assertEquals("start", descriptor.getFormals()[1].getName());
		assertEquals(Int32Type.INSTANCE, descriptor.getFormals()[1].getStaticType());
		assertEquals("end", descriptor.getFormals()[2].getName());
		assertEquals(Int32Type.INSTANCE, descriptor.getFormals()[2].getStaticType());
	}

	@Test
	public void buildAssignsParametersToTheCorrectRoles() {
		VariableRef receiver = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "receiver");
		VariableRef start = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "start");
		VariableRef end = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "end");

		IMPStringSubstring built = IMPStringSubstring.build(
				ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, receiver, start, end);

		assertTrue(built instanceof Substring,
				"the construct must build an instance of the underlying Substring statement");
		assertEquals(receiver, built.getLeft());
		assertEquals(start, built.getMiddle());
		assertEquals(end, built.getRight());
	}

}
