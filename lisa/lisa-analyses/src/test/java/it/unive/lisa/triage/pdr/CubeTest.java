package it.unive.lisa.triage.pdr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.type.Untyped;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CubeTest {

	private static final SyntheticLocation LOC = SyntheticLocation.INSTANCE;

	private static final Variable X = new Variable(Untyped.INSTANCE, "x", LOC);

	private static final Variable Y = new Variable(Untyped.INSTANCE, "y", LOC);

	private static final SymbolicExpression EQ = new BinaryExpression(BoolType.INSTANCE, X, Y,
			ComparisonEq.INSTANCE, LOC);

	private static final Constant TRUE = new Constant(BoolType.INSTANCE, true, LOC);

	@Test
	public void conjunctionsAreSplit() {
		SymbolicExpression and = new BinaryExpression(BoolType.INSTANCE, EQ,
				new BinaryExpression(BoolType.INSTANCE, TRUE, X, LogicalAnd.INSTANCE, LOC), LogicalAnd.INSTANCE,
				LOC);
		Cube cube = new Cube(List.of(and));
		assertEquals(Set.of(EQ, X), cube.getLiterals());
		assertEquals(new Cube(List.of(X)), cube.without(EQ));
		assertTrue(new Cube(List.of(TRUE)).getLiterals().isEmpty());
	}

	@Test
	public void modelValues() {
		TestTypeSystem types = new TestTypeSystem();
		assertEquals(BigInteger.valueOf(-3), ((Constant) Projection.toConstant("(- 3)", types)).getValue());
		assertEquals(BigInteger.valueOf(42), ((Constant) Projection.toConstant("(Value.Int 42)", types)).getValue());
		assertEquals(true, ((Constant) Projection.toConstant("(Value.Bool true)", types)).getValue());
		assertEquals("a\"bè", ((Constant) Projection.toConstant("\"a\"\"b\\u{e8}\"", types)).getValue());
		assertTrue(Projection.toConstant("Value.Null", types) instanceof NullConstant);
		assertNull(Projection.toConstant("(Value.Ref 3)", types));
	}
}
