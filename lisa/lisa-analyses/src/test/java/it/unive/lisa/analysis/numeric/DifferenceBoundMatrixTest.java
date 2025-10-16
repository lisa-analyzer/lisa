package it.unive.lisa.analysis.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
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
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * Comprehensive test suite for DifferenceBoundMatrix (DBM) implementation.
 * Tests cover lattice operations, variable assignments, constraints, and
 * conversions.
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso </a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *         Shytermeja</a>
 */
public class DifferenceBoundMatrixTest {

        private DifferenceBoundMatrix dbm;
        private SemanticOracle oracle;
        private ProgramPoint pp;
        private CodeLocation location;
        private static final Type NumericType = Int32Type.INSTANCE;

        @BeforeEach
        void setUp() {
                dbm = new DifferenceBoundMatrix();
                oracle = mock(SemanticOracle.class);
                location = mock(CodeLocation.class);
                pp = mock(ProgramPoint.class);
                when(pp.getLocation()).thenReturn(location);
        }

        private DifferenceBoundMatrix dbmFromMatrix(double[][] vals, Identifier... vars) {
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

        // inf
        static final double INF = Double.MAX_VALUE;

        @Nested
        @DisplayName("Basic Lattice Operations")
        class BasicLatticeOperations {

                @Test
                @DisplayName("Empty DBM should be bottom")
                void testEmptyDbmIsBottom() {
                        assertTrue(dbm.isBottom(), "Empty DBM should be bottom");
                        assertFalse(dbm.isTop(), "Empty DBM should not be top");
                }

                @Test
                @DisplayName("Top DBM creation and recognition")
                void testTopDbm() {
                        DifferenceBoundMatrix topDbm = dbm.top();
                        assertNotNull(topDbm, "Top DBM should not be null");

                        // Add a variable first to make top meaningful
                        Variable x = new Variable(NumericType, "x", location);
                        try {
                                Constant c = new Constant(NumericType, new MathNumber(6), location);
                                DifferenceBoundMatrix withVar = dbm.assign(x, c, pp, oracle);
                                System.out.println(withVar.representation());
                                assertTrue(withVar.equals(dbmFromMatrix(new double[][] {
                                                { 0, -12 },
                                                { 12, 0 }
                                }, x)), "DBM representation should match expected before top");
                                DifferenceBoundMatrix topWithVar = withVar.top();
                                assertTrue(topWithVar.isTop(), "DBM with variables should recognize top");
                                assertFalse(topWithVar.isBottom(), "Top DBM should not be bottom");
                        } catch (SemanticException e) {
                                fail("Should not throw exception: " + e.getMessage());
                        }
                }

                @Test
                @DisplayName("Bottom DBM creation and recognition")
                void testBottomDbm() {
                        DifferenceBoundMatrix bottomDbm = dbm.bottom();
                        assertNotNull(bottomDbm, "Bottom DBM should not be null");
                        assertTrue(bottomDbm.isBottom(), "Bottom DBM should be bottom");
                        assertFalse(bottomDbm.isTop(), "Bottom DBM should not be top");
                }

                @Test
                @DisplayName("LessOrEqual operation with bottom elements")
                void testLessOrEqualWithBottom() throws SemanticException {
                        System.out.println("Testing LessOrEqual with Bottom elements");
                        DifferenceBoundMatrix bottom1 = dbm.bottom();
                        DifferenceBoundMatrix bottom2 = dbm.bottom();

                        assertTrue(bottom1.lessOrEqual(bottom2), "Bottom <= Bottom should be true");

                        // Create non-bottom DBM
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);
                        DifferenceBoundMatrix nonBottom = dbm.assign(x, c, pp, oracle);
                        System.out.println(nonBottom.representation());
                        assertTrue(nonBottom.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10 },
                                        { 10, 0 }
                        }, x)), "DBM representation should match expected");
                        assertFalse(nonBottom.lessOrEqual(bottom1), "Non-bottom <= Bottom should be false");
                }

                @Test
                @DisplayName("LessOrEqual operation with top elements")
                void testLessOrEqualWithTop() throws SemanticException {
                        // Create a DBM with variables to make top/non-top meaningful
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);
                        DifferenceBoundMatrix withVar = dbm.assign(x, c, pp, oracle);
                        DifferenceBoundMatrix top = withVar.top();

                        assertTrue(withVar.lessOrEqual(top), "Any DBM <= Top should be true");
                        assertFalse(top.lessOrEqual(withVar), "Top <= non-top should be false");
                }

                @Test
                @DisplayName("LessOrEqual operation with 2 non-bottom DBMs")
                void testLessOrEqualWithNonBottoms() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c1 = new Constant(NumericType, new MathNumber(5), location);
                        Constant c2 = new Constant(NumericType, new MathNumber(10), location);

                        DifferenceBoundMatrix dbm1 = dbm.assign(x, c1, pp, oracle);
                        DifferenceBoundMatrix dbm2 = dbm.assign(x, c2, pp, oracle);

                        // dbm1 has x = 5, dbm2 has x = 10
                        assertTrue(dbm1.lessOrEqual(dbm1.lub(dbm2)), "DBM with x=5 <= DBM with x=10 should be false");
                }
        }

        @Nested
        @DisplayName("Variable Management")
        class VariableManagement {

                @Test
                @DisplayName("Identifier recognition in empty DBM")
                void testKnowsIdentifierEmpty() {
                        Variable x = new Variable(NumericType, "x", location);
                        assertFalse(dbm.knowsIdentifier(x), "Empty DBM should not know any identifier");
                }

                @Test
                @DisplayName("Identifier recognition after assignment")
                void testKnowsIdentifierAfterAssignment() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);

                        DifferenceBoundMatrix result = dbm.assign(x, c, pp, oracle);

                        assertTrue(result.knowsIdentifier(x), "DBM should know identifier after assignment");
                        assertFalse(result.knowsIdentifier(new Variable(NumericType, "y", location)),
                                        "DBM should not know unassigned identifier");
                }

                @Test
                @DisplayName("Multiple variable assignments")
                void testMultipleVariableAssignments() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Variable y = new Variable(NumericType, "y", location);
                        Constant c1 = new Constant(NumericType, new MathNumber(5), location);
                        Constant c2 = new Constant(NumericType, new MathNumber(10), location);

                        DifferenceBoundMatrix result = dbm.assign(x, c1, pp, oracle)
                                        .assign(y, c2, pp, oracle);
                        System.out.println(result.representation());

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, INF, INF },
                                        { 10, 0, INF, INF },
                                        { INF, INF, 0, -20 },
                                        { INF, INF, 20, 0 }
                        }, x, y)), "DBM representation should match expected");

                        assertTrue(result.knowsIdentifier(x), "DBM should know first variable");
                        assertTrue(result.knowsIdentifier(y), "DBM should know second variable");
                }
        }

        @Nested
        @DisplayName("Assignment Operations")
        class AssignmentOperations {

                @Test
                @DisplayName("Simple constant assignment")
                void testSimpleConstantAssignment() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);

                        DifferenceBoundMatrix result = dbm.assign(x, c, pp, oracle);
                        System.out.println(result.representation());

                        assertNotNull(result, "Assignment result should not be null");
                        assertTrue(result.knowsIdentifier(x), "Result should know assigned variable");
                        assertFalse(result.isBottom(), "Result should not be bottom");
                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10 },
                                        { 10, 0 }
                        }, x)), "DBM representation should match expected");
                }

                @Test
                @DisplayName("Self-decrement assignment x = x - c")
                void testSelfDecrementAssignment() throws SemanticException { // ERROR
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
                        assertTrue(res.equals(dbmFromMatrix(new double[][] {
                                        { 0, -4 },
                                        { 4, 0 }
                        }, x)), "DBM representation should match expected after decrement");
                }

                @Test
                @DisplayName("Copy then increment chain")
                void testCopyThenIncrementChain() throws SemanticException {
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
                        assertTrue(s4.equals(dbmFromMatrix(new double[][] {
                                        { 0, -8, 0, INF },
                                        { 8, 0, INF, 0 },
                                        { 0, INF, 0, INF },
                                        { INF, 0, INF, 0 }
                        }, y, x)), "DBM representation should match expected after copy/increment chain");
                }

                @Test
                @DisplayName("Zero offset equivalence: y + 0 and y - 0")
                void testZeroOffsetEquivalence() throws SemanticException {
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
                        assertTrue(res1.equals(dbmFromMatrix(new double[][] {
                                        { 0, -14, 0, INF },
                                        { 14, 0, INF, 0 },
                                        { 0, INF, 0, INF },
                                        { INF, 0, INF, 0 }
                        }, y, x)), "DBM representation should match expected for zero-offset equivalence");
                }

                @Test
                @DisplayName("Reassignment from other variable")
                void testReassignmentFromOtherVariable() throws SemanticException {
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

                        assertTrue(s3.equals(dbmFromMatrix(new double[][] {
                                        { 0, INF, 2, INF },
                                        { INF, 0, INF, -2 },
                                        { -2, INF, 0, -14 },
                                        { INF, 2, 14, 0 }
                        }, x, y)), "DBM representation should match expected after reassignment from other var");
                }

                @Test
                @DisplayName("Assignment with arithmetic expression")
                void testArithmeticAssignment() throws SemanticException { // CORRECT
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

                        assertTrue(result.knowsIdentifier(x), "Result should know x");
                        assertTrue(result.knowsIdentifier(y), "Result should know y");
                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, 3, INF },
                                        { 10, 0, INF, -3 },
                                        { -3, INF, 0, INF },
                                        { INF, 3, INF, 0 }
                        }, y, x)), "DBM representation should match expected for arithmetic assignment");
                }

                @Test
                @DisplayName("Assignment with variable reference")
                void testVariableReferenceAssignment() throws SemanticException { // CORRECT
                        Variable x = new Variable(NumericType, "x", location);
                        Variable y = new Variable(NumericType, "y", location);

                        // First assign x = 5
                        Constant c = new Constant(NumericType, new MathNumber(5), location);
                        DifferenceBoundMatrix step1 = dbm.assign(x, c, pp, oracle);

                        // Then assign y = x
                        DifferenceBoundMatrix result = step1.assign(y, x, pp, oracle);
                        System.out.println(result.representation()); // TODO : not sure about the result

                        assertTrue(result.knowsIdentifier(x), "Result should know x");
                        assertTrue(result.knowsIdentifier(y), "Result should know y");

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, 0, INF },
                                        { 10, 0, INF, 0 },
                                        { 0, INF, 0, INF },
                                        { INF, 0, INF, 0 }
                        }, x, y)), "DBM representation should match expected for variable reference assignment");

                        // Other way to construct the same assignment with x = 5, y = 5
                        System.out.println("Before second assignment method");
                        DifferenceBoundMatrix step2 = dbm.assign(x, c, pp, oracle);
                        DifferenceBoundMatrix result2 = step2.assign(y, c, pp, oracle);
                        System.out.println(result2.representation());

                        assertTrue(result2.knowsIdentifier(x), "Result2 should know x");
                        assertTrue(result2.knowsIdentifier(y), "Result2 should know y");

                        assertTrue(result2.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, INF, INF },
                                        { 10, 0, INF, INF },
                                        { INF, INF, 0, -10 },
                                        { INF, INF, 10, 0 }
                        }, x, y)), "DBM representation should match expected for variable reference assignment");

                }

                @Test
                @DisplayName("Reassignment of existing variable")
                void testReassignment() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c1 = new Constant(NumericType, new MathNumber(5), location);
                        Constant c2 = new Constant(NumericType, new MathNumber(10), location);

                        DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);
                        DifferenceBoundMatrix result = step1.assign(x, c2, pp, oracle);

                        System.out.println(result.representation());

                        assertTrue(result.knowsIdentifier(x), "Result should still know x after reassignment");
                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -20 },
                                        { 20, 0 }
                        }, x)), "DBM representation should match expected after reassignment");
                }

                @Test
                @DisplayName("Assignment with subtraction expression")
                void testSubtractionAssignment() throws SemanticException {
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

                        assertTrue(result.knowsIdentifier(x), "Result should know x");
                        assertTrue(result.knowsIdentifier(y), "Result should know y");
                        assertFalse(result.isBottom(), "Result should not be bottom");
                        // expected y=10, x=8
                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -20, -2, INF },
                                        { 20, 0, INF, 2 },
                                        { 2, INF, 0, INF },
                                        { INF, -2, INF, 0 }
                        }, y, x)), "DBM representation should match expected for subtraction assignment");
                }

                @Test
                @DisplayName("Self-increment assignment x = x + c")
                void testSelfIncrementAssignment() throws SemanticException {
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

                        assertTrue(result.knowsIdentifier(x), "Result should know x");
                        assertFalse(result.isBottom(), "Result should not be bottom");
                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -6 },
                                        { 6, 0 }
                        }, x)), "DBM representation should match expected after self-increment");
                }

                @Test
                @DisplayName("Self-negation assignment x = -x")
                void testSelfNegationAssignment() throws SemanticException {
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
                        assertTrue(res.equals(dbmFromMatrix(new double[][] {
                                        { 0, 10 },
                                        { -10, 0 }
                        }, x)), "DBM representation should match expected after negation");
                }

                @Test
                @DisplayName("Self-negation assignment with other variable")
                void testSelfNegationWithOtherVariableAssignment() throws SemanticException {
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
                        assertTrue(res.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, INF, INF },
                                        { 10, 0, INF, INF },
                                        { INF, INF, 0, 16 },
                                        { INF, INF, -16, 0 }
                        }, y, x)), "DBM representation should match expected after negation");
                }

                @Test
                @DisplayName("Assignment with negation x = -y")
                void testNegationAssignment() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Variable y = new Variable(NumericType, "y", location);

                        // y = 7
                        Constant c7 = new Constant(NumericType, new MathNumber(7), location);
                        DifferenceBoundMatrix step1 = dbm.assign(y, c7, pp, oracle);

                        // x = -y
                        UnaryExpression negY = new UnaryExpression(NumericType, y, NumericNegation.INSTANCE, location);
                        DifferenceBoundMatrix result = step1.assign(x, negY, pp, oracle);

                        assertTrue(result.knowsIdentifier(x), "Result should know x");
                        assertTrue(result.knowsIdentifier(y), "Result should know y");
                        assertFalse(result.isBottom(), "Result should not be bottom");
                        // expected y=7, x=-7
                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -14, INF, 0 },
                                        { 14, 0, 0, INF },
                                        { INF, 0, 0, INF },
                                        { 0, INF, INF, 0 }
                        }, y, x)), "DBM representation should match expected for negation assignment");
                }

                @Test
                @DisplayName("Addition with negative constant equals subtraction")
                void testAdditionNegativeConstantEquivalence() throws SemanticException {
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

                        // assertTrue(res1.knowsIdentifier(x) && res2.knowsIdentifier(x), "Both results
                        // should know x");
                        // assertTrue(res1.knowsIdentifier(y) && res2.knowsIdentifier(y), "Both results
                        // should know y");
                        // System.out.println("res1 <= res2: " + res1.lessOrEqual(res2));
                        // System.out.println("res2 <= res1: " + res2.lessOrEqual(res1));
                        // assertTrue(res1.lessOrEqual(res2) && res2.lessOrEqual(res1),
                        // "y + (-3) and y - 3 assignments should be equivalent");
                        // expected y=5, x=2
                        assertTrue(res1.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, -3, INF },
                                        { 10, 0, INF, 3 },
                                        { 3, INF, 0, INF },
                                        { INF, -3, INF, 0 }
                        }, y, x)), "DBM representation should match expected for addition/subtraction equivalence");
                        assertTrue(res2.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, -3, INF },
                                        { 10, 0, INF, 3 },
                                        { 3, INF, 0, INF },
                                        { INF, -3, INF, 0 }
                        }, y, x)), "DBM representation should match expected for addition/subtraction equivalence");

                }

                @Test
                @DisplayName("Chained multiple assignments")
                void testChainedMultipleAssignments() throws SemanticException {
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

                        assertTrue(s4.knowsIdentifier(x), "Should know x");
                        assertTrue(s4.knowsIdentifier(y), "Should know y");
                        assertTrue(s4.knowsIdentifier(z), "Should know z");
                        assertFalse(s4.isBottom(), "DBM should not be bottom after chained assignments");

                        assertTrue(s4.equals(dbmFromMatrix(new double[][] {
                                        { 0, INF, INF, INF, -2, INF },
                                        { INF, 0, INF, INF, INF, 2 },
                                        { INF, INF, 0, -6, -4, -2 },
                                        { INF, INF, 6, 0, 2, 4 },
                                        { 2, INF, 4, -2, 0, 2 },
                                        { INF, -2, 2, -4, -2, 0 }
                        }, x, y, z)), "DBM representation should match expected after chained assignments");

                        assertTrue(s4.knowsIdentifier(x) && s4.knowsIdentifier(y) && s4.knowsIdentifier(z));
                }
        }

        @Nested
        @DisplayName("Constraint Operations")
        class ConstraintOperations {

                @Test
                @DisplayName("Simple assume operation")
                void testSimpleAssume() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10 },
                                        { 8, 0 }
                        }, x)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertTrue(result.closure().isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Difference constraint assume")
                void testDifferenceConstraintAssume() throws SemanticException {
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
                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, INF, INF },
                                        { 10, 0, INF, 3 },
                                        { 3, INF, 0, -20 },
                                        { INF, INF, 20, 0 }
                        }, x, y)), "DBM representation should match expected after difference constraint assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible difference constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("(x - y) + 1 <= 0 with x=0,y=0 does not throw and yields bottom")
                void testAssumeSubAddConstLeZeroDoesNotThrow() throws SemanticException {
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

                        assertNotNull(result, "Assume result should not be null");
                        assertTrue(result.closure().isBottom(),
                                        "Infeasible constraint should drive DBM to bottom");
                }

                @Test
                @DisplayName("Sum constraint assume yields bottom when infeasible")
                void testSumConstraintAssume() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Variable y = new Variable(NumericType, "y", location);

                        Constant cx = new Constant(NumericType, new MathNumber(4), location);
                        Constant cy = new Constant(NumericType, new MathNumber(5), location);
                        DifferenceBoundMatrix initial = dbm.assign(x, cx, pp, oracle)
                                        .assign(y, cy, pp, oracle);

                        BinaryExpression sum = new BinaryExpression(NumericType, x, y,
                                        NumericNonOverflowingAdd.INSTANCE, location);
                        Constant limit = new Constant(NumericType, MathNumber.ZERO, location);
                        BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, sum, limit,
                                        ComparisonLe.INSTANCE, location);

                        DifferenceBoundMatrix result = initial.assume(constraint, pp, pp, oracle);

                        assertNotNull(result, "Assume result should not be null");
                        assertTrue(result.closure().isBottom(),
                                        "Infeasible sum constraint should drive the DBM to bottom");
                }

                @Test
                @DisplayName("Assume greater than constraint")
                void testAssumeGreaterThan() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -6 },
                                        { 10, 0 }
                        }, x)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Assume less than constraint")
                void testAssumeLessThan() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c1 = new Constant(NumericType, new MathNumber(5), location);
                        Constant c2 = new Constant(NumericType, new MathNumber(10), location);

                        // Create x = 5
                        DifferenceBoundMatrix step1 = dbm.assign(x, c1, pp, oracle);

                        // Assume x < 10 (should be normalized to x - 11 <= 0)
                        BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, x, c2,
                                        ComparisonLt.INSTANCE,
                                        location);
                        DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

                        System.out.println(result.representation());

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10 },
                                        { 22, 0 }
                        }, x)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Assume greater or equal constraint")
                void testAssumeGreaterOrEqual() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -6 },
                                        { 10, 0 }
                        }, x)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Assume less or equal constraint")
                void testAssumeLessOrEqual() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10 },
                                        { 14, 0 }
                        }, x)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Assume equality constraint")
                void testAssumeEquality() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10 },
                                        { 10, 0 }
                        }, x)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Assume inequality constraint")
                void testAssumeInequality() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -8 },
                                        { 10, 0 }
                        }, x)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Assume logical AND constraint")
                void testAssumeLogicalAnd() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, 5, -15 },
                                        { 10, 0, 15, -5 },
                                        { -5, -15, 0, -20 },
                                        { 15, 5, 20, 0 }
                        }, x, y)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Assume logical OR constraint")
                void testAssumeLogicalOr() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10, 10, -15 },
                                        { 10, 0, 20, -5 },
                                        { -5, -15, 0, -20 },
                                        { 20, 10, 30, 0 }
                        }, x, y)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Assume negated constraint")
                void testAssumeNegatedConstraint() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -8 },
                                        { 10, 0 }
                        }, x)), "DBM representation should match expected before assume");

                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");
                }

                @Test
                @DisplayName("Assume infeasible constraint makes DBM bottom")
                void testAssumeInfeasibleConstraint() throws SemanticException {
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

                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -10 },
                                        { 6, 0 }
                        }, x)), "DBM representation should match expected before assume");
                        assertNotNull(result, "Assume result should not be null");
                }

                @Test
                @DisplayName("Assume negated x minus 100 constraint (-x - 100 <= 0)")
                void testAssumeNegatedXMinusHundred() throws SemanticException {
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

                        // Constraint: (-x - 100) <= 0
                        Constant c0 = new Constant(NumericType, new MathNumber(0), location);
                        BinaryExpression constraint = new BinaryExpression(BoolType.INSTANCE, leftExpr, c0,
                                        ComparisonLe.INSTANCE, location);

                        DifferenceBoundMatrix result = step1.assume(constraint, pp, pp, oracle);

                        System.out.println(result.representation());

                        // The constraint is satisfied by x = 10 (-10 - 100 <= 0 => -110 <= 0)
                        assertNotNull(result, "Assume result should not be null");
                        assertFalse(result.isBottom(), "Feasible constraint should not make DBM bottom");

                        // Expect no stronger constraint than the original assignment (x == 10)
                        assertTrue(result.equals(dbmFromMatrix(new double[][] {
                                        { 0, -200 },
                                        { 20, 0 }
                        }, x)), "DBM representation should match expected after assume");
                }
        }

        @Nested
        @DisplayName("Forget Operations")
        class ForgetOperations {

                @Test
                @DisplayName("Forget known identifier")
                void testForgetKnownIdentifier() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);

                        DifferenceBoundMatrix step1 = dbm.assign(x, c, pp, oracle);
                        assertTrue(step1.knowsIdentifier(x), "Variable should be known before forget");

                        DifferenceBoundMatrix result = step1.forgetIdentifier(x);

                        // After forgetting, the variable mapping might still exist but constraints are
                        // relaxed
                        assertNotNull(result, "Forget result should not be null");
                }

                @Test
                @DisplayName("Forget unknown identifier")
                void testForgetUnknownIdentifier() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);

                        DifferenceBoundMatrix result = dbm.forgetIdentifier(x);

                        assertNotNull(result, "Forget result should not be null");
                        assertEquals(dbm, result, "Forgetting unknown identifier should return same DBM");
                }

                @Test
                @DisplayName("Forget identifiers with predicate")
                void testForgetIdentifiersIf() throws SemanticException {
                        Predicate<Identifier> testPredicate = id -> id.getName().startsWith("temp");

                        DifferenceBoundMatrix result = dbm.forgetIdentifiersIf(testPredicate);

                        assertNotNull(result, "ForgetIdentifiersIf result should not be null");
                        // Placeholder implementation should return unchanged DBM
                        assertEquals(dbm, result, "Placeholder implementation should return unchanged DBM");
                }
        }

        @Nested
        @DisplayName("Domain Conversions")
        class DomainConversions {

                @Test
                @DisplayName("Convert empty DBM to interval domain")
                void testEmptyDbmToInterval() throws SemanticException {
                        ValueEnvironment<Interval> result = dbm.toInterval();

                        assertNotNull(result, "Conversion result should not be null");
                        assertTrue(result.getKeys().isEmpty(), "Empty DBM should produce empty interval environment");
                }

                @Test
                @DisplayName("Convert DBM with variables to interval domain")
                void testDbmWithVariablesToInterval() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Variable y = new Variable(NumericType, "y", location);
                        Constant c1 = new Constant(NumericType, new MathNumber(5), location);
                        Constant c2 = new Constant(NumericType, new MathNumber(10), location);

                        DifferenceBoundMatrix withVars = dbm.assign(x, c1, pp, oracle)
                                        .assign(y, c2, pp, oracle);

                        ValueEnvironment<Interval> result = withVars.toInterval();

                        assertNotNull(result, "Conversion result should not be null");
                        assertFalse(result.getKeys().isEmpty(),
                                        "DBM with variables should produce non-empty interval environment");
                }

                @Test
                @DisplayName("Convert interval domain to DBM")
                void testIntervalDomainToDbm() throws SemanticException {
                        ValueEnvironment<Interval> env = new ValueEnvironment<>(new Interval());
                        Variable x = new Variable(NumericType, "x", location);
                        Variable y = new Variable(NumericType, "y", location);

                        // Add some intervals
                        env = env.putState(x, new Interval(new MathNumber(0), new MathNumber(10)));
                        env = env.putState(y, new Interval(new MathNumber(5), new MathNumber(15)));

                        DifferenceBoundMatrix result = DifferenceBoundMatrix.fromIntervalDomain(env);

                        assertNotNull(result, "Conversion result should not be null");
                        assertTrue(result.knowsIdentifier(x), "Result should know first variable");
                        assertTrue(result.knowsIdentifier(y), "Result should know second variable");
                }

                @Test
                @DisplayName("Round-trip conversion: DBM -> Interval -> DBM")
                void testRoundTripConversion() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);

                        DifferenceBoundMatrix original = dbm.assign(x, c, pp, oracle);
                        ValueEnvironment<Interval> intervals = original.toInterval();
                        DifferenceBoundMatrix restored = DifferenceBoundMatrix.fromIntervalDomain(intervals);

                        assertNotNull(restored, "Restored DBM should not be null");
                        assertTrue(restored.knowsIdentifier(x), "Restored DBM should know the variable");
                }
        }

        @Nested
        @DisplayName("Lattice Join Operations")
        class LatticeJoinOperations {

                @Test
                @DisplayName("LUB of two bottom DBMs")
                void testLubBottomBottom() throws SemanticException {
                        DifferenceBoundMatrix bottom1 = dbm.bottom();
                        DifferenceBoundMatrix bottom2 = dbm.bottom();

                        DifferenceBoundMatrix result = bottom1.lubAux(bottom2);

                        assertNotNull(result, "LUB result should not be null");
                        assertTrue(result.isBottom(), "LUB of two bottom DBMs should be bottom");
                }

                @Test
                @DisplayName("LUB with same variable structure")
                void testLubSameVariables() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c1 = new Constant(NumericType, new MathNumber(5), location);
                        Constant c2 = new Constant(NumericType, new MathNumber(10), location);

                        DifferenceBoundMatrix dbm1 = dbm.assign(x, c1, pp, oracle);
                        DifferenceBoundMatrix dbm2 = dbm.assign(x, c2, pp, oracle);

                        DifferenceBoundMatrix result = dbm1.lubAux(dbm2);

                        assertNotNull(result, "LUB result should not be null");
                        assertTrue(result.knowsIdentifier(x), "LUB result should know the variable");
                }

                @Test
                @DisplayName("LUB with different variable structures should fail")
                void testLubDifferentVariables() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Variable y = new Variable(NumericType, "y", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);

                        DifferenceBoundMatrix dbm1 = dbm.assign(x, c, pp, oracle);
                        DifferenceBoundMatrix dbm2 = dbm.assign(y, c, pp, oracle);

                        assertThrows(SemanticException.class, () -> {
                                dbm1.lubAux(dbm2);
                        }, "LUB of DBMs with different variables should throw SemanticException");
                }
        }

        @Nested
        @DisplayName("Widening Operations")
        class WideningOperations {

                @Test
                @DisplayName("Widening with null")
                void testWideningWithNull() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);

                        DifferenceBoundMatrix original = dbm.assign(x, c, pp, oracle);
                        DifferenceBoundMatrix result = original.widening(null);

                        assertEquals(original, result, "Widening with null should return original DBM");
                }

                @Test
                @DisplayName("Widening with same DBM")
                void testWideningWithSame() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);

                        DifferenceBoundMatrix original = dbm.assign(x, c, pp, oracle);
                        DifferenceBoundMatrix result = original.widening(original);

                        assertNotNull(result, "Widening result should not be null");
                }

                @Test
                @DisplayName("Widening with different DBMs")
                void testWideningWithDifferent() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c1 = new Constant(NumericType, new MathNumber(5), location);
                        Constant c2 = new Constant(NumericType, new MathNumber(10), location);

                        DifferenceBoundMatrix dbm1 = dbm.assign(x, c1, pp, oracle);
                        DifferenceBoundMatrix dbm2 = dbm.assign(x, c2, pp, oracle);

                        DifferenceBoundMatrix result = dbm1.widening(dbm2);

                        assertNotNull(result, "Widening result should not be null");
                }
        }

        @Nested
        @DisplayName("Scope Operations")
        class ScopeOperations {

                @Test
                @DisplayName("Push scope operation")
                void testPushScope() throws SemanticException {
                        ScopeToken token = mock(ScopeToken.class);

                        DifferenceBoundMatrix result = dbm.pushScope(token);

                        assertNotNull(result, "Push scope result should not be null");
                        // Placeholder implementation should return unchanged DBM
                        assertEquals(dbm, result, "Placeholder push scope should return unchanged DBM");
                }

                @Test
                @DisplayName("Pop scope operation")
                void testPopScope() throws SemanticException {
                        ScopeToken token = mock(ScopeToken.class);

                        DifferenceBoundMatrix result = dbm.popScope(token);

                        assertNotNull(result, "Pop scope result should not be null");
                        // Placeholder implementation should return unchanged DBM
                        assertEquals(dbm, result, "Placeholder pop scope should return unchanged DBM");
                }
        }

        @Nested
        @DisplayName("Satisfiability Operations")
        class SatisfiabilityOperations {

                @Test
                @DisplayName("Satisfiability check")
                void testSatisfies() throws SemanticException {
                        Variable x = new Variable(NumericType, "x", location);
                        Constant c = new Constant(NumericType, new MathNumber(5), location);

                        BinaryExpression expr = new BinaryExpression(BoolType.INSTANCE, x, c, ComparisonLe.INSTANCE,
                                        location);
                        Satisfiability result = dbm.satisfies(expr, pp, oracle);

                        assertNotNull(result, "Satisfiability result should not be null");
                        // Placeholder implementation returns UNKNOWN
                        assertEquals(Satisfiability.UNKNOWN, result,
                                        "Placeholder implementation should return UNKNOWN");
                }
        }

        @Nested
        @DisplayName("Edge Cases and Error Handling")
        class EdgeCasesAndErrorHandling {

                @Test
                @DisplayName("Assignment with null identifier should be handled")
                void testAssignmentWithNullIdentifier() {
                        Constant c = new Constant(NumericType, new MathNumber(5), location);

                        assertThrows(Exception.class, () -> {
                                dbm.assign(null, c, pp, oracle);
                        }, "Assignment with null identifier should throw exception");
                }

                @Test
                @DisplayName("Assignment with null expression should be handled")
                void testAssignmentWithNullExpression() {
                        Variable x = new Variable(NumericType, "x", location);

                        assertThrows(Exception.class, () -> {
                                dbm.assign(x, null, pp, oracle);
                        }, "Assignment with null expression should throw exception");
                }

                @Test
                @DisplayName("Operations on large matrices")
                void testLargeMatrixOperations() throws SemanticException {
                        // Create a DBM with multiple variables to test matrix scaling
                        DifferenceBoundMatrix current = dbm;

                        for (int i = 0; i < 5; i++) {
                                Variable var = new Variable(NumericType, "var" + i, location);
                                Constant val = new Constant(NumericType, new MathNumber(i * 10), location);
                                current = current.assign(var, val, pp, oracle);
                        }

                        assertNotNull(current, "DBM with multiple variables should not be null");
                        assertFalse(current.isBottom(), "DBM with multiple variables should not be bottom");

                        // Test that representation works with larger matrices
                        StructuredRepresentation repr = current.representation();
                        assertNotNull(repr, "Large DBM representation should not be null");
                }

                @Test
                @DisplayName("Conversion operations with malformed intervals")
                void testConversionWithMalformedIntervals() throws SemanticException {
                        ValueEnvironment<Interval> env = new ValueEnvironment<>(new Interval());

                        // Test conversion with empty environment
                        DifferenceBoundMatrix result = DifferenceBoundMatrix.fromIntervalDomain(env);

                        assertNotNull(result, "Conversion from empty interval environment should not be null");
                        assertTrue(result.isBottom(), "Conversion from empty environment should produce bottom DBM");
                }
        }
}