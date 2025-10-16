package it.unive.lisa.util.octagon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.InMemoryType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.MathNumber;

/**
 * Test di unità per {@link BooleanExpressionNormalizer}.
 * Copre normalizzazione di operatori logici, confronti, negazioni e casi
 * specifici per tipi interi.
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso </a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *         Shytermeja</a>
 */
public class BooleanExpressionNormalizerTest {

    private CodeLocation location;
    private Type Num = InMemoryType.INSTANCE;

    private Variable x;
    private Variable y;
    private Variable z;

    @BeforeEach
    void setup() {
        // Usa un mock di CodeLocation come negli altri test del progetto
        location = mock(CodeLocation.class);
        x = new Variable(Num, "x", location);
        y = new Variable(Num, "y", location);
        z = new Variable(Num, "z", location);
    }

    // ------------ Helpers ------------

    private static Constant c(int v, Type t, CodeLocation loc) {
        return new Constant(t, new MathNumber(v), loc);
    }

    private static boolean isZeroConst(SymbolicExpression e) {
        if (!(e instanceof Constant))
            return false;
        Object v = ((Constant) e).getValue();
        if (v instanceof MathNumber)
            return ((MathNumber) v).isZero();
        if (v instanceof Integer)
            return ((Integer) v) == 0;
        if (v instanceof Long)
            return ((Long) v) == 0L;
        return false;

    }

    private static BinaryExpression asBin(SymbolicExpression e) {
        assertTrue(e instanceof BinaryExpression, "Expected BinaryExpression, got: " + e);
        return (BinaryExpression) e;
    }

    private static UnaryExpression asUnary(SymbolicExpression e) {
        assertTrue(e instanceof UnaryExpression, "Expected UnaryExpression, got: " + e);
        return (UnaryExpression) e;
    }

    private static void assertOp(BinaryExpression be, Class<? extends BinaryOperator> opClass) {
        assertTrue(opClass.isInstance(be.getOperator()),
                "Unexpected operator: " + be.getOperator() + ", expected: " + opClass.getSimpleName());
    }

    private static void assertIsSubXY(BinaryExpression e, Variable left, Variable right) {
        assertTrue(e.getOperator() instanceof NumericNonOverflowingSub,
                "Expected subtraction, got: " + e.getOperator());
        assertSame(left, e.getLeft());
        assertSame(right, e.getRight());
    }

    private static void assertIsAdd(BinaryExpression e) {
        assertTrue(e.getOperator() instanceof NumericNonOverflowingAdd,
                "Expected addition, got: " + e.getOperator());
    }

    private static void assertIsSub(BinaryExpression e) {
        assertTrue(e.getOperator() instanceof NumericNonOverflowingSub,
                "Expected subtraction, got: " + e.getOperator());
    }

    private static void assertIsNegXPlusCLeZero(SymbolicExpression res, Variable var, int cVal, Type t,
            CodeLocation loc) {
        // Expect: (-var + c) <= 0
        BinaryExpression top = asBin(res);
        assertOp(top, ComparisonLe.class);
        assertTrue(isZeroConst(top.getRight()), "Right side should be 0: " + top.getRight());

        BinaryExpression sum = asBin(top.getLeft());
        assertIsAdd(sum);
        UnaryExpression neg = asUnary(sum.getLeft());
        assertTrue(neg.getOperator() instanceof NumericNegation);
        // Lo standard atteso è la negazione della variabile;
        // accettiamo anche la negazione di un'espressione composta (es. x+y),
        // purché la forma complessiva sia (-e + c) <= 0.
        if (neg.getExpression() instanceof Variable)
            assertSame(var, neg.getExpression());

        Constant cst = (Constant) sum.getRight();
        assertEquals(new MathNumber(cVal), cst.getValue());
    }

    // ------------ Tests ------------

    @Nested
    @DisplayName("Casi con costanti")
    class ConstantCases {
        @Test
        @DisplayName("x < 5 (interi) => (x - 6) <= 0")
        void ltVarConstInteger() throws SemanticException { // CORRECT
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, c(5, Num, location), ComparisonLt.INSTANCE,
                    location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertTrue(diff.getOperator() instanceof NumericNonOverflowingSub);
            assertSame(x, diff.getLeft());
            assertEquals(new MathNumber(6), ((Constant) diff.getRight()).getValue());
        }

        @Test
        @DisplayName("x <= 5 => (x - 5) <= 0")
        void leVarConst() throws SemanticException { // CORRECT
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, c(5, Num, location), ComparisonLe.INSTANCE,
                    location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);

            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertTrue(diff.getOperator() instanceof NumericNonOverflowingSub);
            assertSame(x, diff.getLeft());
            assertEquals(new MathNumber(5), ((Constant) diff.getRight()).getValue());
        }

        @Test
        @DisplayName("x > 5 (interi) => (-x + 6) <= 0")
        void gtVarConstInteger() throws SemanticException { // CORRECT
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, c(5, Num, location), ComparisonGt.INSTANCE,
                    location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression sum = asBin(top.getLeft());
            assertIsAdd(sum);
            UnaryExpression neg = asUnary(sum.getLeft());
            assertTrue(neg.getOperator() instanceof NumericNegation);
            assertSame(x, neg.getExpression());
            Constant cst = (Constant) sum.getRight();
            assertEquals(new MathNumber(6), cst.getValue());
        }

        @Test
        @DisplayName("x >= 5 => (-x + 5) <= 0")
        void geVarConstInteger() throws SemanticException { // CORRECT
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, c(5, Num, location), ComparisonGe.INSTANCE,
                    location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression sum = asBin(top.getLeft());
            assertIsAdd(sum);
            UnaryExpression neg = asUnary(sum.getLeft());
            assertTrue(neg.getOperator() instanceof NumericNegation);
            assertSame(x, neg.getExpression());
            Constant cst = (Constant) sum.getRight();
            assertEquals(new MathNumber(5), cst.getValue());
        }

    }

    @Nested
    @DisplayName("Confronti: >, >=, <, <=, ==, !=")
    class ComparisonNormalization {

        @Test
        @DisplayName("x > y (integer) => (y - x - 1) <= 0")
        void gtIdentifiers() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonGt.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression sum = asBin(top.getLeft());
            assertIsAdd(sum);
            assertIsSubXY(asBin(sum.getLeft()), y, x);
            assertEquals(new MathNumber(1), ((Constant) sum.getRight()).getValue());

        }

        @Test
        @DisplayName("x >= y => (y - x) <= 0") // CORRECT
        void geIdentifiers() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonGe.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, y, x);
        }

        @Test
        @DisplayName("x < y (interi) => (x - y + 1) <= 0") // CORRECT
        void ltIdentifiers() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLt.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression sum = asBin(top.getLeft());
            assertIsAdd(sum);
            assertIsSubXY(asBin(sum.getLeft()), x, y);
            assertEquals(new MathNumber(1), ((Constant) sum.getRight()).getValue());
        }

        @Test
        @DisplayName("x <= y => (x - y) <= 0") // CORRECT
        void leIdentifiers() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, x, y);
        }

        @Test
        @DisplayName("x == y => (x - y <= 0) AND (y - x <= 0)")
        void eqNormalization() throws SemanticException { // CORRECT
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonEq.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);

            BinaryExpression and = asBin(res);
            assertTrue(and.getOperator() instanceof LogicalAnd);

            // left: x - y <= 0
            BinaryExpression le1 = asBin(and.getLeft());
            assertOp(le1, ComparisonLe.class);
            assertTrue(isZeroConst(le1.getRight()));
            assertIsSubXY(asBin(le1.getLeft()), x, y);

            // right: y - x <= 0
            BinaryExpression le2 = asBin(and.getRight());
            assertOp(le2, ComparisonLe.class);
            assertTrue(isZeroConst(le2.getRight()));
            assertIsSubXY(asBin(le2.getLeft()), y, x);
        }

        @Test
        @DisplayName("x != y (interi) => (x - y + 1 <= 0) OR (y - x + 1 <= 0)")
        void neNormalizationInteger() throws SemanticException { // CORRECT
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonNe.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);

            BinaryExpression or = asBin(res);
            assertTrue(or.getOperator() instanceof LogicalOr);

            // first: (x - y + 1) <= 0
            BinaryExpression le1 = asBin(or.getLeft());
            assertOp(le1, ComparisonLe.class);
            assertTrue(isZeroConst(le1.getRight()));
            BinaryExpression sum1 = asBin(le1.getLeft());
            assertIsAdd(sum1);
            assertIsSubXY(asBin(sum1.getLeft()), x, y);
            assertEquals(new MathNumber(1), ((Constant) sum1.getRight()).getValue());

            // second: (y - x + 1) <= 0
            BinaryExpression le2 = asBin(or.getRight());
            assertOp(le2, ComparisonLe.class);
            assertTrue(isZeroConst(le2.getRight()));
            BinaryExpression sum2 = asBin(le2.getLeft());
            assertIsAdd(sum2);
            assertIsSubXY(asBin(sum2.getLeft()), y, x);
            assertEquals(new MathNumber(1), ((Constant) sum2.getRight()).getValue());
        }

        @Test
        @DisplayName("x + y > 0 (interi) => (-x - y + 1) <= 0")
        void addZeroNormalization() throws SemanticException { // ERROR
            // fails with:
            // it.unive.lisa.analysis.SemanticException: ComparisonGt not handled for
            // non-Identifier operands
            BinaryExpression sum = new BinaryExpression(Num, x, y, NumericNonOverflowingAdd.INSTANCE, location);
            BinaryExpression gt = new BinaryExpression(BoolType.INSTANCE, sum, c(0, Num, location),
                    ComparisonGt.INSTANCE,
                    location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(gt, location);

            // Expect: (-x - y + 1) <= 0
            assertIsNegXPlusCLeZero(res, x, 1, Num, location);
        }

        @Test
        @DisplayName("x - y >= 0 => (y - x) <= 0")
        void subZeroNormalization() throws SemanticException { // ERROR
            BinaryExpression sub = new BinaryExpression(Num, x, y, NumericNonOverflowingSub.INSTANCE, location);
            BinaryExpression ge = new BinaryExpression(BoolType.INSTANCE, sub, c(0, Num, location),
                    ComparisonGe.INSTANCE,
                    location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(ge, location);
            // It returns 0 - x - y <= 0 but we want y - x <= 0
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, y, x);
        }

        @Test
        @DisplayName("-x + y >= 0 => (x - y) <= 0")
        void negAddZeroNormalization() throws SemanticException { // ERROR
            UnaryExpression negX = new UnaryExpression(Num, x, NumericNegation.INSTANCE, location);
            BinaryExpression sum = new BinaryExpression(Num, negX, y, NumericNonOverflowingAdd.INSTANCE, location);
            BinaryExpression ge = new BinaryExpression(BoolType.INSTANCE, sum, c(0, Num, location),
                    ComparisonGe.INSTANCE,
                    location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(ge, location);
            // It returns 0 - - x + y <= 0 but we want x - y <= 0
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, x, y);
        }

        @Test
        @DisplayName("-x - y >= 0 => (x + y) <= 0")
        void negSubZeroNormalization() throws SemanticException { // ERROR
            UnaryExpression negX = new UnaryExpression(Num, x, NumericNegation.INSTANCE, location);
            BinaryExpression sub = new BinaryExpression(Num, negX, y, NumericNonOverflowingSub.INSTANCE, location);
            BinaryExpression ge = new BinaryExpression(BoolType.INSTANCE, sub, c(0, Num, location),
                    ComparisonGe.INSTANCE,
                    location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(ge, location);
            // It returns 0 - - x - y <= 0 but we want x + y <= 0
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression sum = asBin(top.getLeft());
            assertIsAdd(sum);
            assertSame(x, sum.getLeft());
            assertSame(y, sum.getRight());
        }

        @Test
        @DisplayName("x - y + 1 <= 0 does not throw (integer)")
        void subAddConstLeZeroDoesNotThrow() throws SemanticException {
            // Build (x - y) + 1 <= 0
            BinaryExpression sub = new BinaryExpression(Num, x, y, NumericNonOverflowingSub.INSTANCE, location);
            BinaryExpression add = new BinaryExpression(Num, sub, c(1, Num, location),
                    NumericNonOverflowingAdd.INSTANCE, location);
            BinaryExpression le = new BinaryExpression(BoolType.INSTANCE, add, c(0, Num, location),
                    ComparisonLe.INSTANCE, location);

            // This should normalize without throwing
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(le, location);

            // Basic sanity checks on result shape
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
        }

    }

    @Nested
    @DisplayName("Negazioni e De Morgan")
    class Negations {
        @Test
        @DisplayName("not(x >= 0) (interi) => x + 1 <= 0")
        void notGeZeroInt() throws SemanticException {
            // Build not(x >= 0)
            BinaryExpression ge = new BinaryExpression(BoolType.INSTANCE, x, c(0, Num, location),
                    ComparisonGe.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, ge, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);
            // Expect: (x + 1) <= 0
            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight())); // Right side should be 0
            BinaryExpression sum = asBin(le.getLeft());
            assertTrue(sum.getOperator() instanceof NumericNonOverflowingAdd);
            assertSame(x, sum.getLeft());
            Constant cst = (Constant) sum.getRight();
            assertEquals(new MathNumber(1), cst.getValue());
        }

        @Test
        @DisplayName("not(x >= 1) (interi) => x <= 0")
        void notGeOne() throws SemanticException {
            BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, c(1, Num, location),
                    ComparisonGe.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            // Expect: x <= 0
            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));
            assertSame(x, le.getLeft());
        }

        @Test
        @DisplayName("(! x < 0) (interi) => -x <= 0")
        void notLtZeroInt() throws SemanticException {
            // Build not(x < 0)
            BinaryExpression lt = new BinaryExpression(BoolType.INSTANCE, x, c(0, Num, location), ComparisonLt.INSTANCE,
                    location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, lt, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);
            // Expect: -x <= 0
            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight())); // Right side should be 0
            UnaryExpression neg = asUnary(le.getLeft());
            assertTrue(neg.getOperator() instanceof NumericNegation);
            assertSame(x, neg.getExpression());
        }

        @Test
        @DisplayName("not(x >= 1) (interi) => x <= 0")
        void notGeOneInt() throws SemanticException {
            BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, c(1, Num, location),
                    ComparisonGe.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            // Expect: x <= 0
            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));
            assertSame(x, le.getLeft());
        }

        @Test
        @DisplayName("not(x <= y) (interi) => (y - x + 1) <= 0")
        void notLe() throws SemanticException {
            BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));
            BinaryExpression sum = asBin(le.getLeft());
            assertIsAdd(sum);
            assertIsSubXY(asBin(sum.getLeft()), y, x);
            assertEquals(new MathNumber(1), ((Constant) sum.getRight()).getValue());
        }

        @Test
        @DisplayName("not(x <= c) (interi) => (-x + (c + 1)) <= 0")
        void notLeConst() throws SemanticException {
            int cVal = 5;
            BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, c(cVal, Num, location),
                    ComparisonLe.INSTANCE,
                    location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);
            assertIsNegXPlusCLeZero(res, x, cVal + 1, Num, location);
        }

        @Test
        @DisplayName("not(x < y) => (y - x) <= 0")
        void notLt() throws SemanticException {
            BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLt.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));
            assertIsSubXY(asBin(le.getLeft()), y, x);
        }

        @Test
        @DisplayName("(x + 100) < 0 (interi) => x + 101 <= 0")
        void addConstLtZeroInt() throws SemanticException {
            // Build (x + 100) < 0
            BinaryExpression add = new BinaryExpression(Num, x, c(100, Num, location),
                    NumericNonOverflowingAdd.INSTANCE, location);
            BinaryExpression lt = new BinaryExpression(BoolType.INSTANCE, add, c(0, Num, location),
                    ComparisonLt.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(lt, location);

            // Expect: x + 101 <= 0
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight())); // Right side should be 0
            BinaryExpression sum = asBin(top.getLeft());
            assertTrue(sum.getOperator() instanceof NumericNonOverflowingAdd);
            assertSame(x, sum.getLeft());
            Constant cst = (Constant) sum.getRight();
            assertEquals(new MathNumber(101), cst.getValue());
        }

        @Test
        @DisplayName("not(x + 100 < 0) (interi) => -x - 99 <= 0")
        void notAdd100LtZeroInt() throws SemanticException {
            // not((x + 100) < 0) -> -x -100 < 0 -> -x - 99 <= 0
            BinaryExpression add = new BinaryExpression(Num, x, c(100, Num, location),
                    NumericNonOverflowingAdd.INSTANCE, location);
            BinaryExpression lt = new BinaryExpression(BoolType.INSTANCE, add, c(0, Num, location),
                    ComparisonLt.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, lt, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            // Expect: -x - 99 <= 0
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight())); // Right side should be 0
            BinaryExpression sum = asBin(top.getLeft());
            assertIsSub(sum);
            UnaryExpression neg = asUnary(sum.getLeft());
            assertTrue(neg.getOperator() instanceof NumericNegation);
            assertSame(x, neg.getExpression());
            Constant cst = (Constant) sum.getRight();
            assertEquals(new MathNumber(99), cst.getValue());

        }

        @Test
        @DisplayName("not((x <= y) AND (y <= z)) => not(x <= y) OR not(y <= z)")
        void deMorganAnd() throws SemanticException {
            BinaryExpression a = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
            BinaryExpression b = new BinaryExpression(BoolType.INSTANCE, y, z, ComparisonLe.INSTANCE, location);
            BinaryExpression and = new BinaryExpression(BoolType.INSTANCE, a, b, LogicalAnd.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, and, LogicalNegation.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression top = asBin(res);
            assertTrue(top.getOperator() instanceof LogicalOr);
            // Left branch is normalized negation of (x <= y)
            BinaryExpression le1 = asBin(top.getLeft());
            assertOp(le1, ComparisonLe.class);
            // Accept either (y - x + 1) <= 0 (interi) o una forma equivalente
            assertTrue(isZeroConst(le1.getRight()));

            // Right branch is normalized negation of (y <= z)
            BinaryExpression le2 = asBin(top.getRight());
            assertOp(le2, ComparisonLe.class);
            assertTrue(isZeroConst(le2.getRight()));
        }

        @Test
        @DisplayName("not(x == y) => x != y (forma normalizzata)")
        void notEqBecomesNe() throws SemanticException {
            BinaryExpression eq = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonEq.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, eq, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            // Dovrebbe diventare la normalizzazione della disuguaglianza per interi
            BinaryExpression or = asBin(res);
            assertTrue(or.getOperator() instanceof LogicalOr);
        }

        @Test
        @DisplayName("not(x != y) => x == y (forma normalizzata)")
        void notNeBecomesEq() throws SemanticException {
            BinaryExpression ne = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonNe.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, ne, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression and = asBin(res);
            assertTrue(and.getOperator() instanceof LogicalAnd);
        }
    }

    @Nested
    @DisplayName("Operatori logici senza negazione")
    class LogicalOps {
        @Test
        @DisplayName("(x <= y) OR (y <= z) rimane OR con operandi normalizzati")
        void orRemainsOr() throws SemanticException {
            BinaryExpression a = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
            BinaryExpression b = new BinaryExpression(BoolType.INSTANCE, y, z, ComparisonLe.INSTANCE, location);
            BinaryExpression or = new BinaryExpression(BoolType.INSTANCE, a, b, LogicalOr.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(or, location);
            BinaryExpression top = asBin(res);
            assertTrue(top.getOperator() instanceof LogicalOr);
            assertTrue(top.getLeft() instanceof BinaryExpression);
            assertTrue(top.getRight() instanceof BinaryExpression);
        }
    }

    @Nested
    @DisplayName("Already normalized cases")
    class AlreadyNormalized {
        @Test
        @DisplayName("(x - 5) <= 0 remains unchanged shape")
        void subConstLeZeroRemains() throws SemanticException {
            // Build (x - 5) <= 0
            BinaryExpression sub = new BinaryExpression(Num, x, c(5, Num, location), NumericNonOverflowingSub.INSTANCE,
                    location);
            BinaryExpression le = new BinaryExpression(BoolType.INSTANCE, sub, c(0, Num, location),
                    ComparisonLe.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(le, location);

            // Check structure: (something <= 0)
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));

            // Left must be (x - 5)
            BinaryExpression diff = asBin(top.getLeft());
            assertTrue(diff.getOperator() instanceof NumericNonOverflowingSub);
            assertSame(x, diff.getLeft());
            Constant cst = (Constant) diff.getRight();
            assertEquals(new MathNumber(5), cst.getValue());
        }

        @Test
        @DisplayName("(-x + 100) <= 0 remains unchanged")
        void negAddConstLeZeroRemains() throws SemanticException {
            // Build (-x + 100) <= 0
            UnaryExpression negX = new UnaryExpression(Num, x, NumericNegation.INSTANCE, location);
            BinaryExpression add = new BinaryExpression(Num, negX, c(100, Num, location),
                    NumericNonOverflowingAdd.INSTANCE,
                    location);
            BinaryExpression le = new BinaryExpression(BoolType.INSTANCE, add, c(0, Num, location),
                    ComparisonLe.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(le, location);

            // Check structure: (something <= 0)
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));

            // Left must be (-x + 100)
            BinaryExpression sum = asBin(top.getLeft());
            assertIsAdd(sum);
            UnaryExpression neg = asUnary(sum.getLeft());
            assertTrue(neg.getOperator() instanceof NumericNegation);
            assertSame(x, neg.getExpression());
            Constant cst = (Constant) sum.getRight();
            assertEquals(new MathNumber(100), cst.getValue());
        }

    }

    // test non comparisons such as x + 10
    @Nested
    @DisplayName("Non-boolean expressions")
    class NonBooleanExpressions {
        @Test
        @DisplayName("x + 10 remains unchanged")
        void addConstRemains() throws SemanticException {
            BinaryExpression add = new BinaryExpression(Num, x, c(10, Num, location),
                    NumericNonOverflowingAdd.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(add, location);
            // Should remain unchanged
            assertTrue(res instanceof BinaryExpression);
            BinaryExpression binRes = (BinaryExpression) res;
            assertTrue(binRes.getOperator() instanceof NumericNonOverflowingAdd);
            assertSame(x, binRes.getLeft());
        }
    }

}
