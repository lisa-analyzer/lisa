package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import org.junit.Test;

public class StringConstantPropagationTest {

	@Test
	public void testConstructor() {
		new StringConstantPropagation();
	}

	@Test
	public void testConstructor1() {
		new StringConstantPropagation("Hello World!");
	}

	@Test
	public void testEvalBinary() {
		StringConstantPropagation domain = new StringConstantPropagation();
		StringConstantPropagation s1 = new StringConstantPropagation("abc");
		StringConstantPropagation s2 = new StringConstantPropagation("def");

		domain = domain.evalBinaryExpression(StringConcat.INSTANCE, s1, s2, null, null);

		assertEquals(domain, new StringConstantPropagation("abcdef"));
	}

	@Test
	public void testEvalTernary() {
		StringConstantPropagation domain = new StringConstantPropagation();
		StringConstantPropagation s1 = new StringConstantPropagation("aaa");
		StringConstantPropagation s2 = new StringConstantPropagation("aa");
		StringConstantPropagation s3 = new StringConstantPropagation("b");

		domain = domain.evalTernaryExpression(StringReplace.INSTANCE, s1, s2, s3, null, null);

		assertEquals(domain, new StringConstantPropagation("ba"));
	}

}
