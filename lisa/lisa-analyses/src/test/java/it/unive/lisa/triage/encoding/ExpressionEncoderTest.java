package it.unive.lisa.triage.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Float32Type;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.Int8Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMod;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingRem;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.unary.BitwiseNegation;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.StringToUpperCase;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExpressionEncoder}. The semantics of the encodings is
 * checked with z3: tests are skipped if z3 is not available.
 */
public class ExpressionEncoderTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("test", 0, 0);

	private static final Variable X = new Variable(Int32Type.INSTANCE, "x", LOC);

	private static final Variable Y = new Variable(Int32Type.INSTANCE, "y", LOC);

	private static final Variable V = new Variable(Untyped.INSTANCE, "v", LOC);

	private static final Variable S = new Variable(StringType.INSTANCE, "s", LOC);

	private SmtSolver solver;

	private ExpressionEncoder encoder;

	private int fresh;

	/**
	 * A symbol table declaring constants on the fly.
	 */
	private class Symbols
			implements
			SymbolTable {

		private final Map<Identifier, Term> constants = new HashMap<>();

		@Override
		public Term constant(
				Identifier id,
				Sort sort) {
			return constants.computeIfAbsent(id, i -> solver.declare(i.getName(), sort));
		}

		@Override
		public Term fresh(
				Sort sort,
				SymbolicExpression origin) {
			return solver.declare("fresh" + (fresh++), sort);
		}

	}

	@BeforeEach
	public void setup() {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		solver = SmtLibProcessSolver.z3();
		solver.declareDatatype(ValueSort.DATATYPE);
		Map<Identifier, Set<Type>> types = new HashMap<>();
		types.put(X, Set.of(Int32Type.INSTANCE));
		types.put(Y, Set.of(Int32Type.INSTANCE));
		types.put(S, Set.of(StringType.INSTANCE));
		types.put(V, Set.of(Int32Type.INSTANCE, StringType.INSTANCE));
		encoder = new ExpressionEncoder(new SortPolicy(types), new Symbols());
	}

	@AfterEach
	public void teardown() {
		if (solver != null)
			solver.close();
	}

	private static Constant integer(
			int value) {
		return new Constant(Int32Type.INSTANCE, value, LOC);
	}

	private static Constant string(
			String value) {
		return new Constant(StringType.INSTANCE, value, LOC);
	}

	private static BinaryExpression bin(
			Type type,
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator op) {
		return new BinaryExpression(type, left, right, op, LOC);
	}

	private static BinaryExpression eq(
			SymbolicExpression left,
			SymbolicExpression right) {
		return bin(BoolType.INSTANCE, left, right, ComparisonEq.INSTANCE);
	}

	/**
	 * Checks that the given condition holds in every model.
	 */
	private void assertValid(
			SymbolicExpression condition)
			throws SemanticException {
		solver.push();
		solver.assertTerm(Term.not(encoder.encodeCondition(condition)));
		assertEquals(SmtResult.UNSAT, solver.check(), "Not valid: " + condition);
		solver.pop();
	}

	/**
	 * Checks that the given condition holds in some model.
	 */
	private void assertSatisfiable(
			SymbolicExpression condition)
			throws SemanticException {
		solver.push();
		solver.assertTerm(encoder.encodeCondition(condition));
		assertEquals(SmtResult.SAT, solver.check(), "Not satisfiable: " + condition);
		solver.pop();
	}

	@Test
	public void mathematicalIntegers() throws SemanticException {
		// x + 1 > x, with no overflow
		assertValid(bin(BoolType.INSTANCE, bin(Int32Type.INSTANCE, X, integer(1), NumericNonOverflowingAdd.INSTANCE),
				X, ComparisonGt.INSTANCE));
		// ~x == -x - 1
		assertValid(eq(new UnaryExpression(Int32Type.INSTANCE, integer(5), BitwiseNegation.INSTANCE, LOC),
				integer(-6)));
	}

	@Test
	public void overflowingIntegers() throws SemanticException {
		// 127 + 1 wraps to -128 on 8 bits
		assertValid(eq(bin(Int8Type.INSTANCE, integer(127), integer(1), Numeric8BitAdd.INSTANCE), integer(-128)));
		// x + 1 > x does not hold on 8 bits
		assertSatisfiable(bin(BoolType.INSTANCE, X, bin(Int8Type.INSTANCE, X, integer(1), Numeric8BitAdd.INSTANCE),
				ComparisonGt.INSTANCE));
	}

	@Test
	public void divisionAndRemainders() throws SemanticException {
		// truncating division and remainder (sign of the dividend)
		assertValid(eq(bin(Int32Type.INSTANCE, integer(-7), integer(2), NumericNonOverflowingDiv.INSTANCE),
				integer(-3)));
		assertValid(eq(bin(Int32Type.INSTANCE, integer(7), integer(-2), NumericNonOverflowingDiv.INSTANCE),
				integer(-3)));
		assertValid(eq(bin(Int32Type.INSTANCE, integer(-7), integer(2), NumericNonOverflowingRem.INSTANCE),
				integer(-1)));
		// modulo (sign of the divisor)
		assertValid(eq(bin(Int32Type.INSTANCE, integer(-7), integer(2), NumericNonOverflowingMod.INSTANCE),
				integer(1)));
		assertValid(eq(bin(Int32Type.INSTANCE, integer(7), integer(-2), NumericNonOverflowingMod.INSTANCE),
				integer(-1)));
	}

	@Test
	public void floatsAreUnconstrained() throws SemanticException {
		Constant half = new Constant(Float32Type.INSTANCE, 0.5f, LOC);
		BinaryExpression sum = bin(Float32Type.INSTANCE, half, half, NumericNonOverflowingAdd.INSTANCE);
		// float arithmetic is not encoded: the result might be anything
		assertSatisfiable(eq(sum, half));
		assertSatisfiable(new UnaryExpression(BoolType.INSTANCE, eq(sum, half), LogicalNegation.INSTANCE, LOC));
	}

	@Test
	public void strings() throws SemanticException {
		assertValid(eq(new TernaryExpression(StringType.INSTANCE, string("hello"), integer(1), integer(3),
				StringSubstring.INSTANCE, LOC), string("el")));
		assertValid(eq(new TernaryExpression(StringType.INSTANCE, string("abab"), string("b"), string("c"),
				StringReplace.INSTANCE, LOC), string("acab")));
		assertValid(eq(bin(Int32Type.INSTANCE, string("hello"), string("l"), StringIndexOf.INSTANCE), integer(2)));
		assertValid(bin(BoolType.INSTANCE, string("hello"), string("he"), StringStartsWith.INSTANCE));
		assertValid(bin(BoolType.INSTANCE, string("hello"), string("ell"), StringContains.INSTANCE));
		// strlen(s + "ab") == strlen(s) + 2
		assertValid(eq(
				new UnaryExpression(Int32Type.INSTANCE, bin(StringType.INSTANCE, S, string("ab"), StringConcat.INSTANCE),
						StringLength.INSTANCE, LOC),
				bin(Int32Type.INSTANCE, new UnaryExpression(Int32Type.INSTANCE, S, StringLength.INSTANCE, LOC),
						integer(2), NumericNonOverflowingAdd.INSTANCE)));
		// unsupported operations are unconstrained
		assertSatisfiable(eq(new UnaryExpression(StringType.INSTANCE, string("a"), StringToUpperCase.INSTANCE, LOC),
				string("whatever")));
	}

	@Test
	public void values() throws SemanticException {
		// v has more than one type: it is encoded as a value
		assertEquals(ValueSort.SORT, encoder.encode(V).getSort());
		BinaryExpression succ = bin(Untyped.INSTANCE, V, integer(1), NumericNonOverflowingAdd.INSTANCE);
		// if v is the integer 3, v + 1 is the integer 4
		assertValid(implies(eq(V, integer(3)), eq(succ, integer(4))));
		// if v is a string, v + 1 is unconstrained (it might even be a string)
		solver.push();
		solver.assertTerm(encoder.encodeCondition(eq(V, string("a"))));
		solver.assertTerm(encoder.encodeCondition(eq(succ, string("b"))));
		assertEquals(SmtResult.SAT, solver.check());
		solver.pop();
		// values of different kinds are different
		assertValid(new UnaryExpression(BoolType.INSTANCE, eq(integer(1), string("1")), LogicalNegation.INSTANCE,
				LOC));
		// null is equal to itself
		assertValid(eq(new NullConstant(LOC), new NullConstant(LOC)));
	}

	@Test
	public void unknownValuesAreFresh() throws SemanticException {
		PushAny any = new PushAny(Int32Type.INSTANCE, LOC);
		assertEquals(Sort.INT, encoder.encode(any).getSort());
		assertSatisfiable(eq(any, integer(42)));
		assertSatisfiable(eq(any, integer(-1)));
		// two unknown values are independent
		assertSatisfiable(new UnaryExpression(BoolType.INSTANCE, eq(any, any), LogicalNegation.INSTANCE, LOC));
	}

	private static SymbolicExpression implies(
			SymbolicExpression premise,
			SymbolicExpression conclusion) {
		// !premise || conclusion
		return bin(BoolType.INSTANCE,
				new UnaryExpression(BoolType.INSTANCE, premise, LogicalNegation.INSTANCE, LOC),
				conclusion,
				LogicalOr.INSTANCE);
	}
}
