package it.unive.lisa.analysis.numeric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.octagon.Floyd;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive test suite for DifferenceBoundMatrix (DBM) implementation.
 * Tests cover lattice operations, variable assignments, constraints, and
 * conversions.
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso </a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *             Shytermeja</a>
 */
public class DifferenceBoundMatrixTest {

	private DifferenceBoundMatrix dbm;
	private SemanticOracle oracle;
	private ProgramPoint pp;
	private CodeLocation location;
	private static final Type NumericType = Int32Type.INSTANCE;

	@Before
	public void setUp() {
		dbm = new DifferenceBoundMatrix();

		oracle = new SemanticOracle() {
			@Override
			public ExpressionSet rewrite(
					SymbolicExpression expression,
					ProgramPoint pp,
					SemanticOracle oracle)
					throws SemanticException {
				return new ExpressionSet(expression);
			}

			@Override
			public Satisfiability alias(
					SymbolicExpression x,
					SymbolicExpression y,
					ProgramPoint pp,
					SemanticOracle oracle)
					throws SemanticException {
				return Satisfiability.UNKNOWN;
			}

			@Override
			public Satisfiability isReachableFrom(
					SymbolicExpression x,
					SymbolicExpression y,
					ProgramPoint pp,
					SemanticOracle oracle)
					throws SemanticException {
				return Satisfiability.UNKNOWN;
			}

			@Override
			public Set<Type> getRuntimeTypesOf(
					SymbolicExpression e,
					ProgramPoint pp,
					SemanticOracle oracle)
					throws SemanticException {
				return java.util.Collections.singleton(e.getStaticType());
			}

			@Override
			public Type getDynamicTypeOf(
					SymbolicExpression e,
					ProgramPoint pp,
					SemanticOracle oracle)
					throws SemanticException {
				return e.getStaticType();
			}
		};

		location = new CodeLocation() {
			@Override
			public String getCodeLocation() {
				return "test-location";
			}

			@Override
			public int compareTo(
					CodeLocation o) {
				return this.getCodeLocation().compareTo(o.getCodeLocation());
			}

			@Override
			public String toString() {
				return getCodeLocation();
			}
		};

		pp = new ProgramPoint() {
			@Override
			public CFG getCFG() {
				return null;
			}

			@Override
			public CodeLocation getLocation() {
				return location;
			}

			@Override
			public String toString() {
				return "test-program-point";
			}
		};
	}

	public DifferenceBoundMatrix dbmFromMatrix(
			double[][] vals,
			Identifier... vars) {
		int n = vals.length;
		MathNumber[][] matrix = new MathNumber[n][n];
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++) {
				double v = vals[i][j];
				// Handle different representations of infinity that may appear
				if (Double.isInfinite(v) || v == Double.MAX_VALUE)
					matrix[i][j] = MathNumber.PLUS_INFINITY;
				else if (v == Double.NEGATIVE_INFINITY || v == -Double.MAX_VALUE)
					matrix[i][j] = MathNumber.MINUS_INFINITY;
				else if (v == 0)
					matrix[i][j] = MathNumber.ZERO;
				else
					matrix[i][j] = new MathNumber(v);
			}
		java.util.Map<Identifier, Integer> index = new java.util.HashMap<>();
		for (int i = 0; i < vars.length; i++)
			index.put(vars[i], i);
		DifferenceBoundMatrix res = new DifferenceBoundMatrix(matrix, index);
		System.out.println("DifferenceBoundMatrix from long matrix:");
		System.out.println(res.representation());
		return res;
	}

	public DifferenceBoundMatrix dbmFromMatrixMathNumbers(
			MathNumber[][] matrix,
			Identifier... vars) {

		java.util.Map<Identifier, Integer> index = new java.util.HashMap<>();
		for (int i = 0; i < vars.length; i++)
			index.put(vars[i], i);
		DifferenceBoundMatrix res = new DifferenceBoundMatrix(matrix, index);
		System.out.println("DifferenceBoundMatrix from long matrix:");
		System.out.println(res.representation());
		return res;
	}

	// inf
	static final double INF = Double.MAX_VALUE;

	// public class BasicLatticeOperations {

	@Test
	public void testEmptyDbmIsBottom() {
		assertTrue("Empty DBM should be bottom", dbm.isBottom());
		assertFalse("Empty DBM should not be top", dbm.isTop());
	}

	@Test
	public void testTopDbm() {
		DifferenceBoundMatrix topDbm = dbm.top();
		assertNotNull("Top DBM should not be null", topDbm);

		// Add a variable first to make top meaningful
		Variable x = new Variable(NumericType, "x", location);
		try {
			Constant c = new Constant(NumericType, new MathNumber(6), location);
			DifferenceBoundMatrix withVar = dbm.assign(x, c, pp, oracle);
			System.out.println(withVar.representation());
			assertTrue("DBM representation should match expected before top",
					withVar.equals(dbmFromMatrix(new double[][] {
							{ 0, -12 },
							{ 12, 0 }
					}, x)));
			DifferenceBoundMatrix topWithVar = withVar.top();
			assertTrue("DBM with variables should recognize top", topWithVar.isTop());
			assertFalse("Top DBM should not be bottom", topWithVar.isBottom());
		} catch (SemanticException e) {
			fail("Should not throw exception: " + e.getMessage());
		}
	}

	@Test
	public void testBottomDbm() {
		DifferenceBoundMatrix bottomDbm = dbm.bottom();
		assertNotNull("Bottom DBM should not be null", bottomDbm);
		assertTrue("Bottom DBM should be bottom", bottomDbm.isBottom());
		assertFalse("Bottom DBM should not be top", bottomDbm.isTop());
	}

	@Test
	public void testLessOrEqualWithBottom() throws SemanticException {
		System.out.println("Testing LessOrEqual with Bottom elements");
		DifferenceBoundMatrix bottom1 = dbm.bottom();
		DifferenceBoundMatrix bottom2 = dbm.bottom();

		assertTrue("Bottom <= Bottom should be true", bottom1.lessOrEqual(bottom2));

		// Create non-bottom DBM
		Variable x = new Variable(NumericType, "x", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);
		DifferenceBoundMatrix nonBottom = dbm.assign(x, c, pp, oracle);
		System.out.println(nonBottom.representation());
		assertTrue("DBM representation should match expected", nonBottom.equals(dbmFromMatrix(new double[][] {
				{ 0, -10 },
				{ 10, 0 }
		}, x)));
		assertFalse("Non-bottom <= Bottom should be false", nonBottom.lessOrEqual(bottom1));
	}

	@Test
	public void testLessOrEqualWithTop() throws SemanticException {
		// Create a DBM with variables to make top/non-top meaningful
		Variable x = new Variable(NumericType, "x", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);
		DifferenceBoundMatrix withVar = dbm.assign(x, c, pp, oracle);
		DifferenceBoundMatrix top = withVar.top();

		assertTrue("Any DBM <= Top should be true", withVar.lessOrEqual(top));
		assertFalse("Top <= non-top should be false", top.lessOrEqual(withVar));
	}

	@Test
	public void testLessOrEqualWithNonBottoms() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);

		DifferenceBoundMatrix dbm1 = dbm.assign(x, c1, pp, oracle);
		DifferenceBoundMatrix dbm2 = dbm.assign(x, c2, pp, oracle);

		// dbm1 has x = 5, dbm2 has x = 10
		assertTrue("DBM with x=5 <= DBM with x=10 should be false", dbm1.lessOrEqual(dbm1.lub(dbm2)));
	}
	// }

	// public class VariableManagement {

	@Test
	public void testKnowsIdentifierEmpty() {
		Variable x = new Variable(NumericType, "x", location);
		assertFalse("Empty DBM should not know any identifier", dbm.knowsIdentifier(x));
	}

	@Test
	public void testKnowsIdentifierAfterAssignment() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);

		DifferenceBoundMatrix result = dbm.assign(x, c, pp, oracle);

		assertTrue("DBM should know identifier after assignment", result.knowsIdentifier(x));
		assertFalse("DBM should not know unassigned identifier",
				result.knowsIdentifier(new Variable(NumericType, "y", location)));
	}

	@Test
	public void testMultipleVariableAssignments() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);

		DifferenceBoundMatrix result = dbm.assign(x, c1, pp, oracle)
				.assign(y, c2, pp, oracle);
		System.out.println(result.representation());

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-10), MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY },
				{ new MathNumber(10), MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO },
				{ MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, MathNumber.ZERO, new MathNumber(-20) },
				{ MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, new MathNumber(20), MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected",
				result.equals(dbmFromMatrixMathNumbers(resultStrongClosure, x, y)));

		assertTrue("DBM should know first variable", result.knowsIdentifier(x));
		assertTrue("DBM should know second variable", result.knowsIdentifier(y));
	}
	// }

	// public class AssignmentOperations {

	@Test
	public void testSimpleConstantAssignment() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);

		DifferenceBoundMatrix result = dbm.assign(x, c, pp, oracle);
		System.out.println(result.representation());

		assertNotNull("Assignment result should not be null", result);
		assertTrue("Result should know assigned variable", result.knowsIdentifier(x));
		assertFalse("Result should not be bottom", result.isBottom());
		assertTrue("DBM representation should match expected", result.equals(dbmFromMatrix(new double[][] {
				{ 0, -10 },
				{ 10, 0 }
		}, x)));
	}

	@Test
	public void testSelfDecrementAssignment() throws SemanticException { // ERROR
		Variable x = new Variable(NumericType, "x", location);

		// x = 4
		Constant c4 = new Constant(NumericType, new MathNumber(4), location);
		DifferenceBoundMatrix s1 = dbm.assign(x, c4, pp, oracle);
		System.out.println(s1.representation());

		// x = x - 2
		Constant c2 = new Constant(NumericType, new MathNumber(2), location);
		BinaryExpression expr = new BinaryExpression(NumericType, x, c2,
				NumericNonOverflowingSub.INSTANCE,
				location);
		DifferenceBoundMatrix res = s1.assign(x, expr, pp, oracle);
		System.out.println(res.representation());

		assertTrue(res.knowsIdentifier(x));
		assertFalse(res.isBottom());
		assertTrue("DBM representation should match expected after decrement", res.equals(dbmFromMatrix(new double[][] {
				{ 0, -4 },
				{ 4, 0 }
		}, x)));
	}

	@Test
	public void testCopyThenIncrementChain() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// y = 3
		DifferenceBoundMatrix s1 = dbm.assign(y, new Constant(NumericType, new MathNumber(3), location),
				pp,
				oracle);

		// x = y
		DifferenceBoundMatrix s2 = s1.assign(x, y, pp, oracle);
		System.out.println(s2.representation()); // CORRECT

		// y = y + 1
		DifferenceBoundMatrix s3 = s2.assign(y,
				new BinaryExpression(NumericType, y,
						new Constant(NumericType, new MathNumber(1), location),
						NumericNonOverflowingAdd.INSTANCE, location),
				pp, oracle);
		System.out.println(s3.representation()); // CORRECT

		// x = x + 1
		DifferenceBoundMatrix s4 = s3.assign(x,
				new BinaryExpression(NumericType, x,
						new Constant(NumericType, new MathNumber(1), location),
						NumericNonOverflowingAdd.INSTANCE, location),
				pp, oracle);
		System.out.println(s4.representation()); // ERROR

		assertTrue(s4.knowsIdentifier(x));
		assertTrue(s4.knowsIdentifier(y));
		assertFalse(s4.isBottom());

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-8), MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ new MathNumber(8), MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO },
				{ MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected after copy/increment chain",
				s4.equals(dbmFromMatrixMathNumbers(resultStrongClosure, y, x)));
	}

	@Test
	public void testZeroOffsetEquivalence() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// y = 7
		DifferenceBoundMatrix base = dbm.assign(y,
				new Constant(NumericType, new MathNumber(7), location), pp,
				oracle);

		// x = y + 0
		DifferenceBoundMatrix res1 = base.assign(x,
				new BinaryExpression(NumericType, y,
						new Constant(NumericType, new MathNumber(0), location),
						NumericNonOverflowingAdd.INSTANCE, location),
				pp, oracle);

		// x = y - 0
		DifferenceBoundMatrix res2 = base.assign(x,
				new BinaryExpression(NumericType, y,
						new Constant(NumericType, new MathNumber(0), location),
						NumericNonOverflowingSub.INSTANCE, location),
				pp, oracle);

		System.out.println(res2.representation());

		assertTrue(res1.knowsIdentifier(x) && res2.knowsIdentifier(x));
		assertTrue(res1.knowsIdentifier(y) && res2.knowsIdentifier(y));
		assertTrue(res1.lessOrEqual(res2) && res2.lessOrEqual(res1));

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-14), MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ new MathNumber(14), MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO },
				{ MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected for zero-offset equivalence",
				res1.equals(dbmFromMatrixMathNumbers(resultStrongClosure, y, x)));
	}

	@Test
	public void testReassignmentFromOtherVariable() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// x=5
		DifferenceBoundMatrix s1 = dbm.assign(x, new Constant(NumericType, new MathNumber(5), location),
				pp,
				oracle);
		// y = x + 2
		DifferenceBoundMatrix s2 = s1.assign(y,
				new BinaryExpression(NumericType, x,
						new Constant(NumericType, new MathNumber(2), location),
						NumericNonOverflowingAdd.INSTANCE, location),
				pp, oracle);
		System.out.println(s2.representation());
		// x = y - 2
		DifferenceBoundMatrix s3 = s2.assign(x,
				new BinaryExpression(NumericType, y,
						new Constant(NumericType, new MathNumber(2), location),
						NumericNonOverflowingSub.INSTANCE, location),
				pp, oracle);
		System.out.println(s3.representation());

		assertTrue(s3.knowsIdentifier(x));
		assertTrue(s3.knowsIdentifier(y));
		assertFalse(s3.isBottom());

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, MathNumber.PLUS_INFINITY, new MathNumber(2), MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY, new MathNumber(-2) },
				{ new MathNumber(-2), MathNumber.PLUS_INFINITY, MathNumber.ZERO, new MathNumber(-14) },
				{ MathNumber.PLUS_INFINITY, new MathNumber(2), new MathNumber(14), MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected after reassignment from other var",
				s3.equals(dbmFromMatrixMathNumbers(resultStrongClosure, x, y)));
	}

	@Test
	public void testArithmeticAssignment() throws SemanticException { // CORRECT
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// First assign y = 5
		Constant c = new Constant(NumericType, new MathNumber(5), location);
		DifferenceBoundMatrix step1 = dbm.assign(y, c, pp, oracle);

		// Then assign x = y + 3
		Constant offset = new Constant(NumericType, new MathNumber(3), location);
		BinaryExpression expr = new BinaryExpression(NumericType, y, offset,
				NumericNonOverflowingAdd.INSTANCE,
				location);

		DifferenceBoundMatrix result = step1.assign(x, expr, pp, oracle);
		System.out.println(result.representation());

		assertTrue("Result should know x", result.knowsIdentifier(x));
		assertTrue("Result should know y", result.knowsIdentifier(y));

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-10), new MathNumber(3), MathNumber.PLUS_INFINITY },
				{ new MathNumber(10), MathNumber.ZERO, MathNumber.PLUS_INFINITY, new MathNumber(-3) },
				{ new MathNumber(-3), MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, new MathNumber(3), MathNumber.PLUS_INFINITY, MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected for arithmetic assignment",
				result.equals(dbmFromMatrixMathNumbers(resultStrongClosure, y, x)));
	}

	@Test
	public void testVariableReferenceAssignment() throws SemanticException { // CORRECT
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// First assign x = 5
		Constant c = new Constant(NumericType, new MathNumber(5), location);
		DifferenceBoundMatrix step1 = dbm.assign(x, c, pp, oracle);

		// Then assign y = x
		DifferenceBoundMatrix result = step1.assign(y, x, pp, oracle);
		System.out.println(result.representation()); // TODO : not sure about
														// the result

		assertTrue("Result should know x", result.knowsIdentifier(x));
		assertTrue("Result should know y", result.knowsIdentifier(y));

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-10), MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ new MathNumber(10), MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO },
				{ MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected for variable reference assignment",
				result.equals(dbmFromMatrixMathNumbers(resultStrongClosure, x, y)));

		// Other way to construct the same assignment with x = 5, y = 5
		System.out.println("Before second assignment method");
		DifferenceBoundMatrix step2 = dbm.assign(x, c, pp, oracle);
		DifferenceBoundMatrix result2 = step2.assign(y, c, pp, oracle);
		System.out.println(result2.representation());

		assertTrue("Result2 should know x", result2.knowsIdentifier(x));
		assertTrue("Result2 should know y", result2.knowsIdentifier(y));

		MathNumber[][] resultStrongClosure2 = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-10), MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY },
				{ new MathNumber(10), MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, MathNumber.ZERO, new MathNumber(-10) },
				{ MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, new MathNumber(10), MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure2);

		assertTrue("DBM representation should match expected for variable reference assignment",
				result2.equals(dbmFromMatrixMathNumbers(resultStrongClosure2, x, y)));

	}

	@Test
	public void testReassignment() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);

		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);
		DifferenceBoundMatrix result = step1.assign(x, c2, pp, oracle);

		System.out.println(result.representation());

		assertTrue("Result should still know x after reassignment", result.knowsIdentifier(x));
		assertTrue("DBM representation should match expected after reassignment",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -20 },
						{ 20, 0 }
				}, x)));
	}

	@Test
	public void testSubtractionAssignment() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// y = 10
		Constant c10 = new Constant(NumericType, new MathNumber(10), location);
		DifferenceBoundMatrix step1 = dbm.assign(y, c10, pp, oracle);

		// x = y - 2
		Constant c2 = new Constant(NumericType, new MathNumber(2), location);
		BinaryExpression expr = new BinaryExpression(NumericType, y, c2,
				NumericNonOverflowingSub.INSTANCE,
				location);

		DifferenceBoundMatrix result = step1.assign(x, expr, pp, oracle);

		assertTrue("Result should know x", result.knowsIdentifier(x));
		assertTrue("Result should know y", result.knowsIdentifier(y));
		assertFalse("Result should not be bottom", result.isBottom());
		// expected y=10, x=8

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-20), new MathNumber(-2), MathNumber.PLUS_INFINITY },
				{ new MathNumber(20), MathNumber.ZERO, MathNumber.PLUS_INFINITY, new MathNumber(2) },
				{ new MathNumber(2), MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, new MathNumber(-2), MathNumber.PLUS_INFINITY, MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected for subtraction assignment",
				result.equals(dbmFromMatrixMathNumbers(resultStrongClosure, y, x)));
	}

	@Test
	public void testSelfIncrementAssignment() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);

		// x = 1
		Constant c1 = new Constant(NumericType, new MathNumber(1), location);
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// x = x + 2
		Constant c2 = new Constant(NumericType, new MathNumber(2), location);
		BinaryExpression expr = new BinaryExpression(NumericType, x, c2,
				NumericNonOverflowingAdd.INSTANCE,
				location);

		DifferenceBoundMatrix result = step1.assign(x, expr, pp, oracle);

		assertTrue("Result should know x", result.knowsIdentifier(x));
		assertFalse("Result should not be bottom", result.isBottom());
		assertTrue("DBM representation should match expected after self-increment",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -6 },
						{ 6, 0 }
				}, x)));
	}

	@Test
	public void testSelfNegationAssignment() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);

		// x = 5
		Constant c5 = new Constant(NumericType, new MathNumber(5), location);
		DifferenceBoundMatrix s1 = dbm.assign(x, c5, pp, oracle);
		System.out.println(s1.representation());

		// x = -x
		UnaryExpression negX = new UnaryExpression(NumericType, x, NumericNegation.INSTANCE, location);
		DifferenceBoundMatrix res = s1.assign(x, negX, pp, oracle);
		System.out.println("After negation:");
		System.out.println(res.representation());

		assertTrue(res.knowsIdentifier(x));
		assertFalse(res.isBottom());
		// expected x = -5
		assertTrue("DBM representation should match expected after negation", res.equals(dbmFromMatrix(new double[][] {
				{ 0, 10 },
				{ -10, 0 }
		}, x)));
	}

	@Test
	public void testSelfNegationWithOtherVariableAssignment() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// y = 5
		Constant c5 = new Constant(NumericType, new MathNumber(5), location);
		DifferenceBoundMatrix s1 = dbm.assign(y, c5, pp, oracle);
		System.out.println(s1.representation());

		// x = 8
		Constant c8 = new Constant(NumericType, new MathNumber(8), location);
		DifferenceBoundMatrix s2 = s1.assign(x, c8, pp, oracle);
		System.out.println(s2.representation());

		// x = -x
		UnaryExpression negX = new UnaryExpression(NumericType, x, NumericNegation.INSTANCE, location);
		DifferenceBoundMatrix res = s2.assign(x, negX, pp, oracle);
		System.out.println(res.representation());
		assertTrue(res.knowsIdentifier(x));
		assertTrue(res.knowsIdentifier(y));
		assertFalse(res.isBottom());
		// expected y=5, x=-8

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-10), MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY },
				{ new MathNumber(10), MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, MathNumber.ZERO, new MathNumber(16) },
				{ MathNumber.PLUS_INFINITY, MathNumber.ZERO, new MathNumber(-16), MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected after negation",
				res.equals(dbmFromMatrixMathNumbers(resultStrongClosure, y, x)));
	}

	@Test
	public void testNegationAssignment() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// y = 7
		Constant c7 = new Constant(NumericType, new MathNumber(7), location);
		DifferenceBoundMatrix step1 = dbm.assign(y, c7, pp, oracle);

		// x = -y
		UnaryExpression negY = new UnaryExpression(NumericType, y, NumericNegation.INSTANCE, location);
		DifferenceBoundMatrix result = step1.assign(x, negY, pp, oracle);

		assertTrue("Result should know x", result.knowsIdentifier(x));
		assertTrue("Result should know y", result.knowsIdentifier(y));
		assertFalse("Result should not be bottom", result.isBottom());
		// expected y=7, x=-7

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-14), MathNumber.PLUS_INFINITY, MathNumber.ZERO },
				{ new MathNumber(14), MathNumber.ZERO, MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected for negation assignment",
				result.equals(dbmFromMatrixMathNumbers(resultStrongClosure, y, x)));
	}

	@Test
	public void testAdditionNegativeConstantEquivalence() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// y = 5
		Constant c5 = new Constant(NumericType, new MathNumber(5), location);
		DifferenceBoundMatrix base = dbm.assign(y, c5, pp, oracle);

		// x = y + (-3)
		Constant cMinus3 = new Constant(NumericType, new MathNumber(-3), location);
		BinaryExpression addNeg = new BinaryExpression(NumericType, y, cMinus3,
				NumericNonOverflowingAdd.INSTANCE, location);
		DifferenceBoundMatrix res1 = base.assign(x, addNeg, pp, oracle);

		System.out.println("----- res1 -----");
		System.out.println(res1.representation());

		// x' = y - 3
		Constant c3 = new Constant(NumericType, new MathNumber(3), location);
		BinaryExpression sub = new BinaryExpression(NumericType, y, c3,
				NumericNonOverflowingSub.INSTANCE,
				location);
		DifferenceBoundMatrix res2 = base.assign(x, sub, pp, oracle);

		System.out.println("----- res2 -----");
		System.out.println(res2.representation());

		// assertTrue(res1.knowsIdentifier(x) && res2.knowsIdentifier(x), "Both
		// results
		// should know x");
		// assertTrue(res1.knowsIdentifier(y) && res2.knowsIdentifier(y), "Both
		// results
		// should know y");
		// System.out.println("res1 <= res2: " + res1.lessOrEqual(res2));
		// System.out.println("res2 <= res1: " + res2.lessOrEqual(res1));
		// assertTrue(res1.lessOrEqual(res2) && res2.lessOrEqual(res1),
		// "y + (-3) and y - 3 assignments should be equivalent");
		// expected y=5, x=2

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-10), new MathNumber(-3), MathNumber.PLUS_INFINITY },
				{ new MathNumber(10), MathNumber.ZERO, MathNumber.PLUS_INFINITY, new MathNumber(3) },
				{ new MathNumber(3), MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, new MathNumber(-3), MathNumber.PLUS_INFINITY, MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected for addition/subtraction equivalence",
				res1.equals(dbmFromMatrixMathNumbers(resultStrongClosure, y, x)));
		assertTrue("DBM representation should match expected for addition/subtraction equivalence",
				res2.equals(dbmFromMatrixMathNumbers(resultStrongClosure, y, x)));

	}

	@Test
	public void testChainedMultipleAssignments() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);
		Variable z = new Variable(NumericType, "z", location);

		DifferenceBoundMatrix s1 = dbm.assign(x, new Constant(NumericType, new MathNumber(1), location),
				pp,
				oracle); // x=1
		DifferenceBoundMatrix s2 = s1.assign(y,
				new BinaryExpression(NumericType, x,
						new Constant(NumericType, new MathNumber(2), location),
						NumericNonOverflowingAdd.INSTANCE, location),
				pp, oracle); // y=x+2
		DifferenceBoundMatrix s3 = s2.assign(z,
				new BinaryExpression(NumericType, y,
						new Constant(NumericType, new MathNumber(4), location),
						NumericNonOverflowingSub.INSTANCE, location),
				pp, oracle); // z=y-4
		DifferenceBoundMatrix s4 = s3.assign(x,
				new BinaryExpression(NumericType, z,
						new Constant(NumericType, new MathNumber(2), location),
						NumericNonOverflowingAdd.INSTANCE, location),
				pp, oracle); // x=z+2

		assertTrue("Should know x", s4.knowsIdentifier(x));
		assertTrue("Should know y", s4.knowsIdentifier(y));
		assertTrue("Should know z", s4.knowsIdentifier(z));
		assertFalse("DBM should not be bottom after chained assignments", s4.isBottom());

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY,
						new MathNumber(-2), MathNumber.PLUS_INFINITY },
				{ MathNumber.PLUS_INFINITY, MathNumber.ZERO, MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY,
						MathNumber.PLUS_INFINITY, new MathNumber(2) },
				{ MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, MathNumber.ZERO, new MathNumber(-6),
						new MathNumber(-4), new MathNumber(-2) },
				{ MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, new MathNumber(6), MathNumber.ZERO,
						new MathNumber(2), new MathNumber(4) },
				{ new MathNumber(2), MathNumber.PLUS_INFINITY, new MathNumber(4), new MathNumber(-2), MathNumber.ZERO,
						new MathNumber(2) },
				{ MathNumber.PLUS_INFINITY, new MathNumber(-2), new MathNumber(2), new MathNumber(-4),
						new MathNumber(-2), MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected after chained assignments",
				s4.equals(dbmFromMatrixMathNumbers(resultStrongClosure, x, y, z)));

		assertTrue(s4.knowsIdentifier(x) && s4.knowsIdentifier(y) && s4.knowsIdentifier(z));
	}
	// }

	// public class ConstraintOperations {

	@Test
	public void testSimpleAssume() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(4), location);

		// Create x = 5
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// Assume x <= 4 (which is false since x = 5)
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonLe.INSTANCE,
				location);
		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);
		System.out.println(result.representation());

		assertTrue("Unfeasible constraint should drive DBM to bottom", result.isBottom());
	}

	@Test
	public void testDifferenceConstraintAssume() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);
		Constant c3 = new Constant(NumericType, new MathNumber(3), location);

		// Create x = 5, y = 10
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle)
				.assign(y, c2, pp, oracle);

		// Assume x - y <= 3 (which is 5 - 10 <= 3, i.e., -5 <= 3, true)
		BinaryExpression diff = new BinaryExpression(NumericType, x, y,
				NumericNonOverflowingSub.INSTANCE,
				location);
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, diff, c3,
				ComparisonLe.INSTANCE,
				location);

		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		System.out.println(result.representation());

		// test the matrix directly

		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-10), MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY },
				{ new MathNumber(10), MathNumber.ZERO, MathNumber.PLUS_INFINITY, new MathNumber(3) },
				{ new MathNumber(3), MathNumber.PLUS_INFINITY, MathNumber.ZERO, new MathNumber(-20) },
				{ MathNumber.PLUS_INFINITY, MathNumber.PLUS_INFINITY, new MathNumber(20), MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected after difference constraint assume",
				result.equals(dbmFromMatrixMathNumbers(resultStrongClosure, x, y)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible difference constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeSubAddConstLeZeroDoesNotThrow() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		Constant c0 = new Constant(NumericType, new MathNumber(0), location);
		Constant c1 = new Constant(NumericType, new MathNumber(1), location);

		// Set x = 0, y = 0
		DifferenceBoundMatrix initial = dbm.assign(x, c0, pp, oracle)
				.assign(y, c0, pp, oracle);

		// Build (x - y) + 1 <= 0
		BinaryExpression sub = new BinaryExpression(NumericType, x, y,
				NumericNonOverflowingSub.INSTANCE, location);
		BinaryExpression add = new BinaryExpression(NumericType, sub, c1,
				NumericNonOverflowingAdd.INSTANCE, location);
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, add, c0,
				ComparisonLe.INSTANCE, location);

		// This should not throw. The constraint is infeasible given x=0,y=0,
		// so we expect a bottom DBM after closure.
		DifferenceBoundMatrix result = initial.assume(constraint, pp, pp, oracle);

		assertNotNull("Assume result should not be null", result);
		assertTrue("Infeasible constraint should drive DBM to bottom", result.closure().isBottom());
	}

	@Test
	public void testSumConstraintAssume() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// Set x = 4, y = 5
		Constant cx = new Constant(NumericType, new MathNumber(4), location);
		Constant cy = new Constant(NumericType, new MathNumber(5), location);
		DifferenceBoundMatrix initial = dbm.assign(x, cx, pp, oracle)
				.assign(y, cy, pp, oracle);

		// Assume x + y <= 0 (which is 9 <= 0, false since x=4 and y=5)
		BinaryExpression sum = new BinaryExpression(NumericType, x, y,
				NumericNonOverflowingAdd.INSTANCE, location);
		Constant limit = new Constant(NumericType, MathNumber.ZERO, location);
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, sum, limit,
				ComparisonLe.INSTANCE, location);

		DifferenceBoundMatrix result = initial.assume(constraint, pp, pp, oracle);

		assertNotNull("Assume result should not be null", result);
		assertTrue("Infeasible sum constraint should drive the DBM to bottom", result.closure().isBottom());
	}

	@Test
	public void testAssumeGreaterThan() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(3), location);

		// Create x = 5
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// Assume x >= 3 (should be normalized to - x + 3 <= 0)
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonGe.INSTANCE,
				location);
		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		System.out.println(result.representation().toString());

		assertTrue("DBM representation should match expected before assume",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -10 },
						{ 10, 0 }
				}, x)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeLessThan() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);

		// Create x = 5
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// Assume x < 10 (should be normalized to x - 9 <= 0)
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonLt.INSTANCE,
				location);
		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		System.out.println(result.representation());

		assertTrue("DBM representation should match expected before assume",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -10 },
						{ 10, 0 }
				}, x)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeGreaterOrEqual() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(3), location);

		// Create x = 5
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// Assume x >= 3
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonGe.INSTANCE,
				location);
		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		assertTrue("DBM representation should match expected before assume",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -10 },
						{ 10, 0 }
				}, x)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeLessOrEqual() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(7), location);

		// Create x = 5
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// Assume x <= 7
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonLe.INSTANCE,
				location);
		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		assertTrue("DBM representation should match expected before assume",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -10 },
						{ 10, 0 }
				}, x)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeEquality() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(5), location);

		// Create x = 5
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// Assume x == 5
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonEq.INSTANCE,
				location);
		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		assertTrue("DBM representation should match expected before assume",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -10 },
						{ 10, 0 }
				}, x)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeInequality() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(3), location);

		// Create x = 5
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// Assume x != 3
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonNe.INSTANCE,
				location);
		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		System.out.println("result : " + result.representation());

		assertTrue("DBM representation should match expected before assume",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -10 },
						{ 10, 0 }
				}, x)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeLogicalAnd() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);
		Constant c3 = new Constant(NumericType, new MathNumber(15), location);

		// Create x = 5, y = 10
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle)
				.assign(y, c2, pp, oracle);

		// Assume (x <= 10) AND (y <= 15)
		BinaryExpression leftConstraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonLe.INSTANCE,
				location);
		BinaryExpression rightConstraint = new BinaryExpression(BoolType.INSTANCE, y, c3,
				ComparisonLe.INSTANCE,
				location);
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, leftConstraint,
				rightConstraint,
				LogicalAnd.INSTANCE, location);

		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		assertTrue("DBM representation should match expected before assume",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -10, 5, -15 },
						{ 10, 0, 15, -5 },
						{ -5, -15, 0, -20 },
						{ 15, 5, 20, 0 }
				}, x, y)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeLogicalOr() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);
		Constant c3 = new Constant(NumericType, new MathNumber(15), location);

		// Create x = 5, y = 10
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle)
				.assign(y, c2, pp, oracle);

		// Assume (x <= 3) OR (y <= 15)
		BinaryExpression leftConstraint = new BinaryExpression(BoolType.INSTANCE, x,
				new Constant(NumericType, new MathNumber(3), location), ComparisonLe.INSTANCE,
				location);
		BinaryExpression rightConstraint = new BinaryExpression(BoolType.INSTANCE, y, c3,
				ComparisonLe.INSTANCE,
				location);
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, leftConstraint,
				rightConstraint,
				LogicalOr.INSTANCE, location);

		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		// Matrice data da step1
		MathNumber[][] resultStrongClosure = new MathNumber[][] {
				{ MathNumber.ZERO, new MathNumber(-10), new MathNumber(5), new MathNumber(-15) },
				{ new MathNumber(10.0), MathNumber.ZERO, new MathNumber(15), new MathNumber(-5) },
				{ new MathNumber(-5), new MathNumber(-15), MathNumber.ZERO, new MathNumber(-20) },
				{ new MathNumber(15), new MathNumber(5), new MathNumber(20), MathNumber.ZERO }
		};

		Floyd.strongClosureFloyd(resultStrongClosure);

		assertTrue("DBM representation should match expected before assume",
				result.equals(dbmFromMatrixMathNumbers(resultStrongClosure, x, y)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeNegatedConstraint() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(3), location);

		// Create x = 5
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// Assume NOT(x <= 3) which should be equivalent to x > 3
		BinaryExpression innerConstraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonLe.INSTANCE,
				location);
		it.unive.lisa.symbolic.value.UnaryExpression constraint = new it.unive.lisa.symbolic.value.UnaryExpression(
				BoolType.INSTANCE, innerConstraint, LogicalNegation.INSTANCE, location);

		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		assertTrue("DBM representation should match expected before assume",
				result.equals(dbmFromMatrix(new double[][] {
						{ 0, -10 },
						{ 10, 0 }
				}, x)));

		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());
	}

	@Test
	public void testAssumeInfeasibleConstraint() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(3), location);

		// Create x = 5
		DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

		// Assume x <= 3 (which is false since x = 5)
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
				ComparisonLe.INSTANCE,
				location);
		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		assertTrue("DBM should be bottom after infeasible assume",
				result.isBottom());
		assertNotNull("Assume result should not be null", result);
	}

	@Test
	public void testAssumeNegatedXMinusHundred() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		// x = 10
		Constant c10 = new Constant(NumericType, new MathNumber(10), location);
		DifferenceBoundMatrix step1 = dbm.assign(x, c10, pp, oracle);

		// Build expression: -x - 100
		UnaryExpression negX = new UnaryExpression(NumericType, x, NumericNegation.INSTANCE,
				location);
		Constant c100 = new Constant(NumericType, new MathNumber(100), location);
		BinaryExpression leftExpr = new BinaryExpression(NumericType, negX, c100,
				NumericNonOverflowingSub.INSTANCE, location);

		// Constraint: (-x - 100) <= 0 (Equivalent to x >= -100)
		Constant c0 = new Constant(NumericType, new MathNumber(0), location);
		BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, leftExpr, c0,
				ComparisonLe.INSTANCE, location);

		DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

		System.out.println(result.representation());

		// The constraint is satisfied by x = 10 (-10 - 100 <= 0 => -110 <= 0)
		assertNotNull("Assume result should not be null", result);
		assertFalse("Feasible constraint should not make DBM bottom", result.isBottom());

		// Expect no stronger constraint than the original assignment (x == 10)
		assertTrue("DBM representation should match expected after assume", result.equals(dbmFromMatrix(new double[][] {
				{ 0, -20 },
				{ 20, 0 }
		}, x)));
	}
	// }

	// public class ForgetOperations {

	@Test
	public void testForgetKnownIdentifier() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);

		DifferenceBoundMatrix step1 = dbm.assign(x, c, pp, oracle);
		assertTrue("Variable should be known before forget", step1.knowsIdentifier(x));

		DifferenceBoundMatrix result = step1.forgetIdentifier(x);

		// After forgetting, the variable mapping might still exist but
		// constraints are
		// relaxed
		assertNotNull("Forget result should not be null", result);
	}

	@Test
	public void testForgetUnknownIdentifier() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);

		DifferenceBoundMatrix result = dbm.forgetIdentifier(x);

		assertNotNull("Forget result should not be null", result);
		assertEquals("Forgetting unknown identifier should return same DBM", dbm, result);
	}

	@Test
	public void testForgetIdentifiersIf() throws SemanticException {
		Predicate<Identifier> testPredicate = id -> id.getName().startsWith("temp");

		DifferenceBoundMatrix result = dbm.forgetIdentifiersIf(testPredicate);

		assertNotNull("ForgetIdentifiersIf result should not be null", result);
		// Placeholder implementation should return unchanged DBM
		assertEquals("Placeholder implementation should return unchanged DBM", dbm, result);
	}
	// }

	// public class DomainConversions {

	@Test
	public void testEmptyDbmToInterval() throws SemanticException {
		ValueEnvironment<Interval> result = dbm.toInterval();

		assertNotNull("Conversion result should not be null", result);
		assertTrue("Empty DBM should produce empty interval environment", result.getKeys().isEmpty());
	}

	@Test
	public void testDbmWithVariablesToInterval() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);

		DifferenceBoundMatrix withVars = dbm.assign(x, c1, pp, oracle)
				.assign(y, c2, pp, oracle);

		ValueEnvironment<Interval> result = withVars.toInterval();

		assertNotNull("Conversion result should not be null", result);
		assertFalse("DBM with variables should produce non-empty interval environment", result.getKeys().isEmpty());
	}

	@Test
	public void testIntervalDomainToDbm() throws SemanticException {
		ValueEnvironment<Interval> env = new ValueEnvironment<>(new Interval());
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);

		// Add some intervals
		env = env.putState(x, new Interval(new MathNumber(0), new MathNumber(10)));
		env = env.putState(y, new Interval(new MathNumber(5), new MathNumber(15)));

		DifferenceBoundMatrix result = DifferenceBoundMatrix.fromIntervalDomain(env);

		assertNotNull("Conversion result should not be null", result);
		assertTrue("Result should know first variable", result.knowsIdentifier(x));
		assertTrue("Result should know second variable", result.knowsIdentifier(y));
	}

	@Test
	public void testRoundTripConversion() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);

		DifferenceBoundMatrix original = dbm.assign(x, c, pp, oracle);
		ValueEnvironment<Interval> intervals = original.toInterval();
		DifferenceBoundMatrix restored = DifferenceBoundMatrix.fromIntervalDomain(intervals);

		assertNotNull("Restored DBM should not be null", restored);
		assertTrue("Restored DBM should know the variable", restored.knowsIdentifier(x));
	}
	// }

	// public class LatticeJoinOperations {

	@Test
	public void testLubBottomBottom() throws SemanticException {
		DifferenceBoundMatrix bottom1 = dbm.bottom();
		DifferenceBoundMatrix bottom2 = dbm.bottom();

		DifferenceBoundMatrix result = bottom1.lubAux(bottom2);

		assertNotNull("LUB result should not be null", result);
		assertTrue("LUB of two bottom DBMs should be bottom", result.isBottom());
	}

	@Test
	public void testLubSameVariables() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);

		DifferenceBoundMatrix dbm1 = dbm.assign(x, c1, pp, oracle);
		DifferenceBoundMatrix dbm2 = dbm.assign(x, c2, pp, oracle);

		DifferenceBoundMatrix result = dbm1.lubAux(dbm2);

		assertNotNull("LUB result should not be null", result);
		assertTrue("LUB result should know the variable", result.knowsIdentifier(x));
	}

	@Test
	public void testLubDifferentVariables() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Variable y = new Variable(NumericType, "y", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);

		DifferenceBoundMatrix dbm1 = dbm.assign(x, c, pp, oracle);
		DifferenceBoundMatrix dbm2 = dbm.assign(y, c, pp, oracle);

		try {
			dbm1.lubAux(dbm2);
			fail("LUB of DBMs with different variables should throw SemanticException");
		} catch (SemanticException e) {
		}
	}
	// }

	// public class WideningOperations {

	@Test
	public void testWideningWithNull() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);

		DifferenceBoundMatrix original = dbm.assign(x, c, pp, oracle);

		// Da vedere se è widening come conflitto
		DifferenceBoundMatrix result = original.widening((DifferenceBoundMatrix) null);

		assertEquals("Widening with null should return original DBM", original, result);
	}

	@Test
	public void testWideningWithSame() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);

		DifferenceBoundMatrix original = dbm.assign(x, c, pp, oracle);
		DifferenceBoundMatrix result = original.widening(original);

		assertNotNull("Widening result should not be null", result);
	}

	@Test
	public void testWideningWithDifferent() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c1 = new Constant(NumericType, new MathNumber(5), location);
		Constant c2 = new Constant(NumericType, new MathNumber(10), location);

		DifferenceBoundMatrix dbm1 = dbm.assign(x, c1, pp, oracle);
		DifferenceBoundMatrix dbm2 = dbm.assign(x, c2, pp, oracle);

		DifferenceBoundMatrix result = dbm1.widening(dbm2);

		assertNotNull("Widening result should not be null", result);
	}
	// }

	// public class ScopeOperations {

	@Test
	public void testPushScope() throws SemanticException {
		ScopeToken token = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("Unimplemented method 'getLocation'");
			}

		});

		DifferenceBoundMatrix result = dbm.pushScope(token);

		assertNotNull("Push scope result should not be null", result);
		// Placeholder implementation should return unchanged DBM
		assertEquals("Placeholder push scope should return unchanged DBM", dbm, result);
	}

	@Test
	public void testPopScope() throws SemanticException {
		// ScopeToken token = mock(ScopeToken.class);

		ScopeToken token = new ScopeToken(new CodeElement() {

			@Override
			public CodeLocation getLocation() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("Unimplemented method 'getLocation'");
			}

		});

		DifferenceBoundMatrix result = dbm.popScope(token);

		assertNotNull("Pop scope result should not be null", result);
		// Placeholder implementation should return unchanged DBM
		assertEquals("Placeholder pop scope should return unchanged DBM", dbm, result);
	}
	// }

	// public class SatisfiabilityOperations {

	@Test
	public void testSatisfies() throws SemanticException {
		Variable x = new Variable(NumericType, "x", location);
		Constant c = new Constant(NumericType, new MathNumber(5), location);

		BinaryExpression expr = new BinaryExpression(BoolType.INSTANCE, x, c, ComparisonLe.INSTANCE,
				location);
		Satisfiability result = dbm.satisfies(expr, pp, oracle);

		assertNotNull("Satisfiability result should not be null", result);
		// Placeholder implementation returns UNKNOWN
		assertEquals("Placeholder implementation should return UNKNOWN", Satisfiability.UNKNOWN, result);
	}
	// }

	// public class EdgeCasesAndErrorHandling {

	@Test
	public void testAssignmentWithNullIdentifier() {
		Constant c = new Constant(NumericType, new MathNumber(5), location);

		try {
			dbm.assign(null, c, pp, oracle);
			fail("Assignment with null identifier should throw exception");
		} catch (Exception e) {

		}
	}

	@Test
	public void testAssignmentWithNullExpression() {
		Variable x = new Variable(NumericType, "x", location);

		try {
			dbm.assign(x, null, pp, oracle);
			fail("Assignment with null expression should throw exception");
		} catch (Exception e) {
		}
	}

	@Test
	public void testLargeMatrixOperations() throws SemanticException {
		// Create a DBM with multiple variables to test matrix scaling
		DifferenceBoundMatrix current = dbm;

		for (int i = 0; i < 5; i++) {
			Variable var = new Variable(NumericType, "var" + i, location);
			Constant val = new Constant(NumericType, new MathNumber(i * 10), location);
			current = current.assign(var, val, pp, oracle);
		}

		assertNotNull("DBM with multiple variables should not be null", current);
		assertFalse("DBM with multiple variables should not be bottom", current.isBottom());

		// Test that representation works with larger matrices
		StructuredRepresentation repr = current.representation();
		assertNotNull("Large DBM representation should not be null", repr);
	}

	@Test
	public void testConversionWithMalformedIntervals() throws SemanticException {
		ValueEnvironment<Interval> env = new ValueEnvironment<>(new Interval());

		// Test conversion with empty environment
		DifferenceBoundMatrix result = DifferenceBoundMatrix.fromIntervalDomain(env);

		assertNotNull("Conversion from empty interval environment should not be null", result);
		assertTrue("Conversion from empty environment should produce bottom DBM", result.isBottom());
	}
	// }
}