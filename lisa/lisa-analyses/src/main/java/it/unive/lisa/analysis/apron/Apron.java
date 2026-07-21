package it.unive.lisa.analysis.apron;

import apron.*;
import gmp.Mpfr;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An implementation of a numerical abstract domain based on the Apron library.
 * <p>
 * This class serves as an adapter between the LiSA framework and the native
 * Apron C library. It provides support for various relational and
 * non-relational numerical domains (e.g., Box, Octagon, Polyhedra) to track,
 * evaluate, and constrain numeric variables during abstract interpretation.
 * </p>
 * <p>
 * It implements all standard lattice operations (Least Upper Bound, Greatest
 * Lower Bound, widening, narrowing), arithmetic and semantic operations
 * (assignment, assumption, satisfiability).
 * </p>
 * 
 * @see it.unive.lisa.analysis.value.ValueDomain
 * @see it.unive.lisa.analysis.value.ValueLattice
 */
public class Apron
		implements
		ValueDomain<Apron>,
		ValueLattice<Apron> {

	private static final Logger logger = LogManager.getLogger(Apron.class);

	/**
	 * The internal manager used by the Apron library to coordinate operations
	 * within the chosen numerical domain . It must be initialized before
	 * performing any abstract interpretation.
	 */
	private static Manager manager;

	/**
	 * The internal representation of the abstract state as defined by the
	 * native Apron library.
	 */
	final Abstract1 state;

	/**
	 * A flag indicating whether the native Apron C library was successfully
	 * loaded.
	 */
	private static boolean IS_AVAILABLE = false;

	/**
	 * Attempts to load the native Apron library and its dependency from the
	 * system's default library path. Sets the {@code IS_AVAILABLE} flag to true
	 * if successful, or logs a warning if the UnsatisfiedLinkError is thrown.
	 */
	public static void loadLibrary() {
		boolean loaded = false;
		try {
			// jgmp necessary as dependency
			System.loadLibrary("jgmp");
			System.loadLibrary("japron");
			loaded = true;
		} catch (UnsatisfiedLinkError e) {
			logger.error("[WARNING]: Apron library not loaded:", e);
		}
		IS_AVAILABLE = loaded;
	}

	/**
	 * Attempts to load the native Apron library and its dependency from a
	 * specific folder path. Sets the {@code IS_AVAILABLE} flag to true if
	 * successful, or logs a warning if the library is not found.
	 *
	 * @param folderPath the absolute path to the directory containing the
	 *                       shared libraries.
	 */
	public static void loadLibrary(
			String folderPath) {
		boolean loaded = false;
		try {
			// gmp needed to japron as dependence
			System.load(folderPath + "/libjgmp.so");
			System.load(folderPath + "/libjapron.so");
			loaded = true;
		} catch (UnsatisfiedLinkError e) {
			logger.error("[WARNING]: Apron library not loaded from {}:", folderPath, e);
		}
		IS_AVAILABLE = loaded;
	}

	/**
	 * Allows the LiSA framework to verify if the native Apron library is
	 * supported and currently loaded in the system.
	 *
	 * @return {@code true} if the library is available, {@code false}
	 *             otherwise.
	 */
	public static Boolean isAvailable() {
		return IS_AVAILABLE;
	}

	/*
	 * START TODO METHODS SECTIONS
	 */

	@Override
	public Apron pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new Apron(state);
	}

	@Override
	public Apron popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new Apron(state);
	}

	/*
	 * END TODO METHODS SECTION
	 */

	/**
	 * Represents the supported numerical abstract domains available in the
	 * Apron library.
	 */
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
		 * Reduced product of the Polka convex polyhedra and PplGrid the linear
		 * congruence equalities domains Compile Apron with the specific flag
		 * for PPL set to 1 in order to use such domain.
		 */
		PolkaGrid,

		/**
		 * Parma Polyhedra Library linear congruence equalities domain Compile
		 * Apron with the specific flag for PPL set to 1 in order to use such
		 * domain.
		 */
		PplGrid,

		/**
		 * The Parma Polyhedra library convex polyhedra domain Compile Apron
		 * with the specific flag for PPL set to 1 in order to use such domain.
		 */
		PplPoly
	}

	/**
	 * Initializes the static Apron manager with the specified numerical domain.
	 * * @param numericalDomain the desired abstract numerical domain.
	 * 
	 * @throws UnsupportedOperationException if the native library is missing,
	 *                                           or if a requested domain (like
	 *                                           PPL) is not compiled in the
	 *                                           current Apron installation.
	 */
	public static void setManager(
			ApronDomain numericalDomain) {
		if (!IS_AVAILABLE) {
			throw new UnsupportedOperationException(
					"Failed to set Apron manager: native library missing.");
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
		case PplGrid:
			try {
				manager = new PplGrid();
			} catch (LinkageError | Exception e) {
				throw new UnsupportedOperationException(
						"Failed to initialize PplGrid. Ensure the PPL library is installed and Apron was compiled with PPL support.",
						e);
			}
			break;
		case PplPoly:
			try {
				manager = new apron.PplPoly(false);
			} catch (LinkageError | Exception e) {
				throw new UnsupportedOperationException(
						"Failed to initialize PplPoly. Ensure the PPL library is installed and Apron was compiled with PPL support.",
						e);
			}
			break;
		default:
			throw new UnsupportedOperationException("Numerical domain " + numericalDomain + " unknown in Apron");
		}
	}

	/**
	 * Constructs a new Apron abstract domain instance representing the Top
	 * state.
	 * <p>
	 * If the manager has not been explicitly set via
	 * {@link #setManager(ApronDomain)}, the {@link ApronDomain#Box} domain is
	 * used by default. An initial environment is created containing a special
	 * return variable {@code <ret>}.
	 * </p>
	 *
	 * @throws UnsupportedOperationException if the native library is missing or
	 *                                           if the creation of the initial
	 *                                           state fails.
	 */
	public Apron() {
		if (!isAvailable()) {
			throw new UnsupportedOperationException("Failed to initialize Apron domain: native library missing.");
		}

		// If user doesn't set the manager, Box is used by default
		if (manager == null) {
			setManager(ApronDomain.Box);
		}

		try {
			String[] vars = { "<ret>" }; // Variable needed to represent the
			// returned value
			state = new Abstract1(manager, new apron.Environment(new String[0], vars));
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Internal constructor used to create a new instance wrapping an existing
	 * Apron native state.
	 *
	 * @param state the native {@link Abstract1} state to be wrapped.
	 */
	Apron(
			Abstract1 state) {
		this.state = state;
	}

	/**
	 * Performs an abstract assignment of an expression to an identifier.
	 * <p>
	 * This method first ensures mathematical safety by calling
	 * {@link #getConstraintsForDivision(Apron, ValueExpression, ProgramPoint, SemanticOracle)}
	 * to split the state into safe partitions where no division by zero can
	 * occur. Each safe partition is evaluated independently, and the final
	 * result is the Least Upper Bound of all successful assignments.
	 * </p>
	 *
	 * @param state      the current abstract state.
	 * @param id         the identifier to which the expression is assigned.
	 * @param expression the symbolic expression to evaluate.
	 * @param pp         the program point where the assignment occurs.
	 * @param oracle     the semantic oracle for inter-domain communication.
	 * 
	 * @return the updated abstract state after the assignment.
	 * 
	 * @throws SemanticException if an error occurs during the abstract
	 *                               evaluation.
	 */
	@Override
	public Apron assign(
			Apron state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

		Set<Apron> safeStates = getConstraintsForDivision(state, expression, pp, oracle);

		if (safeStates.isEmpty()) {
			return bottom();
		}

		Apron finalResult = null;

		for (Apron safeState : safeStates) {
			try {
				Environment env = safeState.state.getEnvironment();
				Var variable = toApronVar(id);
				Abstract1 newState;
				if (!env.hasVar(variable)) {
					Var[] vars = { variable };
					env = env.add(new Var[0], vars);
					newState = safeState.state.changeEnvironmentCopy(manager, env, false);
				} else {
					newState = safeState.state;
				}

				Texpr1Node apronExpression = toApronExpression(expression);

				if (apronExpression == null) {
					Apron forgot = forgetAbstractionOf(newState, id, pp, oracle);
					if (finalResult == null)
						finalResult = forgot;
					else
						finalResult = finalResult.lub(forgot);
					continue;
				}

				Var[] vars = apronExpression.getVars();
				for (Var var : vars) {
					if (!newState.getEnvironment().hasVar(var)) {
						Var[] vars1 = { var };
						env = newState.getEnvironment().add(new Var[0], vars1);
						newState = newState.changeEnvironmentCopy(manager, env, false);
					}
				}

				Apron assignedState = new Apron(newState.assignCopy(manager, variable,
						new Texpr1Intern(newState.getEnvironment(), apronExpression), null));

				if (finalResult == null) {
					finalResult = assignedState;
				} else {
					finalResult = finalResult.lub(assignedState);
				}

			} catch (ApronException e) {
				throw new UnsupportedOperationException("Apron library crashed", e);
			}
		}

		return finalResult != null ? finalResult : bottom();
	}

	/**
	 * Handles expressions that cannot be translated into Apron's tree
	 * expressions.
	 * <p>
	 * It resets the knowledge of the identifier and assumes it could be any
	 * value greater than or equal to zero, or less than or equal to zero,
	 * effectively making it Unknown while keeping it in the environment.
	 * </p>
	 */
	private Apron forgetAbstractionOf(
			Abstract1 newState,
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Apron result = new Apron(newState);
		result = result.forgetIdentifiers(java.util.Collections.singleton(id), pp);

		// Using Untyped.INSTANCE for do not depend on the frontend
		Constant zero = new Constant(Untyped.INSTANCE, 0, SyntheticLocation.INSTANCE);

		// Creation of the bin expr >= and <=
		BinaryExpression geExpr = new BinaryExpression(Untyped.INSTANCE, id, zero, ComparisonGe.INSTANCE,
				SyntheticLocation.INSTANCE);
		BinaryExpression leExpr = new BinaryExpression(Untyped.INSTANCE, id, zero, ComparisonLe.INSTANCE,
				SyntheticLocation.INSTANCE);

		// Call of the modified assume method
		// pp used as scr and dest because the method is forcing the assumption
		// on a node
		Apron ge = result.assume(result, geExpr, pp, pp, oracle);
		Apron le = result.assume(result, leExpr, pp, pp, oracle);
		return ge.lub(le);
	}

	/**
	 * Translates a LiSA {@link SymbolicExpression} into an Apron
	 * {@link Texpr1Node}.
	 * <p>
	 * This method recursively traverses the expression tree. It supports
	 * identifiers, constants and binary operations.
	 * </p>
	 *
	 * @param exp the LiSA symbolic expression.
	 * 
	 * @return the corresponding Apron tree expression node, or {@code null} if
	 *             translation is not possible.
	 * 
	 * @throws ApronException if the native library encounters an error during
	 *                            node creation.
	 */
	private Texpr1Node toApronExpression(
			SymbolicExpression exp)
			throws ApronException {
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

	/**
	 * Checks if a LiSA {@link BinaryOperator} has a direct mapping to an Apron
	 * operator.
	 *
	 * @param op the binary operator to check.
	 * 
	 * @return {@code true} if a mapping exists, {@code false} otherwise.
	 */
	private boolean canBeConvertedToApronOperator(
			BinaryOperator op) {
		return op == StringConcat.INSTANCE
				|| op == NumericNonOverflowingAdd.INSTANCE
				|| op == NumericNonOverflowingMul.INSTANCE
				|| op == NumericNonOverflowingDiv.INSTANCE
				|| op == NumericNonOverflowingSub.INSTANCE
				|| op == NumericNonOverflowingMod.INSTANCE
				|| op == NumericNonOverflowingRem.INSTANCE
				|| op == ComparisonEq.INSTANCE
				|| op == ComparisonNe.INSTANCE
				|| op == ComparisonGe.INSTANCE
				|| op == ComparisonGt.INSTANCE;
	}

	/**
	 * Maps a LiSA {@link BinaryOperator} to its corresponding Apron numerical
	 * or comparison operator code.
	 *
	 * @param op the LiSA binary operator.
	 * 
	 * @return the integer code representing the operator in Apron's
	 *             {@link Texpr1BinNode} or {@link Tcons1}.
	 * 
	 * @throws UnsupportedOperationException if the operator is not supported.
	 */
	private int toApronOperator(
			BinaryOperator op) {
		if (op == StringConcat.INSTANCE || op == NumericNonOverflowingAdd.INSTANCE)
			return Texpr1BinNode.OP_ADD;
		else if (op == NumericNonOverflowingMul.INSTANCE)
			return Texpr1BinNode.OP_MUL;
		else if (op == NumericNonOverflowingSub.INSTANCE)
			return Texpr1BinNode.OP_SUB;
		else if (op == NumericNonOverflowingDiv.INSTANCE)
			return Texpr1BinNode.OP_DIV;
		else if (op == NumericNonOverflowingMod.INSTANCE || op == NumericNonOverflowingRem.INSTANCE)
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

	/**
	 * Computes the small-step semantics of a given expression without actively
	 * applying numerical constraints to the abstract state's bounds.
	 * <p>
	 * In the context of the Apron domain, this method's primary responsibility
	 * is environment management. It traverses the expression to identify newly
	 * referenced variables and registers them within the native Apron
	 * environment, ensuring they are tracked for subsequent relational
	 * operations.
	 * </p>
	 *
	 * @param state      the current abstract state.
	 * @param expression the symbolic expression being evaluated.
	 * @param pp         the program point where the evaluation occurs.
	 * @param oracle     the semantic oracle for inter-domain communication.
	 * 
	 * @return a new {@link Apron} instance containing the updated environment.
	 * 
	 * @throws SemanticException if an error occurs during the semantic
	 *                               evaluation.
	 */
	@Override
	public Apron smallStepSemantics(
			Apron state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// the small-step semantics does not alter the state, but it should
		// add to the environment the identifiers produced by expression in
		// order to be
		// tracked by Apron

		if (expression instanceof Identifier) {
			Identifier id = (Identifier) expression;
			Environment env = state.state.getEnvironment();
			Var variable = toApronVar(id);
			if (!env.hasVar(variable)) {
				Var[] vars = { variable };
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

	/**
	 * Enforces a boolean condition on the current abstract state, restricting
	 * its numerical boundaries.
	 * <p>
	 * This method acts as a mathematical filter (e.g., during the evaluation of
	 * conditional branches like {@code if} or {@code while}). It translates the
	 * given logical expression into native Apron constraints ({@link Tcons1})
	 * and computes the intersection between the current state and these new
	 * constraints. If the condition is mathematically impossible in the current
	 * state, it returns the Bottom state.
	 * </p>
	 *
	 * @param state      the current abstract state.
	 * @param expression the boolean expression representing the condition to
	 *                       assume.
	 * @param src        the program point where the execution originates.
	 * @param dest       the targeted program point after the assumption.
	 * @param oracle     the semantic oracle for inter-domain communication.
	 * 
	 * @return a new restricted {@link Apron} state, or Bottom if the condition
	 *             is unsatisfiable.
	 * 
	 * @throws SemanticException if an error occurs during the constraint
	 *                               evaluation.
	 */
	@Override
	public Apron assume(
			Apron state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		try {
			if (state.state.isBottom(manager))
				return state;
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
		if (expression instanceof UnaryExpression) {
			UnaryExpression un = (UnaryExpression) expression;
			Operator op = un.getOperator();

			// Double neg
			if (op == LogicalNegation.INSTANCE) {
				ValueExpression inner = (ValueExpression) un.getExpression();
				if (inner instanceof UnaryExpression
						&& ((UnaryExpression) inner).getOperator() == LogicalNegation.INSTANCE) {
					// Passed src and dest instead pp

					return assume(state,
							((ValueExpression) ((UnaryExpression) inner).getExpression()).removeNegations(), src, dest,
							oracle);
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

			if (op == ComparisonNe.INSTANCE) {
				// A!=B -> (A<B) OR (A>B)
				BinaryExpression lt = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(),
						ComparisonLt.INSTANCE, bin.getCodeLocation());
				BinaryExpression gt = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(),
						ComparisonGt.INSTANCE, bin.getCodeLocation());
				BinaryExpression or = new BinaryExpression(bin.getStaticType(), lt, gt, LogicalOr.INSTANCE,
						bin.getCodeLocation());

				return assume(state, or, src, dest, oracle);
			}

			if (op == ComparisonEq.INSTANCE
					|| op == ComparisonGe.INSTANCE
					|| op == ComparisonGt.INSTANCE
					|| op == ComparisonLe.INSTANCE
					|| op == ComparisonLt.INSTANCE
			/* || op == ComparisonNe.INSTANCE */) {

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

	/**
	 * Checks whether the current abstract state tracks the specified
	 * identifier.
	 * <p>
	 * This method queries the native Apron environment to determine if the
	 * variable represented by the given identifier is currently defined and
	 * bounded within the state.
	 * </p>
	 *
	 * @param id the identifier to check.
	 * 
	 * @return {@code true} if the identifier is present in the environment,
	 *             {@code false} otherwise (or if the identifier is null).
	 */
	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		if (id == null || this.state == null)
			return false;
		try {
			String var = id.getName();
			return state.getEnvironment().hasVar(var);
		} catch (Exception e) {
			throw new UnsupportedOperationException(
					"Apron library crashed during knowsIdentifier for var: " + id.getName(), e);
		}
	}

	/**
	 * Translates a LiSA comparison expression into a native Apron numerical
	 * constraint.
	 * <p>
	 * The Apron library requires all constraints to be evaluated against zero
	 * (e.g., {@code Expr = 0} or {@code Expr >= 0}). This method algebraically
	 * rewrites standard comparisons to match this requirement. For instance,
	 * {@code x == y} is translated to {@code x - y == 0}, and strict
	 * inequalities like {@code x > y} are handled as {@code x - y - 1 >= 0}
	 * (assuming integer semantics).
	 * </p>
	 *
	 * @param state the current abstract state providing the environment.
	 * @param exp   the binary expression representing the comparison.
	 * 
	 * @return the equivalent Apron tree constraint node ({@link Tcons1}).
	 * 
	 * @throws ApronException                if the native library encounters an
	 *                                           error during constraint
	 *                                           creation.
	 * @throws UnsupportedOperationException if the comparison operator cannot
	 *                                           be mapped or translated.
	 */
	private Tcons1 toApronComparison(
			Apron state,
			BinaryExpression exp)
			throws ApronException {
		BinaryOperator op = exp.getOperator();

		// cases < and <= transformed into > and >= logic
		if (op == ComparisonLe.INSTANCE) {
			BinaryExpression newExpr = new BinaryExpression(exp.getStaticType(), exp.getRight(), exp.getLeft(),
					ComparisonGe.INSTANCE, exp.getCodeLocation());
			return toApronComparison(state, newExpr);
		} else if (op == ComparisonLt.INSTANCE) {
			BinaryExpression newExpr = new BinaryExpression(exp.getStaticType(), exp.getRight(), exp.getLeft(),
					ComparisonGt.INSTANCE, exp.getCodeLocation());
			return toApronComparison(state, newExpr);
		}

		// >, >= and == logic
		SymbolicExpression combinedExpr;
		int apronOp;

		if (op == ComparisonEq.INSTANCE) {
			// x == y -> x - y == 0
			combinedExpr = new BinaryExpression(
					exp.getStaticType(),
					exp.getLeft(),
					exp.getRight(),
					NumericNonOverflowingSub.INSTANCE,
					exp.getCodeLocation());
			apronOp = Tcons1.EQ;
		} else if (op == ComparisonGe.INSTANCE) {
			// x >= y -> x - y >= 0
			combinedExpr = new BinaryExpression(
					exp.getStaticType(), exp.getLeft(),
					exp.getRight(),
					NumericNonOverflowingSub.INSTANCE,
					exp.getCodeLocation());
			apronOp = Tcons1.SUPEQ;
		} else if (op == ComparisonGt.INSTANCE) {
			// Apron handle numbers as reals
			// x > y -> x >= y + 1 -> x - y - 1 >= 0
			Constant one = new Constant(
					Untyped.INSTANCE,
					1,
					exp.getCodeLocation());

			SymbolicExpression subExpr = new BinaryExpression(
					exp.getStaticType(),
					exp.getLeft(),
					exp.getRight(),
					NumericNonOverflowingSub.INSTANCE,
					exp.getCodeLocation());

			combinedExpr = new BinaryExpression(
					exp.getStaticType(),
					subExpr,
					one,
					NumericNonOverflowingSub.INSTANCE,
					exp.getCodeLocation());
			apronOp = Tcons1.SUPEQ;

		} else {
			// A != B handled in assume as A < B OR A > B
			throw new UnsupportedOperationException("Comparison operator " + op + " not supported");
		}

		Texpr1Node apronExpression = toApronExpression(combinedExpr);
		if (apronExpression != null) {
			return new Tcons1(state.state.getEnvironment(), apronOp, apronExpression);
		} else {
			throw new UnsupportedOperationException("Impossible to translate the expression: " + combinedExpr);
		}
	}

	/**
	 * Evaluates the satisfiability of a boolean expression within the current
	 * abstract state.
	 * <p>
	 * Unlike {@link #assume}, this method does not modify or restrict the
	 * state. It merely queries the underlying Apron domain to check if the
	 * given expression is definitively true (SATISFIED), definitively false
	 * (NOT_SATISFIED), or if the state lacks sufficient precision to decide
	 * (UNKNOWN).
	 * </p>
	 *
	 * @param state      the current abstract state.
	 * @param expression the boolean expression to evaluate.
	 * @param pp         the program point where the evaluation occurs.
	 * @param oracle     the semantic oracle for inter-domain communication.
	 * 
	 * @return a {@link Satisfiability} enum value representing the result of
	 *             the query.
	 * 
	 * @throws SemanticException if an error occurs during the evaluation.
	 */
	@Override
	public Satisfiability satisfies(
			Apron state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
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

				if (op == ComparisonNe.INSTANCE) {
					// A != B -> (A < B) OR (A > B)
					BinaryExpression lt = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(),
							ComparisonLt.INSTANCE, bin.getCodeLocation());
					BinaryExpression gt = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(),
							ComparisonGt.INSTANCE, bin.getCodeLocation());
					BinaryExpression or = new BinaryExpression(bin.getStaticType(), lt, gt, LogicalOr.INSTANCE,
							bin.getCodeLocation());

					return satisfies(state, or, pp, oracle);

				} else if (op == ComparisonEq.INSTANCE) {
					if (state.state.satisfy(manager, toApronComparison(state, bin)))
						return Satisfiability.SATISFIED;
					else {
						neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(),
								ComparisonNe.INSTANCE, bin.getCodeLocation());

						if (satisfies(state, neg, pp, oracle) == Satisfiability.SATISFIED)
							return Satisfiability.NOT_SATISFIED;

						return Satisfiability.UNKNOWN;
					}
				} else if (op == ComparisonGe.INSTANCE) {
					if (state.state.satisfy(manager, toApronComparison(state, bin)))
						return Satisfiability.SATISFIED;
					else {
						neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(),
								ComparisonLt.INSTANCE, bin.getCodeLocation());

						if (state.state.satisfy(manager, toApronComparison(state, neg)))
							return Satisfiability.NOT_SATISFIED;

						return Satisfiability.UNKNOWN;
					}
				} else if (op == ComparisonGt.INSTANCE) {
					if (state.state.satisfy(manager, toApronComparison(state, bin)))
						return Satisfiability.SATISFIED;
					else {
						neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(),
								ComparisonLe.INSTANCE, bin.getCodeLocation());

						if (state.state.satisfy(manager, toApronComparison(state, neg)))
							return Satisfiability.NOT_SATISFIED;

						return Satisfiability.UNKNOWN;
					}
				} else if (op == ComparisonLe.INSTANCE) {
					if (state.state.satisfy(manager, toApronComparison(state, bin)))
						return Satisfiability.SATISFIED;
					else {
						neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(),
								ComparisonGt.INSTANCE, bin.getCodeLocation());

						if (state.state.satisfy(manager, toApronComparison(state, neg)))
							return Satisfiability.NOT_SATISFIED;

						return Satisfiability.UNKNOWN;
					}
				} else if (op == ComparisonLt.INSTANCE) {
					if (state.state.satisfy(manager, toApronComparison(state, bin)))
						return Satisfiability.SATISFIED;
					else {
						neg = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(),
								ComparisonGe.INSTANCE, bin.getCodeLocation());

						if (state.state.satisfy(manager, toApronComparison(state, neg)))
							return Satisfiability.NOT_SATISFIED;

						return Satisfiability.UNKNOWN;
					}
				} else if (op == LogicalAnd.INSTANCE)
					return satisfies(state, (ValueExpression) bin.getLeft(), pp, oracle)
							.and(satisfies(state, (ValueExpression) bin.getRight(), pp, oracle));
				else if (op == LogicalOr.INSTANCE)
					return satisfies(state, (ValueExpression) bin.getLeft(), pp, oracle)
							.or(satisfies(state, (ValueExpression) bin.getRight(), pp, oracle));
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

	/**
	 * Computes the Least Upper Bound (LUB) of this state and another state.
	 * <p>
	 * This operation mathematically merges two abstract states. Since Apron
	 * domains are generally convex, the resulting state is the smallest convex
	 * geometric shape that fully encloses both input states. Before joining,
	 * the environments of both states are unified to their Least Common
	 * Environment.
	 * </p>
	 *
	 * @param other the other abstract state to join with.
	 * 
	 * @return a new {@link Apron} state representing the least upper bound.
	 * 
	 * @throws SemanticException if an error occurs during the join operation.
	 */
	@Override
	public Apron lub(
			Apron other)
			throws SemanticException {

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
				return new Apron(new Abstract1(manager, lubEnv));

			return new Apron(unifiedThis.joinCopy(manager, unifiedOther));
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Computes the Greatest Lower Bound (GLB) of this state and another state.
	 * <p>
	 * This operation calculates the mathematical intersection of the two
	 * abstract states. The resulting state satisfies the constraints of both
	 * input states simultaneously.
	 * </p>
	 *
	 * @param other the other abstract state to intersect with.
	 * 
	 * @return a new {@link Apron} state representing the greatest lower bound.
	 * 
	 * @throws SemanticException if an error occurs during the meet operation.
	 */
	@Override
	public Apron glb(
			Apron other)
			throws SemanticException {
		try {
			// we compute the least environment extending this and other
			// environment
			Environment lubEnv = state.getEnvironment().lce(other.state.getEnvironment());
			Abstract1 unifiedThis = state.changeEnvironmentCopy(manager, lubEnv, state.isBottom(manager));
			Abstract1 unifiedOther = other.state.changeEnvironmentCopy(manager, lubEnv, other.state.isBottom(manager));

			return new Apron(unifiedThis.meetCopy(manager, unifiedOther));
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Applies the widening operator between this state and another state.
	 * <p>
	 * Widening is crucial for ensuring the termination of the static analysis
	 * in the presence of loops. If an interval or geometric bound grows between
	 * iterations, the widening operator aggressively extrapolates it to
	 * infinity to quickly reach a fixed point.
	 * </p>
	 *
	 * @param other the abstract state from the subsequent iteration.
	 * 
	 * @return a new, widened {@link Apron} state.
	 * 
	 * @throws SemanticException if an error occurs during the widening
	 *                               operation.
	 */
	@Override
	public Apron widening(
			Apron other)
			throws SemanticException {
		try {
			Environment lubEnv = state.getEnvironment().lce(other.state.getEnvironment());
			Abstract1 unifiedThis = state.changeEnvironmentCopy(manager, lubEnv, state.isBottom(manager));
			Abstract1 unifiedOther = other.state.changeEnvironmentCopy(manager, lubEnv, other.state.isBottom(manager));

			if (unifiedOther.isBottom(manager))
				return new Apron(unifiedThis);
			if (unifiedThis.isBottom(manager))
				return new Apron(unifiedOther);

			return new Apron(unifiedThis.widening(manager, unifiedOther));
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Applies the narrowing operator between this state and another state.
	 * <p>
	 * Narrowing is typically used after a fixed point has been reached via
	 * widening, in order to safely refine the over-approximated bounds and
	 * recover lost precision. In this implementation, it defaults to the
	 * Greatest Lower Bound.
	 * </p>
	 *
	 * @param other the abstract state from the subsequent iteration.
	 * 
	 * @return a new, narrowed {@link Apron} state.
	 * 
	 * @throws SemanticException if an error occurs during the narrowing
	 *                               operation.
	 */
	@Override
	public Apron narrowing(
			Apron other)
			throws SemanticException {
		// Narrowing is the meet with next iteration's state
		try {
			if (this.isBottom()) {
				return this;
			}
			if (other.isBottom()) {
				return this;
			}
			return this.glb(other);

		} catch (Exception e) {
			throw new SemanticException("Apron error during narrowing() method:", e);
		}
	}

	/**
	 * Checks if this abstract state is included in (or equal to) another state.
	 * <p>
	 * This is the partial order check of the abstract lattice. Geometrically,
	 * it verifies if the shape represented by this state is completely
	 * contained within the shape of the {@code other} state. It is primarily
	 * used by the analyzer to determine if a fixed point has been reached.
	 * </p>
	 *
	 * @param other the state to compare against.
	 * 
	 * @return {@code true} if this state is less than or equal to the other
	 *             state, {@code false} otherwise.
	 * 
	 * @throws SemanticException if an error occurs during the inclusion check.
	 */
	@Override
	public boolean lessOrEqual(
			Apron other)
			throws SemanticException {
		try {
			if (state.isBottom(manager))
				return true;
			else if (other.state.isBottom(manager))
				return false;
			else if (other.state.isTop(manager))
				return true;
			else if (state.isTop(manager))
				return false;

			// we first need to uniform the environments
			Environment unifiedEnv = state.getEnvironment().lce(other.state.getEnvironment());

			Abstract1 unifiedOther = other.state.changeEnvironmentCopy(manager, unifiedEnv, false);
			Abstract1 unifiedThis = state.changeEnvironmentCopy(manager, unifiedEnv, false);

			return unifiedThis.isIncluded(manager, unifiedOther);
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Returns the Top element of the abstract domain.
	 * <p>
	 * The Top state represents total lack of information or maximum uncertainty
	 * (all variables can assume any possible value). It is the universal
	 * over-approximation.
	 * </p>
	 *
	 * @return a new {@link Apron} instance representing the Top state.
	 */
	@Override
	public Apron top() {
		try {
			return new Apron(new Abstract1(manager, new apron.Environment()));
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Returns the Bottom element of the abstract domain.
	 * <p>
	 * The Bottom state represents an unreachable program point or a
	 * mathematical contradiction (the state resulting from an impossible
	 * assumption or a guaranteed division by zero).
	 * </p>
	 *
	 * @return a new {@link Apron} instance representing the Bottom state.
	 */
	@Override
	public Apron bottom() {
		try {
			return new Apron(new Abstract1(manager, new apron.Environment(), true));
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Checks if the current abstract state is the Bottom state.
	 *
	 * @return {@code true} if this state represents unreachable code or a
	 *             contradiction, {@code false} otherwise.
	 */
	@Override
	public boolean isBottom() {
		try {
			// delegate the computation to apron
			return state.isBottom(manager);
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Checks if the current abstract state is the Top state.
	 *
	 * @return {@code true} if this state holds no specific constraints,
	 *             {@code false} otherwise.
	 */
	@Override
	public boolean isTop() {
		try {
			// delegate the computation to apron
			return state.isTop(manager);
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Converts a LiSA identifier into an Apron variable representation.
	 * <p>
	 * This utility method bridges the gap between LiSA's variable
	 * representation ({@link Identifier}) and Apron's native string-based
	 * variable format ({@link StringVar}).
	 * </p>
	 *
	 * @param id the LiSA identifier to convert.
	 * 
	 * @return the corresponding Apron {@link Var}.
	 */
	private Var toApronVar(
			Identifier id) {
		String n = id.getName();
		return new StringVar(n);
	}

	/**
	 * Checks if the given LiSA identifier is currently tracked within the
	 * abstract state's environment.
	 * <p>
	 * This method retrieves the active variables from the native Apron
	 * environment and verifies the presence of the translated identifier. It is
	 * often used to ensure a variable exists before attempting assignment or
	 * forgetting operations.
	 * </p>
	 *
	 * @param id the identifier to search for.
	 * 
	 * @return {@code true} if the identifier is part of the current
	 *             environment, {@code false} otherwise.
	 */
	public boolean containsIdentifier(
			Identifier id) {
		return Arrays.asList(state.getEnvironment().getVars()).contains(toApronVar(id));
	}

	public Abstract1 getApronState() {
		return state;
	}

	/**
	 * Projects the abstract state onto a smaller environment by forgetting a
	 * specific set of identifiers.
	 * <p>
	 * This operation is equivalent to existential quantification in logic. It
	 * safely removes the specified variables from the underlying Apron
	 * environment, which is essential for performance and precision when
	 * variables go out of scope.
	 * </p>
	 *
	 * @param ids the collection of identifiers to remove from the state.
	 * @param pp  the program point where the forgetting operation occurs.
	 * 
	 * @return a new {@link Apron} state without the specified identifiers.
	 * 
	 * @throws SemanticException if an error occurs during the operation.
	 */
	@Override
	public Apron forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
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

	/**
	 * Forgets a single identifier from the abstract state.
	 *
	 * @param id the identifier to remove.
	 * @param pp the program point where the forgetting operation occurs.
	 * 
	 * @return a new {@link Apron} state without the specified identifier.
	 * 
	 * @throws SemanticException if an error occurs during the operation.
	 */
	@Override
	public Apron forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		if (!containsIdentifier(id))
			return this;

		try {
			return new Apron(state.forgetCopy(manager, toApronVar(id), false));
		} catch (ApronException e) {
			throw new UnsupportedOperationException("Apron library crashed", e);
		}
	}

	/**
	 * Provides a structured, human-readable representation of the abstract
	 * state.
	 * <p>
	 * This representation is typically used for logging, debugging, or
	 * outputting the final results of the static analysis. It handles special
	 * cases like Top ("#TOP#") and Bottom ("_|_").
	 * </p>
	 *
	 * @return a {@link StructuredRepresentation} wrapping the string output of
	 *             the native Apron state.
	 */
	@Override
	public StructuredRepresentation representation() {
		if (isTop()) {
			return new StringRepresentation("#TOP#");
		}
		if (isBottom()) {
			return new StringRepresentation("_|_");
		}
		return new StringRepresentation(state.toString());
	}

	/**
	 * Stores the value of a source identifier into a target identifier.
	 * <p>
	 * This represents a variable-to-variable assignment that updates the
	 * environment and propagates existing bounds and relations to the new
	 * variable.
	 * </p>
	 *
	 * @param target the identifier that will receive the value.
	 * @param source the identifier holding the value to store.
	 * 
	 * @return a new {@link Apron} state reflecting the storage operation.
	 * 
	 * @throws SemanticException if an error occurs (e.g., if the source is
	 *                               missing from the environment).
	 */
	@Override
	public Apron store(
			Identifier target,
			Identifier source)
			throws SemanticException {
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
				throw new SemanticException("Error during store() method. Var '" + sourceName
						+ "' does not exist in the current environment");
			}

			// Tree expr for var source
			apron.Texpr1Intern expr = new apron.Texpr1Intern(env, new apron.Texpr1VarNode(sourceName));

			// target <- expr - must be in the env
			return new Apron(state.assignCopy(manager, targetName, expr, null));

		} catch (Exception e) {
			throw new SemanticException("Apron error during sotre() method:", e);
		}
	}

	/**
	 * Creates a new, default instance of the Apron abstract lattice.
	 *
	 * @return a new {@link Apron} instance.
	 */
	@Override
	public Apron makeLattice() {
		return new Apron();
	}

	/**
	 * Forgets all identifiers from the abstract state that satisfy a given
	 * predicate.
	 *
	 * @param test the predicate used to determine which identifiers should be
	 *                 removed.
	 * @param pp   the program point where the forgetting operation occurs.
	 * 
	 * @return a new {@link Apron} state with the matching identifiers removed.
	 * 
	 * @throws SemanticException if an error occurs during the operation.
	 */
	@Override
	public Apron forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
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
						pp.getLocation());

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

	/**
	 * Recursively traverses a value expression tree to identify and extract all
	 * sub-expressions that act as denominators.
	 * <p>
	 * This method inspects unary and binary operations, specifically looking
	 * for division, modulo, and remainder operators
	 * ({@link NumericNonOverflowingDiv}, {@link NumericNonOverflowingMod},
	 * {@link NumericNonOverflowingRem}). It isolates their right-hand operands
	 * to be later evaluated for division-by-zero risks.
	 * </p>
	 *
	 * @param expression the symbolic expression tree to explore.
	 * 
	 * @return a list of {@link ValueExpression} representing all found
	 *             denominators.
	 */
	private List<ValueExpression> extractDenominators(
			ValueExpression expression) {
		List<ValueExpression> denominators = new ArrayList<>();

		if (expression instanceof UnaryExpression) {
			denominators.addAll(extractDenominators((ValueExpression) ((UnaryExpression) expression).getExpression()));
		} else if (expression instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expression;
			denominators.addAll(extractDenominators((ValueExpression) bin.getLeft()));
			denominators.addAll(extractDenominators((ValueExpression) bin.getRight()));

			BinaryOperator op = bin.getOperator();
			if (op == NumericNonOverflowingDiv.INSTANCE
					|| op == NumericNonOverflowingMod.INSTANCE
					|| op == NumericNonOverflowingRem.INSTANCE) {
				denominators.add((ValueExpression) bin.getRight());
			}
		}
		return denominators;
	}

	/**
	 * Generates a set of mathematically safe abstract states where division by
	 * zero is strictly impossible for the given expression.
	 * <p>
	 * This method extracts all denominators from the expression and evaluates
	 * their bounds. If a denominator's abstract interval includes zero (i.e.,
	 * its minimum is &le; 0 and its maximum is &ge; 0), the method performs a
	 * safe state partition. It forces assumptions to create two distinct
	 * universes: one where the denominator is strictly negative ({@code < 0})
	 * and one where it is strictly positive ({@code > 0}). The process is
	 * iterative, producing a Cartesian product of safe states for expressions
	 * with multiple risky denominators.
	 * </p>
	 *
	 * @param initialState the initial abstract state before evaluation.
	 * @param expression   the complete symbolic expression containing potential
	 *                         divisions.
	 * @param pp           the current program point.
	 * @param oracle       the semantic oracle for cross-domain queries.
	 * 
	 * @return a set of safe {@link Apron} states. If a denominator is
	 *             guaranteed to be zero, the returned set will naturally filter
	 *             out the Bottom states, potentially returning empty.
	 * 
	 * @throws SemanticException if an error occurs during the assumption of the
	 *                               strict constraints.
	 */
	private Set<Apron> getConstraintsForDivision(
			Apron initialState,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

		Set<Apron> currentStates = new HashSet<>();
		currentStates.add(initialState);

		List<ValueExpression> denominators = extractDenominators(expression);

		if (denominators.isEmpty()) {
			return currentStates;
		}

		Constant zeroExp = new Constant(Untyped.INSTANCE, 0, expression.getCodeLocation());

		for (ValueExpression den : denominators) {
			Set<Apron> nextStates = new HashSet<>();

			for (Apron state : currentStates) {
				if (state.isBottom())
					continue;

				try {
					Texpr1Node denNode = toApronExpression(den);
					if (denNode != null) {
						apron.Interval denBound = state.state.getBound(manager,
								new Texpr1Intern(state.state.getEnvironment(), denNode));

						int inf = denBound.inf.sgn();
						int sup = denBound.sup.sgn();

						if (inf <= 0 && sup >= 0) {
							BinaryExpression ltZero = new BinaryExpression(
									den.getStaticType(), den, zeroExp, ComparisonLt.INSTANCE, den.getCodeLocation());
							BinaryExpression gtZero = new BinaryExpression(
									den.getStaticType(), den, zeroExp, ComparisonGt.INSTANCE, den.getCodeLocation());

							Apron assumedLt = assume(state, ltZero, pp, pp, oracle);
							Apron assumedGt = assume(state, gtZero, pp, pp, oracle);

							if (!assumedLt.isBottom()) {
								nextStates.add(assumedLt);
							}
							if (!assumedGt.isBottom()) {
								nextStates.add(assumedGt);
							}
						} else {
							// secure state
							nextStates.add(state);
						}
					} else {
						// non-translatable expr
						nextStates.add(state);
					}
				} catch (ApronException e) {
					throw new UnsupportedOperationException("Apron library crashed during division check", e);
				}
			}
			currentStates = nextStates;
		}
		return currentStates;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(state);
	}

	@Override
	public boolean equals(
			Object obj) {
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