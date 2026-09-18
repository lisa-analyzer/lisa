package it.unive.lisa.imp.constructs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.imp.constructs.StringEndsWith.IMPStringEndsWith;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.string.EndsWith;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.StringType;
import org.junit.jupiter.api.Test;

public class StringEndsWithTest {

	@Test
	public void descriptorMatchesTheDocumentedSignature() {
		StringEndsWith construct = new StringEndsWith(ConstructTestFixtures.LOCATION, ConstructTestFixtures.UNIT);
		var descriptor = construct.getDescriptor();

		assertEquals("endsWith", descriptor.getName());
		assertEquals(BoolType.INSTANCE, descriptor.getReturnType());
		assertTrue(descriptor.isInstance(), "endsWith() must be an instance method invoked on a string");
		assertEquals(2, descriptor.getFormals().length);
		assertEquals("this", descriptor.getFormals()[0].getName());
		assertEquals(StringType.INSTANCE, descriptor.getFormals()[0].getStaticType());
		assertEquals("other", descriptor.getFormals()[1].getName());
		assertEquals(StringType.INSTANCE, descriptor.getFormals()[1].getStaticType());
	}

	@Test
	public void buildAssignsParametersToTheCorrectRoles() {
		VariableRef receiver = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "receiver");
		VariableRef arg = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "arg");

		IMPStringEndsWith built = IMPStringEndsWith.build(
				ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, receiver, arg);

		assertTrue(built instanceof EndsWith,
				"the construct must build an instance of the underlying EndsWith statement");
		assertEquals(receiver, built.getLeft());
		assertEquals(arg, built.getRight());
	}

}
