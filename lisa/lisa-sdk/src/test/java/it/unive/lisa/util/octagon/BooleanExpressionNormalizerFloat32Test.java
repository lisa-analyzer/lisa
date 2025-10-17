package it.unive.lisa.util.octagon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SyntheticLocation;
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

    private static CodeLocation location = SyntheticLocation.INSTANCE;
    private static Type Num = Float32Type.INSTANCE;

    private static Variable x;
    private static Variable y;
    private static Variable z;

    @BeforeClass
    public static void setup() {
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
        assertTrue("Expected BinaryExpression, got: " + e, e instanceof BinaryExpression);
        return (BinaryExpression) e;
    }

    private static UnaryExpression asUnary(SymbolicExpression e) {
        assertTrue("Expected UnaryExpression, got: " + e, e instanceof UnaryExpression);
        return (UnaryExpression) e;
    }

    private static void assertOp(BinaryExpression be, Class<? extends BinaryOperator> opClass) {
        assertTrue("Unexpected operator: " + be.getOperator() + ", expected: " + opClass.getSimpleName(),
                opClass.isInstance(be.getOperator()));
    }

    private static void assertIsSubXY(BinaryExpression e, Variable left, Variable right) {
        assertTrue("Expected subtraction, got: " + e.getOperator(),
                e.getOperator() instanceof NumericNonOverflowingSub);
        assertSame(left, e.getLeft());
        assertSame(right, e.getRight());
    }

    private static void assertIsAdd(BinaryExpression e) {
        assertTrue("Expected addition, got: " + e.getOperator(), e.getOperator() instanceof NumericNonOverflowingAdd);
    }

    private static void assertIsSub(BinaryExpression e) {
        assertTrue("Expected subtraction, got: " + e.getOperator(),
                e.getOperator() instanceof NumericNonOverflowingSub);
    }

    @Test // x < 5 (float) => (x - 5) <= 0 // case of relaxation
    public void ltVarConstFloat() throws SemanticException {
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

    @Test // x <= 5 (float) => (x - 5) <= 0
    public void leVarConstFloat() throws SemanticException {
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

    @Test // x > 5 (float) => (-x + 5) <= 0 // case of relaxation
    public void gtVarConstFloat() throws SemanticException {
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

    @Test // x >= 5 (float) => (-x + 5) <= 0
    public void geVarConstFloat() throws SemanticException {
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

    @Test // x > y (float) => (y - x) <= 0
    public void gtIdentifiersFloat() throws SemanticException {
        BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonGt.INSTANCE, location);
        SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
        BinaryExpression top = asBin(res);
        assertOp(top, ComparisonLe.class);
        assertTrue(isZeroConst(top.getRight()));
        BinaryExpression diff = asBin(top.getLeft());
        assertIsSubXY(diff, y, x);
    }

    @Test // x >= y (float) => (y - x) <= 0
    public void geIdentifiersFloat() throws SemanticException {
        BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonGe.INSTANCE, location);
        SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
        BinaryExpression top = asBin(res);
        assertOp(top, ComparisonLe.class);
        assertTrue(isZeroConst(top.getRight()));
        BinaryExpression diff = asBin(top.getLeft());
        assertIsSubXY(diff, y, x);
    }

    @Test // x < y (float) => (x - y) <= 0 // case of relaxation
    public void ltIdentifiersFloat() throws SemanticException {
        BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLt.INSTANCE, location);
        SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
        BinaryExpression top = asBin(res);
        assertOp(top, ComparisonLe.class);
        assertTrue(isZeroConst(top.getRight()));
        BinaryExpression diff = asBin(top.getLeft());
        assertIsSubXY(diff, x, y);
    }

    @Test // x <= y (float) => (x - y) <= 0
    public void leIdentifiersFloat() throws SemanticException {
        BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
        SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);
        BinaryExpression top = asBin(res);
        assertOp(top, ComparisonLe.class);
        assertTrue(isZeroConst(top.getRight()));
        BinaryExpression diff = asBin(top.getLeft());
        assertIsSubXY(diff, x, y);
    }

    @Test // x == y (float) => (x - y <= 0) AND (y - x <= 0)
    public void eqNormalizationFloat() throws SemanticException {
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

    @Test // x != y (float) => (x - y) != 0
    public void neNormalizationFloat() throws SemanticException {
        BinaryExpression e = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonNe.INSTANCE, location);
        SymbolicExpression res = BooleanExpressionNormalizer.normalize(e, location);

        BinaryExpression cmp = asBin(res);
        assertOp(cmp, ComparisonNe.class);
        assertTrue(isZeroConst(cmp.getRight()));
        BinaryExpression diff = asBin(cmp.getLeft());
        assertIsSubXY(diff, x, y);
    }

    @Test // x + y > 0 (float) => -x - y <= 0
    public void addZeroNormalizationFloat() throws SemanticException {
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
        assertTrue("Expected negations of x and y, got: " + e1 + ", " + e2,
                (e1 == x && e2 == y) || (e1 == y && e2 == x));

    }

    @Test // x - y >= 0 (float) => (y - x) <= 0
    public void subZeroNormalizationFloat() throws SemanticException {
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

    @Test // -x + y >= 0 (float) => (x - y) <= 0
    public void negAddZeroNormalizationFloat() throws SemanticException {
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

    @Test // -x - y >= 0 (float) => (x + y) <= 0
    public void negSubZeroNormalizationFloat() throws SemanticException {
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

    @Test // (x - y) + 1 <= 0 does not throw (float)
    public void subAddConstLeZeroDoesNotThrowFloat() throws SemanticException {
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

    @Test // not(x >= 0) => x <= 0
    // not(x >= 0) is equivalent to x < 0, but in float case we need to relax to x
    public void notGeZeroFloat() throws SemanticException {
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

    @Test // not(x < 0) => -x <= 0
    public void notLtZeroFloat() throws SemanticException {
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

    @Test // not(x >= 1) (float) => x <= 1
    public void notGeOneFloat() throws SemanticException {
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

    @Test // not(x <= y) (float) => (y - x) <= 0
    public void notLeFloat() throws SemanticException {
        BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
        UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
        SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

        BinaryExpression le = asBin(res);
        assertOp(le, ComparisonLe.class);
        assertTrue(isZeroConst(le.getRight()));
        BinaryExpression diff = asBin(le.getLeft());
        assertIsSubXY(diff, y, x);
    }

    @Test // not(x <= c) (float) => (-x + c) <= 0
    public void notLeConstFloat() throws SemanticException {
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

    @Test // not(x < y) (float) => (y - x) <= 0
    public void notLtFloat() throws SemanticException {
        BinaryExpression base = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLt.INSTANCE, location);
        UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, base, LogicalNegation.INSTANCE, location);
        SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

        BinaryExpression le = asBin(res);
        assertOp(le, ComparisonLe.class);
        assertTrue(isZeroConst(le.getRight()));
        BinaryExpression diff = asBin(le.getLeft());
        assertIsSubXY(diff, y, x);
    }

    @Test // not((x <= y) AND (y <= z)) (float) => not(x <= y) OR not(y <= z)
    public void deMorganAndFloat() throws SemanticException {
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

    @Test // not(x == y) (float) => (x - y) != 0
    public void notEqBecomesNeFloat() throws SemanticException {
        BinaryExpression eq = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonEq.INSTANCE, location);
        UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, eq, LogicalNegation.INSTANCE, location);
        SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

        BinaryExpression cmp = asBin(res);
        assertOp(cmp, ComparisonNe.class);
    }

    @Test // not(x != y) (float) => x == y (forma normalizzata)
    public void notNeBecomesEqFloat() throws SemanticException {
        BinaryExpression ne = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonNe.INSTANCE, location);
        UnaryExpression not = new UnaryExpression(BoolType.INSTANCE, ne, LogicalNegation.INSTANCE, location);
        SymbolicExpression res = BooleanExpressionNormalizer.normalize(not, location);

        BinaryExpression and = asBin(res);
        assertTrue(and.getOperator() instanceof LogicalAnd);
    }

    @Test // (x <= y) OR (y <= z) remains OR with normalized operands
    public void orRemainsOrFloat() throws SemanticException {
        BinaryExpression a = new BinaryExpression(BoolType.INSTANCE, x, y, ComparisonLe.INSTANCE, location);
        BinaryExpression b = new BinaryExpression(BoolType.INSTANCE, y, z, ComparisonLe.INSTANCE, location);
        BinaryExpression or = new BinaryExpression(BoolType.INSTANCE, a, b, LogicalOr.INSTANCE, location);

        SymbolicExpression res = BooleanExpressionNormalizer.normalize(or, location);
        BinaryExpression top = asBin(res);
        assertTrue(top.getOperator() instanceof LogicalOr);
        assertTrue(top.getLeft() instanceof BinaryExpression);
        assertTrue(top.getRight() instanceof BinaryExpression);
    }

    @Test // (x - 5) <= 0 remains unchanged shape
    public void subConstLeZeroRemainsFloat() throws SemanticException {
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

    @Test // not(x + 100 < 0) (float) => -x -100 <= 0
    public void notAdd100LtZeroFloat() throws SemanticException {
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
