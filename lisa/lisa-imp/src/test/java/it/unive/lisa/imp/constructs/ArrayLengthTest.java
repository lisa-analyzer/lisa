package it.unive.lisa.imp.constructs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.imp.constructs.ArrayLength.IMPArrayLength;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class ArrayLengthTest {

	@Test
	public void descriptorMatchesTheDocumentedSignature() {
		// unlike the string constructs, arraylen(a) is invoked as a plain
		// (static) function taking the array as its only argument, not as an
		// instance method on the array itself - it is declared on the
		// Program, not on a ClassUnit
		ArrayLength construct = new ArrayLength(ConstructTestFixtures.LOCATION, ConstructTestFixtures.PROGRAM);
		var descriptor = construct.getDescriptor();

		assertEquals("arraylen", descriptor.getName());
		assertEquals(Int32Type.INSTANCE, descriptor.getReturnType());
		assertFalse(descriptor.isInstance(), "arraylen() must be a static/free function, not an instance method");
		assertEquals(1, descriptor.getFormals().length);
		assertEquals("a", descriptor.getFormals()[0].getName());
		assertEquals(Untyped.INSTANCE, descriptor.getFormals()[0].getStaticType());
	}

	@Test
	public void buildAssignsTheParameterAsTheOperand() {
		VariableRef array = new VariableRef(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, "array");

		IMPArrayLength built = IMPArrayLength.build(ConstructTestFixtures.CFG, ConstructTestFixtures.LOCATION, array);

		assertEquals(array, built.getSubExpression());
	}

}
