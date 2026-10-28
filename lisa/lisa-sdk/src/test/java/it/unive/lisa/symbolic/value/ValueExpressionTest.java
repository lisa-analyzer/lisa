package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class ValueExpressionTest {

	@Test
	public void removeNegationsDefaultsToIdentity() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertSame(v, v.removeNegations());
	}

	@Test
	public void negateDefaultsToWrappingInLogicalNegation() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		ValueExpression negated = v.negate();
		assertTrue(negated instanceof UnaryExpression);
		UnaryExpression u = (UnaryExpression) negated;
		assertSame(LogicalNegation.INSTANCE, u.getOperator());
		assertSame(v, u.getExpression());
		assertSame(v.getStaticType(), u.getStaticType());
	}

}
