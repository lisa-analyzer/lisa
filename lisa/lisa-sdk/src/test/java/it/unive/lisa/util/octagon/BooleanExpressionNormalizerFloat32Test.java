package it.unive.lisa.util.octagon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Float32Type;
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
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.MathNumber;

/**
 * Float32 variants of BooleanExpressionNormalizer tests.
 * Expectations are adapted for non-integer semantics (no +1 offsets, use <
 * where applicable).
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso </a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *         Shytermeja</a>
 */
public class BooleanExpressionNormalizerFloat32Test {

    private CodeLocation location;
    private Type Num = Float32Type.INSTANCE;

    private Variable x;
    private Variable y;
    private Variable z;

    @BeforeEach
    void setup() {
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

    // ------------ Tests ------------

    @Nested
    @DisplayName("Casi con costanti (Float32)")
    class ConstantCases {
        @Test
        @DisplayName("x < 5 (float) => (x - 5) <= 0") // case of relaxation
        void ltVarConstFloat() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, c(5, Num, location), ComparisonLt.INSTANCE,
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
        @DisplayName("x <= 5 (float) => (x - 5) <= 0")
        void leVarConstFloat() throws SemanticException {
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
        @DisplayName("x > 5 (float) => (-x + 5) <= 0") // case of relaxation
        void gtVarConstFloat() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, c(5, Num, location), ComparisonGt.INSTANCE,
                    location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            // Implementation returns <= here for variable vs constant
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

        @Test
        @DisplayName("x >= 5 (float) => (-x + 5) <= 0")
        void geVarConstFloat() throws SemanticException {
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
    @DisplayName("Confronti: >, >=, <, <=, ==, != (Float32)")
    class ComparisonNormalizationFloat {

        @Test
        @DisplayName("x > y (float) => (y - x) <= 0") // case of relaxation
        void gtIdentifiersFloat() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonGt.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, y, x);
        }

        @Test
        @DisplayName("x >= y (float) => (y - x) <= 0")
        void geIdentifiersFloat() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonGe.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, y, x);
        }

        @Test
        @DisplayName("x < y (float) => (x - y) <= 0") // case of relaxation
        void ltIdentifiersFloat() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLt.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, x, y);
        }

        @Test
        @DisplayName("x <= y (float) => (x - y) <= 0")
        void leIdentifiersFloat() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, x, y);
        }

        @Test
        @DisplayName("x == y (float) => (x - y <= 0) AND (y - x <= 0)")
        void eqNormalizationFloat() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonEq.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);

            BinaryExpression and = asBin(res);
            assertTrue(and.getOperator() instanceof LogicalAnd);

            BinaryExpression le1 = asBin(and.getLeft());
            assertOp(le1, ComparisonLe.class);
            assertTrue(isZeroConst(le1.getRight()));
            assertIsSubXY(asBin(le1.getLeft()), x, y);

            BinaryExpression le2 = asBin(and.getRight());
            assertOp(le2, ComparisonLe.class);
            assertTrue(isZeroConst(le2.getRight()));
            assertIsSubXY(asBin(le2.getLeft()), y, x);
        }

        @Test
        @DisplayName("x != y (float) => (x - y) != 0")
        void neNormalizationFloat() throws SemanticException {
            BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonNe.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);

            BinaryExpression cmp = asBin(res);
            assertOp(cmp, ComparisonNe.class);
            assertTrue(isZeroConst(cmp.getRight()));
            BinaryExpression diff = asBin(cmp.getLeft());
            assertIsSubXY(diff, x, y);
        }

        @Test
        @DisplayName("x + y > 0 (float) => -x - y <= 0") // case of relaxation
        void addZeroNormalizationFloat() throws SemanticException {
            BinaryExpression sum = new BinaryExpression(Num, x, y, NumericNonOverflowingAdd.INSTANCE, location);
            BinaryExpression gt = new BinaryExpression(BoolType.INSTANCE, sum, c(0, Num, location),
                    ComparisonGt.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(gt, location);

            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression left = asBin(top.getLeft());
            assertIsAdd(left);

            UnaryExpression u1 = asUnary(left.getLeft());
            UnaryExpression u2 = asUnary(left.getRight());
            assertTrue(u1.getOperator() instanceof NumericNegation);
            assertTrue(u2.getOperator() instanceof NumericNegation);

            SymbolicExpression e1 = u1.getExpression();
            SymbolicExpression e2 = u2.getExpression();
            assertTrue((e1 == x && e2 == y) || (e1 == y && e2 == x),
                    "Expected negations of x and y, got: " + e1 + ", " + e2);

        }

        @Test
        @DisplayName("x - y >= 0 (float) => (y - x) <= 0")
        void subZeroNormalizationFloat() throws SemanticException {
            BinaryExpression sub = new BinaryExpression(Num, x, y, NumericNonOverflowingSub.INSTANCE, location);
            BinaryExpression ge = new BinaryExpression(BoolType.INSTANCE, sub, c(0, Num, location),
                    ComparisonGe.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(ge, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, y, x);
        }

        @Test
        @DisplayName("-x + y >= 0 (float) => (x - y) <= 0")
        void negAddZeroNormalizationFloat() throws SemanticException {
            UnaryExpression negX = new UnaryExpression(Num, x, NumericNegation.INSTANCE, location);
            BinaryExpression sum = new BinaryExpression(Num, negX, y, NumericNonOverflowingAdd.INSTANCE, location);
            BinaryExpression ge = new BinaryExpression(BoolType.INSTANCE, sum, c(0, Num, location),
                    ComparisonGe.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(ge, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression diff = asBin(top.getLeft());
            assertIsSubXY(diff, x, y);
        }

        @Test
        @DisplayName("-x - y >= 0 (float) => (x + y) <= 0")
        void negSubZeroNormalizationFloat() throws SemanticException {
            UnaryExpression negX = new UnaryExpression(Num, x, NumericNegation.INSTANCE, location);
            BinaryExpression sub = new BinaryExpression(Num, negX, y, NumericNonOverflowingSub.INSTANCE, location);
            BinaryExpression ge = new BinaryExpression(BoolType.INSTANCE, sub, c(0, Num, location),
                    ComparisonGe.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(ge, location);
            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));
            BinaryExpression sum = asBin(top.getLeft());
            assertIsAdd(sum);
            assertSame(x, sum.getLeft());
            assertSame(y, sum.getRight());
        }

        @Test
        @DisplayName("x - y + 1 <= 0 does not throw (float)")
        void subAddConstLeZeroDoesNotThrowFloat() throws SemanticException {
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
    @DisplayName("Negazioni e De Morgan (Float32)")
    class NegationsFloat {

        @Test
        @DisplayName("not(x >= 0) => x <= 0")
        // not(x >= 0) is equivalent to x < 0, but in float case we need to relax to x
        void notGeZeroFloat() throws SemanticException {
            // Build not(x >= 0)
            BinaryExpression ge = new BinaryExpression(BoolType.INSTANCE, x, c(0, Num, location),
                    ComparisonGe.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, ge, LogicalNegation.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            // Expect: x <= 0
            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));
            assertSame(x, le.getLeft());
        }

        @Test
        @DisplayName("(! x < 0) => -x <= 0")
        void notLtZeroFloat() throws SemanticException {
            // Build not(x < 0)
            BinaryExpression lt = new BinaryExpression(BoolType.INSTANCE, x, c(0, Num, location),
                    ComparisonLt.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, lt, LogicalNegation.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            // Expect: (-x) <= 0
            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));

            UnaryExpression neg = asUnary(le.getLeft());
            assertTrue(neg.getOperator() instanceof NumericNegation);
            assertSame(x, neg.getExpression());
        }

        @Test
        @DisplayName("not(x >= 1) (float) => x <= 1")
        void notGeOneFloat() throws SemanticException {
            // Build not(x >= 1)
            BinaryExpression ge = new BinaryExpression(BoolType.INSTANCE, x, c(1, Num, location),
                    ComparisonGe.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, ge, LogicalNegation.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            // Expect: (x - 1) <= 0
            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));

            BinaryExpression diff = asBin(le.getLeft());
            assertTrue(diff.getOperator() instanceof NumericNonOverflowingSub);
            assertSame(x, diff.getLeft());
            assertEquals(new MathNumber(1), ((Constant) diff.getRight()).getValue());
        }

        @Test
        @DisplayName("not(x <= y) (float) => (y - x) <= 0")
        void notLeFloat() throws SemanticException {
            BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));
            BinaryExpression diff = asBin(le.getLeft());
            assertIsSubXY(diff, y, x);
        }

        @Test
        @DisplayName("not(x <= c) (float) => (-x + c) <= 0")
        void notLeConstFloat() throws SemanticException {
            BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, c(5, Num, location),
                    ComparisonLe.INSTANCE,
                    location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));
            BinaryExpression sum = asBin(le.getLeft());
            assertIsAdd(sum);
            UnaryExpression neg = asUnary(sum.getLeft());
            assertTrue(neg.getOperator() instanceof NumericNegation);
            assertSame(x, neg.getExpression());
            Constant cst = (Constant) sum.getRight();
            assertEquals(new MathNumber(5), cst.getValue());
        }

        @Test
        @DisplayName("not(x < y) (float) => (y - x) <= 0")
        void notLtFloat() throws SemanticException {
            BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLt.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));
            BinaryExpression diff = asBin(le.getLeft());
            assertIsSubXY(diff, y, x);
        }

        @Test
        @DisplayName("not((x <= y) AND (y <= z)) (float) => not(x <= y) OR not(y <= z)")
        void deMorganAndFloat() throws SemanticException {
            BinaryExpression a = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
            BinaryExpression b = new BinaryExpression(BoolType.INSTANCE, y, z, ComparisonLe.INSTANCE, location);
            BinaryExpression and = new BinaryExpression(BoolType.INSTANCE, a, b, LogicalAnd.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, and, LogicalNegation.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression top = asBin(res);
            assertTrue(top.getOperator() instanceof LogicalOr);

            BinaryExpression l = asBin(top.getLeft());
            assertOp(l, ComparisonLe.class);
            assertTrue(isZeroConst(l.getRight()));

            BinaryExpression r = asBin(top.getRight());
            assertOp(r, ComparisonLe.class);
            assertTrue(isZeroConst(r.getRight()));
        }

        @Test
        @DisplayName("not(x == y) (float) => (x - y) != 0")
        void notEqBecomesNeFloat() throws SemanticException {
            BinaryExpression eq = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonEq.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, eq, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression cmp = asBin(res);
            assertOp(cmp, ComparisonNe.class);
        }

        @Test
        @DisplayName("not(x != y) (float) => x == y (forma normalizzata)")
        void notNeBecomesEqFloat() throws SemanticException {
            BinaryExpression ne = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonNe.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, ne, LogicalNegation.INSTANCE, location);
            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            BinaryExpression and = asBin(res);
            assertTrue(and.getOperator() instanceof LogicalAnd);
        }
    }

    @Nested
    @DisplayName("Operatori logici senza negazione (Float32)")
    class LogicalOpsFloat {
        @Test
        @DisplayName("(x <= y) OR (y <= z) remains OR with normalized operands")
        void orRemainsOrFloat() throws SemanticException {
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
    @DisplayName("Already normalized cases (Float32)")
    class AlreadyNormalizedFloat {
        @Test
        @DisplayName("(x - 5) <= 0 remains unchanged shape")
        void subConstLeZeroRemainsFloat() throws SemanticException {
            BinaryExpression sub = new BinaryExpression(Num, x, c(5, Num, location), NumericNonOverflowingSub.INSTANCE,
                    location);
            BinaryExpression le = new BinaryExpression(BoolType.INSTANCE, sub, c(0, Num, location),
                    ComparisonLe.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(le, location);

            BinaryExpression top = asBin(res);
            assertOp(top, ComparisonLe.class);
            assertTrue(isZeroConst(top.getRight()));

            BinaryExpression diff = asBin(top.getLeft());
            assertTrue(diff.getOperator() instanceof NumericNonOverflowingSub);
            assertSame(x, diff.getLeft());
            Constant cst = (Constant) diff.getRight();
            assertEquals(new MathNumber(5), cst.getValue());
        }

        @Test
        @DisplayName("not(x + 100 < 0) (float) => -x -100 <= 0")
        void notAdd100LtZeroFloat() throws SemanticException {
            // Build not((x + 100) < 0)
            BinaryExpression add = new BinaryExpression(Num, x, c(100, Num, location),
                    NumericNonOverflowingAdd.INSTANCE, location);
            BinaryExpression lt = new BinaryExpression(BoolType.INSTANCE, add, c(0, Num, location),
                    ComparisonLt.INSTANCE, location);
            UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, lt, LogicalNegation.INSTANCE, location);

            SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

            // Expect: -x - 100 <= 0
            BinaryExpression le = asBin(res);
            assertOp(le, ComparisonLe.class);
            assertTrue(isZeroConst(le.getRight()));
            BinaryExpression sub = asBin(le.getLeft());
            assertIsSub(sub);
            UnaryExpression neg = asUnary(sub.getLeft());
            assertTrue(neg.getOperator() instanceof NumericNegation);
            assertSame(x, neg.getExpression());
            Constant cst = (Constant) sub.getRight();
            assertEquals(new MathNumber(100), cst.getValue());

        }
    }
}
