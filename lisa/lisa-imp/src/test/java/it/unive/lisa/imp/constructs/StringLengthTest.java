package it.unive.lisa.imp.constructs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.imp.constructs.StringLength.IMPStringLength;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.string.Length;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import org.junit.jupiter.api.Test;

public class StringLengthTest {

	@Test
	public void descriptorMatchesTheDocumentedSignature() {
		StringLength construct = new StringLength(ConstructTestFixtures.LOCATION, ConstructTestFixtures.UNIT);
		var descriptor = construct.getDescriptor();

		assertEquals("len", descriptor.getName());
		assertEquals(Int32Type.INSTANCE, descriptor.getReturnType());
		assertTrue(descriptor.isInstance(), "len() must be an instance method invoked on a string");
		assertEquals(1, descriptor.getFormals().length);
		assertEquals("this", descriptor.getFormals()[0].getName());
		assertEquals(StringType.INSTANCE, descriptor.getFormals()[0].getStaticType());
	}

	@Test
	public void buildAssignsTheParameterAsTheOperand() {
		VariableRef receiver = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "receiver");

		IMPStringLength built = IMPStringLength.build(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION,
				receiver);

		assertTrue(built instanceof Length, "the construct must build an instance of the underlying Length statement");
		assertEquals(receiver, built.getSubExpression());
	}

}
