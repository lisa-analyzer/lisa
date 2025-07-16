package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.analysis.string.StringConstantPropagation.SCP;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import org.junit.Test;

public class StringConstantPropagationTest {

	@Test
	public void testConstructor() {
		new SCP();
	}

	@Test
	public void testConstructor1() {
		new SCP("Hello World!");
	}

	@Test
	public void testEvalBinary() {
		StringConstantPropagation domain = new StringConstantPropagation();
		SCP s1 = new SCP("abc");
		SCP s2 = new SCP("def");

		SCP res = domain
				.evalBinaryExpression(
						new BinaryExpression(
								StringType.INSTANCE,
								new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE),
								new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE),
								StringConcat.INSTANCE,
								SyntheticLocation.INSTANCE),
						s1,
						s2,
						null,
						null);

		assertEquals(res, new SCP("abcdef"));
	}

	@Test
	public void testEvalTernary() {
		StringConstantPropagation domain = new StringConstantPropagation();
		SCP s1 = new SCP("aaa");
		SCP s2 = new SCP("aa");
		SCP s3 = new SCP("b");

		SCP res = domain
				.evalTernaryExpression(
						new TernaryExpression(
								StringType.INSTANCE,
								new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE),
								new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE),
								new Variable(StringType.INSTANCE, "z", SyntheticLocation.INSTANCE),
								StringReplace.INSTANCE,
								SyntheticLocation.INSTANCE),
						s1,
						s2,
						s3,
						null,
						null);

		assertEquals(res, new SCP("ba"));
	}

}
