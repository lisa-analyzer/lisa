package it.unive.lisa.imp.constructs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.imp.constructs.StringReplace.IMPStringReplace;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.string.Replace;
import it.unive.lisa.program.type.StringType;
import org.junit.jupiter.api.Test;

public class StringReplaceTest {

	// SUSPECTED BUG: the descriptor declares BoolType.INSTANCE as the return
	// type, but this class's own javadoc says "the type of this expression
	// is the StringType" and the underlying Replace statement's constructor
	// sets its static type via getTypes().getStringType(). Looks like a
	// copy-paste from a boolean-returning sibling (e.g. Contains/Equals)
	// where only the parameter list was updated. Asserting the
	// documented/actual type here, not the currently-declared one.
	@Test
	public void descriptorMatchesTheDocumentedSignature() {
		StringReplace construct = new StringReplace(ConstructTestFixtures.LOCATION, ConstructTestFixtures.UNIT);
		var descriptor = construct.getDescriptor();

		assertEquals("replace", descriptor.getName());
		assertEquals(StringType.INSTANCE, descriptor.getReturnType());
		assertTrue(descriptor.isInstance(), "replace() must be an instance method invoked on a string");
		assertEquals(3, descriptor.getFormals().length);
		assertEquals("this", descriptor.getFormals()[0].getName());
		assertEquals(StringType.INSTANCE, descriptor.getFormals()[0].getStaticType());
		assertEquals("search", descriptor.getFormals()[1].getName());
		assertEquals(StringType.INSTANCE, descriptor.getFormals()[1].getStaticType());
		assertEquals("replacement", descriptor.getFormals()[2].getName());
		assertEquals(StringType.INSTANCE, descriptor.getFormals()[2].getStaticType());
	}

	@Test
	public void buildAssignsParametersToTheCorrectRoles() {
		VariableRef receiver = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "receiver");
		VariableRef search = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "search");
		VariableRef replacement = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION,
				"replacement");

		IMPStringReplace built = IMPStringReplace.build(
				ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, receiver, search, replacement);

		assertTrue(built instanceof Replace,
				"the construct must build an instance of the underlying Replace statement");
		assertEquals(receiver, built.getLeft());
		assertEquals(search, built.getMiddle());
		assertEquals(replacement, built.getRight());
	}

}
