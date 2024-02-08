package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import javassist.compiler.ast.Symbol;
import org.graphstream.ui.graphicGraph.stylesheet.Value;

import java.util.function.Predicate;

public class Stability implements ValueDomain<Stability> {

    /**
     * The abstract top element.
     */
    public static final Stability TOP = new Stability((byte) 0);

    /**
     * The abstract bottom element.
     */
    public static final Stability BOTTOM = new Stability((byte) 1);

    /**
     * The abstract stable element.
     */
    public static final Stability STABLE = new Stability((byte) 2);

    /**
     * The abstract increasing element.
     */
    public static final Stability INC = new Stability((byte) 3);

    /**
     * The abstract decreasing element.
     */
    public static final Stability DEC = new Stability((byte) 4);

    /**
     * The abstract not stable element.
     */
    public static final Stability NON_STABLE = new Stability((byte) 5);

    /**
     * The abstract non-increasing element.
     */
    public static final Stability NON_INC = new Stability((byte) 6);

    /**
     * The abstract non-decreasing element.
     */
    public static final Stability NON_DEC = new Stability((byte) 7);

    private final byte stability;

    private final ValueEnvironment<Interval> intervals;

    public Stability(byte stability){
        this.stability = stability;
        this.intervals = new ValueEnvironment<>(new Interval()).top();
    }

    public Stability(byte stability, ValueEnvironment<Interval> intervals) {
        this.stability = stability;
        this.intervals = intervals;
    }

    @Override
    public boolean lessOrEqual(Stability other) throws SemanticException {
        return false;
    }

    @Override
    public Stability lub(Stability other) throws SemanticException {
        return null;
    }

    @Override
    public Stability top() {
        return TOP;
    }

    @Override
    public Stability bottom() {
        return BOTTOM;
    }

    @Override
    public Stability pushScope(ScopeToken token) throws SemanticException {
        return null;
    }

    @Override
    public Stability popScope(ScopeToken token) throws SemanticException {
        return null;
    }

    private boolean queryToAux(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return intervals.satisfies(expression, pp, oracle) == Satisfiability.SATISFIED;

    }

    /**
     * Compares values of id and the result of expression in the auxiliary abstract domain
     * @param id
     * @param expression
     * @param pp
     * @param oracle
     * @return  STABLE iff id == expression
     *          [...]
     */
    private Stability auxCompareAfterAssignment(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (queryToAux("x > expr", pp, oracle)) return DEC;
        else if (queryToAux("x == expr", pp, oracle)) return STABLE;
        else if (queryToAux("x < expr", pp, oracle)) return INC;
        else if (queryToAux("x >= expr", pp, oracle)) return NON_INC;
        else if (queryToAux("x <= expr", pp, oracle)) return NON_DEC;
        else if (queryToAux("x != expr", pp, oracle)) return NON_STABLE;
        else return TOP;
    }

    /**
     * Auxiliary function for comparing two ValueExpressions in the auxiliary abstract domain
     * @param a
     * @param b
     * @param pp
     * @param oracle
     * @return
     * @throws SemanticException
     */
    private Stability increasingIfGreater(ValueExpression a, ValueExpression b, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if ( queryToAux(new BinaryExpression(a.getStaticType(), a, b, new ComparisonGt()), pp, oracle) ) return INC;
        else if (queryToAux("a == b", pp, oracle)) return STABLE;
        else if (queryToAux("a < b", pp, oracle)) return DEC;
        else if (queryToAux("a >= b", pp, oracle)) return NON_DEC;
        else if (queryToAux("a <= b", pp, oracle)) return NON_INC;
        else if (queryToAux("a != b", pp, oracle)) return NON_STABLE;
        else return TOP;
    }

    private Stability increasingIfLess(ValueExpression a, ValueExpression b, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return increasingIfGreater(b, a, pp, oracle);
    }

    private Stability nonDecreasingIfGreaterOrEqual(ValueExpression a, ValueExpression b, ProgramPoint pp, SemanticOracle oracle){

        if (queryToAux("a == b", pp, oracle)) return STABLE;

        if (queryToAux("a > b", pp, oracle)
                || queryToAux("a >= b", pp, oracle))
            return NON_DEC;

        else if (queryToAux("a < b", pp, oracle)
                || queryToAux("a <= b", pp, oracle))
            return NON_INC;

        else return TOP;
    }

    private Stability nonDecreasingIfLessOrEqual(ValueExpression a, ValueExpression b, ProgramPoint pp, SemanticOracle oracle){
        return nonDecreasingIfGreaterOrEqual(b, a, pp, oracle);
    }

    private Stability increasingIfBetween(ValueExpression a, ValueExpression b1, ValueExpression b2, ProgramPoint pp, SemanticOracle oracle){
        if (queryToAux("a > b1", pp, oracle) && queryToAux("a < b2", pp, oracle))
            return INC;
        else if (queryToAux("a > b1", pp, oracle) && queryToAux("a < b2", pp, oracle))

    }



    // TO DO: fix the queries
    @Override
    public Stability assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {

        if (expression instanceof UnaryExpression &&
                ((UnaryExpression) expression).getOperator() instanceof NumericNegation)
            return auxCompareAfterAssignment(id, expression, pp, oracle);

        else if (expression instanceof BinaryExpression) {
            BinaryExpression be = (BinaryExpression) expression;
            BinaryOperator op = be.getOperator();
            SymbolicExpression left = be.getLeft();
            SymbolicExpression right = be.getRight();

            boolean isLeft = id.equals(be.getLeft());
            boolean isRight = id.equals(be.getRight());

            // x = a + b
            if (op instanceof AdditionOperator) {
                if (isLeft) return increasingIfGreater(right, 0, pp, oracle);
                else if (isRight) return increasingIfGreater(left, 0, pp, oracle);
                else return auxCompareAfterAssignment(id, expression, pp, oracle);
            }

            // x = a - b
            if (op instanceof SubtractionOperator){
                if (isLeft) return increasingIfLess(right, 0, pp, oracle);
                else return auxCompareAfterAssignment(id, expression, pp, oracle);
            }

            // x = a * b
            if (op instanceof MultiplicationOperator) {

                if (isLeft || isRight) {
                    SymbolicExpression other = isLeft ? right : left;

                    if (queryToAux("x == 0", pp, oracle)
                            || queryToAux("other == 1", pp, oracle))
                        return STABLE;

                    else if (queryToAux("x > 0", pp, oracle))
                        return increasingIfGreater(other, 1, pp, oracle);

                    else if (queryToAux("x < 0", pp, oracle))
                        return increasingIfLess(other, 1, pp, oracle);

                    else if (queryToAux("x >= 0", pp, oracle)) {
                        if (queryToAux("other != 1", pp, oracle))
                            return NON_STABLE;
                        else return nonDecreasingIfGreaterOrEqual(other, 1, pp, oracle);

                        /*
                        if (queryToAux("other > 1", pp, oracle)
                                || queryToAux("other >= 1", pp, oracle))
                            return NON_DEC;
                        else if (queryToAux("other < 1", pp, oracle)
                                || queryToAux("other <= 1", pp, oracle)
                                || queryToAux("other == 0", pp, oracle))
                            return NON_INC;
                        */


                    } else if (queryToAux("x <= 0", pp, oracle)) {
                        if (queryToAux("other != 1", pp, oracle))
                            return NON_STABLE;
                        else return nonDecreasingIfLessOrEqual(other, 1, pp, oracle);

                    } else if (queryToAux("x != 0", pp, oracle))
                        return NON_STABLE;
                }

                else return auxCompareAfterAssignment(id, expression, pp, oracle);
            }

            else if (op instanceof DivisionOperator)
                return auxCompareAfterAssignment(id, expression, pp, oracle);
        }
        return TOP;
    }
                /*
        if (expression instanceof UnaryExpression){
            UnaryExpression ue = (UnaryExpression) expression;

            if (ue.getOperator() instanceof NumericNegation) {
                // "x = - x"
                if (id.equals(ue.getExpression())) {

                    //return auxCompare(id, 0, pp, oracle);

                    // x -> [0, 0]
                    if (queryToAux("id == 0", pp, oracle))
                        return STABLE;

                        // x -> [a, b] with a > 0
                    else if (queryToAux("id > 0", pp, oracle))
                        return DEC;

                        // x -> [a, b] with b < 0
                    else if (queryToAux("id < 0", pp, oracle))
                        return INC;

                        // x -> [a, b] with a == 0 (b != 0)
                    else if (queryToAux("id >= 0", pp, oracle))
                        return NON_INC;

                        // x -> [a, b] with b == 0 (a != 0)
                    else if (queryToAux("id <= 0", pp, oracle))
                        return NON_DEC;

                        // x -> [a, b] with a > 0 or b < 0
                    else if (queryToAux("id != 0", pp, oracle))
                        return NON_STABLE;

                    else return TOP;

                }
                // "x = - expr"
                else return auxCompareAfterAssignment(id, expression, pp, oracle);
            }

            // Q: direttamente TOP?
            else return TOP;
        }

        else if (expression instanceof BinaryExpression){
            BinaryExpression be = (BinaryExpression) expression;
            BinaryOperator op = be.getOperator();
            boolean isLeft = id.equals(be.getLeft());
            boolean isRight = id.equals(be.getRight());

            // x = a + b
            if(op instanceof AdditionOperator){
                if (isLeft){
                    if (isRight) return auxCompareAfterAssignment(id, expression, pp, oracle);
                    }
                }


            }

            return TOP;
        }

        else return TOP;
    }
    */

    @Override
    public Stability smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return null;
    }

    @Override
    public Stability assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
        return null;
    }

    @Override
    public boolean knowsIdentifier(Identifier id) {
        return false;
    }

    @Override
    public Stability forgetIdentifier(Identifier id) throws SemanticException {
        return null;
    }

    @Override
    public Stability forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
        return null;
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return null;
    }

    @Override
    public StructuredRepresentation representation() {
        return null;
    }
}
