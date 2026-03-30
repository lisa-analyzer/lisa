package it.unive.lisa.analysis.apron;

import apron.*;
import gmp.Mpfr;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Predicate;

public class Apron implements ValueDomain<Apron>, ValueLattice<Apron> {

    private static Manager manager;


    final Abstract1 state;

    private static boolean IS_AVAILABLE = false;

    public static void loadLibrary() {
        boolean loaded = false;
        try {
            // jgmp necessary as dependency
            System.loadLibrary("jgmp");
            System.loadLibrary("japron");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[WARNING]: Apron library not loaded: " + e.getMessage());
        }
        IS_AVAILABLE = loaded;
    }

    public static void loadLibrary(String folderPath) {
        boolean loaded = false;
        try {
            // gmp needed to japron as dependence
            System.load(folderPath + "/libjgmp.so");
            System.load(folderPath + "/libjapron.so");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[WARNING]: Apron library not loaded from " + folderPath + ": " + e.getMessage());
        }
        IS_AVAILABLE = loaded;
    }

    // Allow to LiSA to verify if Apron is supported
    public static Boolean isAvailable() {
        return IS_AVAILABLE;
    }

    /*
     * START TODO METHODS SECTIONS
     * */

    @Override
    public Apron pushScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
        return new Apron(state);
    }

    @Override
    public Apron popScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
        return new Apron(state);
    }

    /*
     * END TODO METHODS SECTION
     * */

    public enum ApronDomain {
        /**
         * Intervals
         */
        Box,

        /**
         * Octagons
         */
        Octagon,

        /**
         * Convex polyhedra
         */
        Polka,

        /**
         * Linear equalities
         */
        PolkaEq,

        /**
         * Reduced product of the Polka convex polyhedra and PplGrid the linear congruence equalities domains
         * Compile Apron with the specific flag for PPL set to 1 in order to use such domain.
         */
        PolkaGrid,

        /**
         * Parma Polyhedra Library linear congruence equalities domain
         * Compile Apron with the specific flag for PPL set to 1 in order to use such domain.
         */
        PplGrid,

        /**
         * The Parma Polyhedra library convex polyhedra domain
         * Compile Apron with the specific flag for PPL set to 1 in order to use such domain.
         */
        PplPoly
    }

    public static void setManager(ApronDomain numericalDomain) {
        if (!IS_AVAILABLE) {
            throw new UnsupportedOperationException(
                    "Failed to set Apron manager: native library missing."
            );
        }

        switch (numericalDomain) {
            case Box:
                manager = new apron.Box();
                break;
            case Octagon:
                manager = new Octagon();
                break;
            case Polka:
                manager = new Polka(false);
                break;
            case PolkaEq:
                manager = new PolkaEq();
                break;
            // ppl needed
            case PplGrid:
            case PplPoly:
                throw new UnsupportedOperationException(
                        numericalDomain + " domain require PPL library, which is not included in the current Apron compilation"
                );
            default:
                throw new UnsupportedOperationException("Numerical domain " + numericalDomain + " unknown in Apron");
        }
    }

    public Apron() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("Failed to initialize Apron domain: native library missing.");
        }

        // If user doesn't set the manager, Box is used by default
        if (manager == null) {
            setManager(ApronDomain.Box);
        }

        try {
            String[] vars = {"<ret>"}; // Variable needed to represent the returned value
            state = new Abstract1(manager, new apron.Environment(new String[0], vars));
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }

    Apron(Abstract1 state) {
        this.state = state;
    }

    @Override
    public Apron assign(Apron state, Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {

        try {
            Environment env = state.state.getEnvironment();
            Var variable = toApronVar(id);
            Abstract1 newState;
            if (!env.hasVar(variable)) {
                Var[] vars = {variable};
                env = env.add(new Var[0], vars);
                newState = state.state.changeEnvironmentCopy(manager, env, false);
            } else
                newState = state.state;

            Texpr1Node apronExpression = toApronExpression(expression);

            // we are not able to translate expression
            // hence, we treat it as "don't know"
            if (apronExpression == null)
                return forgetAbstractionOf(newState, id, pp, oracle);//new Apron(newState.forgetCopy(manager, toApronVar(id), false));//

            Var[] vars = apronExpression.getVars();

            for (Var var : vars)
                if (!newState.getEnvironment().hasVar(var)) {
                    Var[] vars1 = {var};
                    env = newState.getEnvironment().add(new Var[0], vars1);
                    newState = newState.changeEnvironmentCopy(manager, env, false);
                }

            return new Apron(newState.assignCopy(manager, variable, new Texpr1Intern(newState.getEnvironment(), apronExpression), null));

        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }


    private Apron forgetAbstractionOf(Abstract1 newState, Identifier id, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        Apron result = new Apron(newState);
        result = result.forgetIdentifiers(java.util.Collections.singleton(id), pp);

        // Using Untyped.INSTANCE for do not depend on the frontend
        Constant zero = new Constant(Untyped.INSTANCE, 0, SyntheticLocation.INSTANCE);

        // Creation of the bin expr >= and <=
        BinaryExpression geExpr = new BinaryExpression(Untyped.INSTANCE, id, zero, ComparisonGe.INSTANCE, SyntheticLocation.INSTANCE);
        BinaryExpression leExpr = new BinaryExpression(Untyped.INSTANCE, id, zero, ComparisonLe.INSTANCE, SyntheticLocation.INSTANCE);

        // Call of the modified assume method
        // pp used as scr and dest because the method is forcing the assumption on a node
        Apron ge = result.assume(result, geExpr, pp, pp, oracle);
        Apron le = result.assume(result, leExpr, pp, pp, oracle);
        return ge.lub
                (le);
    }

    private Texpr1Node toApronExpression(SymbolicExpression exp) throws ApronException {
        if (exp instanceof Identifier)
            return new Texpr1VarNode(((Identifier) exp).getName());

        if (exp instanceof Constant) {
            Constant c = (Constant) exp;
            Coeff coeff;

            if (c.getValue() instanceof Integer)
                coeff = new MpqScalar((int) c.getValue());
            else if (c.getValue() instanceof Float)
                coeff = new MpfrScalar((double) c.getValue(), Mpfr.getDefaultPrec());
            else if (c.getValue() instanceof Long)
                coeff = new MpfrScalar((long) c.getValue(), Mpfr.getDefaultPrec());
            else if (c.getValue() instanceof BigInteger)
                coeff = new MpfrScalar(new Mpfr(((BigInteger) c.getValue()), Mpfr.RNDN));
            else
                return null;


            return new Texpr1CstNode(coeff);
        }

        if (exp instanceof UnaryExpression) {
            return null;
            //			UnaryExpression un = (UnaryExpression) exp;
            //			UnaryOperator op = un.getOperator();
            //			if (op == LogicalNegation.INSTANCE)
            //				break;
            //			else if (op == NumericNegation.INSTANCE)
            //				break;
            //			else if (op == StringLength.INSTANCE)
            //				break;
            //			else if (op == TypeOf.INSTANCE)
            //				break;
            //			else
            //				break;
        }

        if (exp instanceof BinaryExpression) {
            BinaryExpression bin = (BinaryExpression) exp;
            BinaryOperator op = bin.getOperator();
            if (op == TypeCast.INSTANCE || op == TypeConv.INSTANCE) {
                // check if the expr has a static type assigned
                if (exp.getStaticType() != null)
                    return toApronExpression(bin.getLeft());
            } else {
                Texpr1Node rewrittenLeft = toApronExpression(bin.getLeft());
                if (rewrittenLeft == null)
                    return null;

                Texpr1Node rewrittenRight = toApronExpression(bin.getRight());
                if (rewrittenRight == null)
                    return null;

                if (op == ComparisonLt.INSTANCE)
                    return new Texpr1BinNode(Tcons1.SUP, rewrittenRight, rewrittenLeft);

                if (op == ComparisonLe.INSTANCE)
                    return new Texpr1BinNode(Tcons1.SUPEQ, rewrittenRight, rewrittenLeft);

                if (!canBeConvertedToApronOperator(bin.getOperator()))
                    // we are not able to translate the expression
                    return null;

                return new Texpr1BinNode(toApronOperator(bin.getOperator()), rewrittenLeft, rewrittenRight);
            }
        }

        // we are not able to translate the expression
        return null;
    }


    private boolean canBeConvertedToApronOperator(BinaryOperator op) {
        return op == StringConcat.INSTANCE
                || op == NumericNonOverflowingAdd.INSTANCE
                || op == NumericNonOverflowingMul.INSTANCE
                || op == NumericNonOverflowingDiv.INSTANCE
                || op == NumericNonOverflowingSub.INSTANCE
                || op == NumericNonOverflowingMod.INSTANCE
                || op == ComparisonEq.INSTANCE
                || op == ComparisonNe.INSTANCE
                || op == ComparisonGe.INSTANCE
                || op == ComparisonGt.INSTANCE;
    }

    private int toApronOperator(BinaryOperator op) {
        if (op == StringConcat.INSTANCE || op == NumericNonOverflowingAdd.INSTANCE)
            return Texpr1BinNode.OP_ADD;
        else if (op == NumericNonOverflowingMul.INSTANCE)
            return Texpr1BinNode.OP_MUL;
        else if (op == NumericNonOverflowingSub.INSTANCE)
            return Texpr1BinNode.OP_SUB;
        else if (op == NumericNonOverflowingDiv.INSTANCE)
            return Texpr1BinNode.OP_DIV;
        else if (op == NumericNonOverflowingMod.INSTANCE)
            return Texpr1BinNode.OP_MOD;
        else if (op == ComparisonEq.INSTANCE)
            return Tcons1.EQ;
        else if (op == ComparisonNe.INSTANCE)
            return Tcons1.DISEQ;
        else if (op == ComparisonGe.INSTANCE)
            return Tcons1.SUPEQ;
        else if (op == ComparisonGt.INSTANCE)
            return Tcons1.SUP;

        throw new UnsupportedOperationException("Operator " + op + " not yet supported by Apron interface");

    }

    @Override
    public Apron smallStepSemantics(Apron state, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        // the small-step semantics does not alter the state, but it should
        // add to the environment the identifiers produced by expression in order to be
        // tracked by Apron

        if (expression instanceof Identifier) {
            Identifier id = (Identifier) expression;
            Environment env = state.state.getEnvironment();
            Var variable = toApronVar(id);
            if (!env.hasVar(variable)) {
                Var[] vars = {variable};
                env = env.add(new Var[0], vars);
                try {
                    return new Apron(state.state.changeEnvironmentCopy(manager, env, state.state.isBottom(manager)));
                } catch (ApronException e) {
                    throw new UnsupportedOperationException("Apron library crashed", e);
                }
            } else
                return new Apron(state.state);
        }

        if (expression instanceof UnaryExpression) {
            UnaryExpression un = (UnaryExpression) expression;
            return smallStepSemantics(state, (ValueExpression) un.getExpression(), pp, oracle);
        }

        if (expression instanceof BinaryExpression) {
            BinaryExpression bin = (BinaryExpression) expression;
            Apron left = smallStepSemantics(state, (ValueExpression) bin.getLeft(), pp, oracle);
            Apron right = smallStepSemantics(state, (ValueExpression) bin.getRight(), pp, oracle);
            return left.lub(right);
        }

        return new Apron(state.state);
    }

    @Override
    public Apron assume(Apron state, ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
        try {
            if (state.state.isBottom(manager))
                return state;
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
        if (expression instanceof UnaryExpression) {
            UnaryExpression un = (UnaryExpression) expression;
            Operator op = un.getOperator();

            if (op == LogicalNegation.INSTANCE) {
                ValueExpression inner = (ValueExpression) un.getExpression();
                if (inner instanceof UnaryExpression && ((UnaryExpression) inner).getOperator() == LogicalNegation.INSTANCE) {
                    // Passed src and dest instead pp
                    return assume(state, ((ValueExpression) ((UnaryExpression) inner).getExpression()).removeNegations(), src, dest, oracle);
                }

                ValueExpression rewritten = un.removeNegations();
                if (rewritten != un)
                    return assume(state, rewritten, src, dest, oracle);
                else
                    return state;
            } else {
                return state;
            }
        }

        if (expression instanceof BinaryExpression) {
            BinaryExpression bin = (BinaryExpression) expression;
            BinaryOperator op = bin.getOperator();
            Apron left, right;

            if (op == ComparisonEq.INSTANCE
                    || op == ComparisonGe.INSTANCE
                    || op == ComparisonGt.INSTANCE
                    || op == ComparisonLe.INSTANCE
                    || op == ComparisonLt.INSTANCE
                    || op == ComparisonNe.INSTANCE) {

                try {
                    return new Apron(state.state.meetCopy(manager, toApronComparison(state, bin)));
                } catch (ApronException e) {
                    throw new UnsupportedOperationException("Apron library crashed", e);
                } catch (UnsupportedOperationException e) {
                    return state;
                }
            } else if (op == LogicalAnd.INSTANCE) {
                left = assume(state, (ValueExpression) bin.getLeft(), src, dest, oracle);
                right = assume(state, (ValueExpression) bin.getRight(), src, dest, oracle);
                try {
                    return new Apron(left.state.meetCopy(manager, right.state));
                } catch (ApronException e) {
                    throw new UnsupportedOperationException("Apron library crashed", e);
                } catch (UnsupportedOperationException e) {
                    return state;
                }
            } else if (op == LogicalOr.INSTANCE) {
                left = assume(state, (ValueExpression) bin.getLeft(), src, dest, oracle);
                right = assume(state, (ValueExpression) bin.getRight(), src, dest, oracle);
                try {
                    return new Apron(left.state.joinCopy(manager, right.state));
                } catch (ApronException e) {
                    throw new UnsupportedOperationException("Apron library crashed", e);
                } catch (UnsupportedOperationException e) {
                    return state;
                }
            } else {
                return state;
            }
        }

        return state;
    }

    @Override
    public boolean knowsIdentifier(Identifier id) {
        if (id == null || this.state == null)
            return false;
        try {
            String var = id.getName();
            return state.getEnvironment().hasVar(var);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Apron library crashed during knowsIdentifier for var: " + id.getName(), e);
        }
    }

    private Tcons1 toApronComparison(Apron state, BinaryExpression exp) throws ApronException {
        // Apron supports only "exp <comparison> 0", so we need to move everything on the left node
        SymbolicExpression combinedExpr = new BinaryExpression(exp.getStaticType(), exp.getLeft(), exp.getRight(), NumericNonOverflowingSub.INSTANCE, exp.getCodeLocation());
        BinaryOperator op = exp.getOperator();
        if (op == ComparisonGt.INSTANCE
                || op == ComparisonGe.INSTANCE
                || op == ComparisonNe.INSTANCE
                || op == ComparisonEq.INSTANCE) {
            Texpr1Node apronExpression = toApronExpression(combinedExpr);
            if (apronExpression != null)
                return new Tcons1(state.state.getEnvironment(), toApronOperator(exp.getOperator()), apronExpression);
            else
                throw new UnsupportedOperationException("Comparison operator " + exp.getOperator() + " not yet supported");
        } else if (op == ComparisonLe.INSTANCE)
            return toApronComparison(state, new BinaryExpression(exp.getStaticType(), exp.getRight(), exp.getLeft(), ComparisonGe.INSTANCE, exp.getCodeLocation()));
        else if (op == ComparisonLt.INSTANCE)
            return toApronComparison(state, new BinaryExpression(exp.getStaticType(), exp.getRight(), exp.getLeft(), ComparisonGt.INSTANCE, exp.getCodeLocation()));
        else
            throw new UnsupportedOperationException("Comparison operator " + exp.getOperator() + " not yet supported");
    }

    @Override
    public Satisfiability satisfies(Apron state, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        try {

            if (state.state.getEnvironment().equals(new apron.Environment()))
                return Satisfiability.BOTTOM;

            if (expression instanceof UnaryExpression) {
                UnaryExpression un = (UnaryExpression) expression;
                Operator op = un.getOperator();

                if (op == LogicalNegation.INSTANCE) {
                    Satisfiability isSAT = satisfies(state, (ValueExpression) un.getExpression(), pp, oracle);
                    if (isSAT == Satisfiability.SATISFIED)
                        return Satisfiability.NOT_SATISFIED;
                    else if (isSAT == Satisfiability.NOT_SATISFIED)
                        return Satisfiability.SATISFIED;
                    else
                        return Satisfiability.UNKNOWN;
                } else
                    return Satisfiability.UNKNOWN;
            }


            if (expression instanceof BinaryExpression) {
                BinaryExpression bin = (BinaryExpression) expression;
                BinaryExpression neg;
                BinaryOperator op = bin.getOperator();
                // FIXME: it seems there's a bug with manager.wasExact
                if (op == ComparisonEq.INSTANCE) {
                    if (state.state.satisfy(manager, toApronComparison(state,bin)))
                        return Satisfiability.SATISFIED;
                    else {
                        neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(), ComparisonNe.INSTANCE, bin.getCodeLocation());

                        if (state.state.satisfy(manager, toApronComparison(state, neg)))
                            return Satisfiability.NOT_SATISFIED;

                        return Satisfiability.UNKNOWN;
                    }
                } else if (op == ComparisonGe.INSTANCE) {
                    if (state.state.satisfy(manager, toApronComparison(state, bin)))
                        return Satisfiability.SATISFIED;
                    else {
                        neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(), ComparisonLt.INSTANCE, bin.getCodeLocation());

                        if (state.state.satisfy(manager, toApronComparison(state, neg)))
                            return Satisfiability.NOT_SATISFIED;

                        return Satisfiability.UNKNOWN;
                    }
                } else if (op == ComparisonGt.INSTANCE) {
                    if (state.state.satisfy(manager, toApronComparison(state, bin)))
                        return Satisfiability.SATISFIED;
                    else {
                        neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(), ComparisonLe.INSTANCE, bin.getCodeLocation());

                        if (state.state.satisfy(manager, toApronComparison(state, neg)))
                            return Satisfiability.NOT_SATISFIED;

                        return Satisfiability.UNKNOWN;
                    }
                } else if (op == ComparisonLe.INSTANCE) {
                    if (state.state.satisfy(manager, toApronComparison(state, bin)))
                        return Satisfiability.SATISFIED;
                    else {
                        neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(), ComparisonGt.INSTANCE, bin.getCodeLocation());

                        if (state.state.satisfy(manager, toApronComparison(state, neg)))
                            return Satisfiability.NOT_SATISFIED;

                        return Satisfiability.UNKNOWN;
                    }
                } else if (op == ComparisonLt.INSTANCE) {
                    if (state.state.satisfy(manager, toApronComparison(state, bin)))
                        return Satisfiability.SATISFIED;
                    else {
                        neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(), ComparisonGe.INSTANCE, bin.getCodeLocation());

                        if (state.state.satisfy(manager, toApronComparison(state, neg)))
                            return Satisfiability.NOT_SATISFIED;

                        return Satisfiability.UNKNOWN;
                    }
                } else if (op == ComparisonNe.INSTANCE) {
                    if (state.state.satisfy(manager, toApronComparison(state, bin)))
                        return Satisfiability.SATISFIED;
                    else {
                        neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(), ComparisonEq.INSTANCE, bin.getCodeLocation());

                        if (state.state.satisfy(manager, toApronComparison(state, neg)))
                            return Satisfiability.NOT_SATISFIED;

                        return Satisfiability.UNKNOWN;
                    }

                } else if (op == LogicalAnd.INSTANCE)
                    return satisfies(state, (ValueExpression) bin.getLeft(), pp, oracle).and(satisfies(state, (ValueExpression) bin.getRight(), pp, oracle));
                else if (op == LogicalOr.INSTANCE)
                    return satisfies(state, (ValueExpression) bin.getLeft(), pp, oracle).or(satisfies(state, (ValueExpression) bin.getRight(), pp, oracle));
                else
                    return Satisfiability.UNKNOWN;
            }

            return Satisfiability.UNKNOWN;
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        } catch (UnsupportedOperationException e) {
            // if a sub-expression of expression cannot be
            // translated by Apron, then Unknown is returned.
            return Satisfiability.UNKNOWN;
        }
    }

    @Override
    public Apron lub(Apron other) throws SemanticException {

        // we compute the least environment extending this and other environment
        Environment lubEnv = state.getEnvironment().lce(other.state.getEnvironment());
        try {
            Abstract1 unifiedThis = state.changeEnvironmentCopy(manager, lubEnv, state.isBottom(manager));
            if (other.state.isBottom(manager))
                return new Apron(unifiedThis);

            Abstract1 unifiedOther = other.state.changeEnvironmentCopy(manager, lubEnv, other.state.isBottom(manager));
            if (this.state.isBottom(manager))
                return new Apron(unifiedOther);

            if (state.isTop(manager) || other.state.isTop(manager))
                return new Apron(unifiedOther);

            return new Apron(unifiedThis.joinCopy(manager, unifiedOther));
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }

    public Apron meet(Apron other) throws SemanticException {
        try {
            // we compute the least environment extending this and other environment
            Environment lubEnv = state.getEnvironment().lce(other.state.getEnvironment());
            Abstract1 unifiedThis = state.changeEnvironmentCopy(manager, lubEnv, state.isBottom(manager));
            Abstract1 unifiedOther = other.state.changeEnvironmentCopy(manager, lubEnv, other.state.isBottom(manager));

            return new Apron(unifiedThis.meetCopy(manager, unifiedOther));
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }


    @Override
    public Apron widening(Apron other) throws SemanticException {
        try {
            if (other.state.isBottom(manager))
                return new Apron(state);
            if (this.state.isBottom(manager))
                return other;
            return new Apron(state.widening(manager, other.state));
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }

    @Override
    public boolean lessOrEqual(Apron other) throws SemanticException {
        try {
            if (state.isBottom(manager))
                return true;
            else if (other.state.isBottom(manager))
                return false;
            else if (other.state.isTop(manager))
                return true;
            else if (state.isTop(manager))
                return false;

            // we first need to  uniform the environments
            Environment unifiedEnv = state.getEnvironment().lce(other.state.getEnvironment());

            Abstract1 unifiedOther = other.state.changeEnvironmentCopy(manager, unifiedEnv, false);
            Abstract1 unifiedThis = state.changeEnvironmentCopy(manager, unifiedEnv, false);

            return unifiedThis.isIncluded(manager, unifiedOther);
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }

    @Override
    public Apron top() {
        try {
            return new Apron(new Abstract1(manager, new apron.Environment()));
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }

    @Override
    public Apron bottom() {
        try {
            return new Apron(new Abstract1(manager, new apron.Environment(), true));
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }

    @Override
    public boolean isBottom() {
        try {
            // delegate the computation to apron
            return state.isBottom(manager);
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }

    @Override
    public boolean isTop() {
        try {
            // delegate the computation to apron
            return state.isTop(manager);
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }

    private Var toApronVar(Identifier id) {
        String n = id.getName();
        return new StringVar(n);
    }

    public boolean containsIdentifier(Identifier id) {
        return Arrays.asList(state.getEnvironment().getVars()).contains(toApronVar(id));
    }

    public Abstract1 getApronState() {
        return state;
    }

    @Override
    public Apron forgetIdentifiers(Iterable<Identifier> ids, ProgramPoint pp) throws SemanticException {
        try {
            if (state.isBottom(manager) || state.isTop(manager)) {
                return this;
            }

            // extract env
            apron.Environment env = state.getEnvironment();
            java.util.List<apron.Var> varsToForget = new java.util.ArrayList<>();

            for (Identifier id : ids) {
                apron.Var apronVar = toApronVar(id);

                if (env.hasVar(apronVar.toString())) {
                    varsToForget.add(apronVar);
                }
            }

            if (varsToForget.isEmpty()) {
                return this;
            }

            // list -> array: required from apron
            apron.Var[] varsArray = varsToForget.toArray(new apron.Var[0]);

            return new Apron(state.forgetCopy(manager, varsArray, false));

        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed during forgetIdentifiers", e);
        }
    }

    @Override
    public Apron forgetIdentifier(Identifier id, ProgramPoint pp) throws SemanticException {
        if (!containsIdentifier(id))
            return this;

        try {
            return new Apron(state.forgetCopy(manager, toApronVar(id), false));
        } catch (ApronException e) {
            throw new UnsupportedOperationException("Apron library crashed", e);
        }
    }

    @Override
    public StructuredRepresentation representation() {
        return new StringRepresentation(state.toString());
    }

    @Override
    public Apron store(Identifier target, Identifier source) throws SemanticException {
        try {
            // no assign
            if (state.isBottom(manager)) {
                return this;
            }

            apron.Environment env = state.getEnvironment();
            String targetName = target.getName();
            String sourceName = source.getName();

            if (!env.hasVar(sourceName)) {
                // variable doesn't exist in apron's env
                throw new SemanticException("Error during store() metohd. Var '" + sourceName + "' does not exist in the current environment");
            }

            // Tree expr for var source
            apron.Texpr1Intern expr = new apron.Texpr1Intern(env, new apron.Texpr1VarNode(sourceName));

            // target <- expr - must be in the env
            return new Apron(state.assignCopy(manager, targetName, expr, null));

        } catch (Exception e) {
            throw new SemanticException("Apron error during sotre() method:", e);
        }
    }


    @Override
    public Apron makeLattice() {
        return new Apron();
    }

    @Override
    public Apron forgetIdentifiersIf(Predicate<Identifier> test, ProgramPoint pp) throws SemanticException {
        try {
            if (state.isBottom(manager) || state.isTop(manager)) {
                return this;
            }

            apron.Environment env = state.getEnvironment();
            java.util.List<apron.Var> varsToRemove = new java.util.ArrayList<>();

            // cicle on all apron vars
            for (Var var : env.getVars()) {
                String varName = var.toString();

                Identifier tmpId = new it.unive.lisa.symbolic.value.Variable(
                        it.unive.lisa.type.Untyped.INSTANCE,
                        varName,
                        pp.getLocation()
                );

                if (test.test(tmpId)) {
                    varsToRemove.add(var);
                }
            }

            if (varsToRemove.isEmpty()) {
                return this;
            }

            // list -> array
            apron.Var[] arrayToRemove = varsToRemove.toArray(new apron.Var[0]);

            apron.Environment newEnv = env.remove(arrayToRemove);

            return new Apron(state.changeEnvironmentCopy(manager, newEnv, false));

        } catch (Exception e) {
            throw new SemanticException("Apron error in forgetIdentifiersIf() method", e);
        }
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(state);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Apron other = (Apron) obj;
        return java.util.Objects.equals(this.state, other.state);
    }
}