package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionInverseSet;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The substring relational abstract domain, tracking relation between string
 * expressions. The domain is implemented as a {@link FunctionalLattice},
 * mapping identifiers to string expressions, tracking which string expressions
 * are <i>definitely</i> substring of an identifier. This domain follows the one
 * defined
 * <a href="https://link.springer.com/chapter/10.1007/978-3-030-94583-1_2">in
 * this paper</a>.
 * 
 * @author <a href="mailto:michele.martelli1@studenti.unipr.it">Michele
 *             Martelli</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class SubstringDomain extends FunctionalLattice<SubstringDomain, Identifier, ExpressionInverseSet>
		implements
		ValueDomain<SubstringDomain> {

	private static final SubstringDomain TOP = new SubstringDomain(new ExpressionInverseSet().top());
	private static final SubstringDomain BOTTOM = new SubstringDomain(new ExpressionInverseSet().bottom());

	/**
	 * Builds the top abstract value.
	 */
	public SubstringDomain() {
		this(new ExpressionInverseSet());
	}

	/**
	 * Builds the abstract value by cloning the given function.
	 * 
	 * @param lattice  the underlying lattice
	 * @param function the function to clone
	 */
	protected SubstringDomain(
			ExpressionInverseSet lattice,
			Map<Identifier, ExpressionInverseSet> function) {
		super(lattice, function);
	}

	private SubstringDomain(
			ExpressionInverseSet lattice) {
		super(lattice);
	}

	@Override
	public SubstringDomain lubAux(
			SubstringDomain other)
			throws SemanticException {
		return functionalLift(other, lattice.top(), this::glbKeys, (
				o1,
				o2) -> o1.lub(o2)).clear();
	}

	@Override
	public SubstringDomain glbAux(
			SubstringDomain other)
			throws SemanticException {
		return functionalLift(other, lattice.top(), this::lubKeys, (
				o1,
				o2) -> o1.glb(o2)).closure();
	}

	@Override
	public SubstringDomain top() {
		return isTop() ? this : TOP;
	}

	@Override
	public SubstringDomain bottom() {
		return isBottom() ? this : BOTTOM;
	}

	@Override
	public ExpressionInverseSet stateOfUnknown(
			Identifier key) {
		return lattice.top();
	}

	@Override
	public SubstringDomain mk(
			ExpressionInverseSet lattice,
			Map<Identifier, ExpressionInverseSet> function) {
		return new SubstringDomain(lattice.isBottom() ? lattice.bottom() : lattice.top(), function);
	}

	@Override
	public SubstringDomain assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

		/*
		 * If the assigned expression is not dynamically typed as a string (or
		 * untyped) return this.
		 */
		if (oracle != null && pp != null && oracle.getRuntimeTypesOf(expression, pp, oracle).stream()
				.allMatch(t -> !t.isStringType() && !t.isUntyped()))
			return this;

		/*
		 * The string type is unique and can be retrieved from the type system.
		 */
		Type strType;
		if (pp != null) // Correct: get the string type from the program point
			strType = pp.getProgram().getTypes().getStringType();
		else // Used in tests where pp is null, get the string type from the
				// expression
			strType = expression.getStaticType();

		Set<SymbolicExpression> expressions = extrPlus(expression, pp, oracle, strType);
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		result = result.remove(expressions, id);
		result = result.add(expressions, id);
		result = result.interasg(id, expression);
		result = result.closure(id);
		return result.clear();
	}

	@Override
	public SubstringDomain smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return this;
	}

	@Override
	public SubstringDomain assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {

		/*
		 * Assume only binary expressions
		 */
		if (expression instanceof BinaryExpression) {

			BinaryExpression binaryExpression = (BinaryExpression) expression;
			BinaryOperator binaryOperator = binaryExpression.getOperator();

			SymbolicExpression left = binaryExpression.getLeft();
			SymbolicExpression right = binaryExpression.getRight();

			/*
			 * The string type is unique and can be retrieved from the type
			 * system.
			 */
			Type strType;
			if (src != null) // Correct: get the string type from the program
								// point
				strType = src.getProgram().getTypes().getStringType();
			else // Used in tests where src is null, get the string type from
					// the expression
				strType = left.getStaticType();

			if (binaryOperator instanceof StringContains
					|| binaryOperator instanceof StringStartsWith
					|| binaryOperator instanceof StringEndsWith) {

				/*
				 * Evaluate only if the left operand is an identidier
				 */

				if (!(left instanceof Identifier))
					return this;

				if (!(right instanceof ValueExpression))
					throw new SemanticException("instanceof right");

				Set<SymbolicExpression> extracted = extrPlus((ValueExpression) right, src, oracle, strType);
				SubstringDomain result = mk(lattice, mkNewFunction(function, false));

				result = result.add(extracted, (Identifier) left);
				result = result.closure();
				return result.clear();

			} else if (binaryOperator instanceof StringEquals) {

				// case both operands are identifiers
				if ((left instanceof Identifier) && (right instanceof Identifier)) {
					SubstringDomain result = mk(lattice, mkNewFunction(function, false));
					result = result.add(left, (Identifier) right);
					result = result.add(right, (Identifier) left);
					result = result.closure();
					return result.clear();
				}
				// case where only one is an identifier
				else if ((left instanceof Identifier) || (right instanceof Identifier)) {
					if (right instanceof Identifier) {
						// make left the identifier
						SymbolicExpression temp = left;
						left = right;
						right = temp;
					}

					if (!(right instanceof ValueExpression))
						throw new SemanticException("instanceof right != ValueExpression.class");

					Set<SymbolicExpression> add = extrPlus((ValueExpression) right, src, oracle, strType);

					SubstringDomain result = mk(lattice, mkNewFunction(function, false));

					result = result.add(add, (Identifier) left);
					result = result.closure();
					return result.clear();

				}
			} else if (binaryOperator instanceof LogicalOr || binaryOperator instanceof LogicalAnd) {

				if (!(left instanceof ValueExpression) || !(right instanceof ValueExpression))
					throw new SemanticException(
							"!(left instanceof ValueExpression) || !(right instanceof ValueExpression)");

				ValueExpression rightValueExpression = (ValueExpression) right;
				ValueExpression leftValueExpression = (ValueExpression) left;
				SubstringDomain leftDomain = assume(leftValueExpression, src, dest, oracle);
				SubstringDomain rightDomain = assume(rightValueExpression, src, dest, oracle);

				if (binaryOperator instanceof LogicalOr) {
					return leftDomain.lub(rightDomain).clear();
				} else {
					return leftDomain.glb(rightDomain).clear();
				}
			}

		}

		return this;
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		if (id == null || function == null)
			return false;

		if (function.containsKey(id))
			return true;

		for (Map.Entry<Identifier, ExpressionInverseSet> entry : function.entrySet()) {
			for (SymbolicExpression expr : entry.getValue().elements) {
				if (appears(id, expr))
					return true;
			}
		}

		return false;
	}

	@Override
	public SubstringDomain forgetIdentifier(
			Identifier id)
			throws SemanticException {
		if (!knowsIdentifier(id))
			return this;

		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false); // function
																							// !=
																							// null
		newFunction.remove(id);
		newFunction.replaceAll((
				key,
				value) -> removeFromSet(value, id));

		return mk(lattice, newFunction);
	}

	@Override
	public SubstringDomain forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		if (function == null || function.keySet().isEmpty())
			return this;

		// Get all identifiers
		Set<Identifier> ids = new HashSet<>();
		for (Map.Entry<Identifier, ExpressionInverseSet> entry : function.entrySet()) {
			ids.add(entry.getKey());
			for (SymbolicExpression s : entry.getValue().elements()) {
				if (s instanceof Identifier) {
					Identifier id = (Identifier) s;
					ids.add(id);
				}
			}
		}

		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		for (Identifier id : ids) {
			// Test each identifier
			if (test.test(id)) {
				result = result.forgetIdentifier(id);
			}
		}

		return result;
	}

	@Override
	public Satisfiability satisfies(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

		if (isBottom() || !(expression instanceof BinaryExpression))
			return Satisfiability.UNKNOWN;

		BinaryExpression binaryExpression = (BinaryExpression) expression;
		BinaryOperator binaryOperator = binaryExpression.getOperator();

		SymbolicExpression left = binaryExpression.getLeft();
		SymbolicExpression right = binaryExpression.getRight();

		if (binaryOperator instanceof StringContains) {
			if (!(left instanceof Variable))
				return Satisfiability.UNKNOWN;

			return getState((Identifier) left).contains(right) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
		} else if (binaryOperator instanceof StringEquals || binaryOperator instanceof StringEndsWith
				|| binaryOperator instanceof StringStartsWith) {
			if (!(left instanceof Variable) || !(right instanceof Variable))
				return Satisfiability.UNKNOWN;

			return (getState((Identifier) left).contains(right)) && (getState((Identifier) right).contains(left))
					? Satisfiability.SATISFIED
					: Satisfiability.UNKNOWN;
		} else if (binaryOperator instanceof LogicalOr) {
			if (!(left instanceof ValueExpression) || !(right instanceof ValueExpression))
				throw new SemanticException(
						"!(left instanceof ValueExpression) || !(right instanceof ValueExpression)");
			Satisfiability leftSatisfiability = satisfies((ValueExpression) left, pp, oracle);

			if (leftSatisfiability.equals(Satisfiability.SATISFIED))
				return Satisfiability.SATISFIED;

			Satisfiability rightSatisfiability = satisfies((ValueExpression) right, pp, oracle);

			return rightSatisfiability;

		} else if (binaryOperator instanceof LogicalAnd) {
			if (!(left instanceof ValueExpression) || !(right instanceof ValueExpression))
				throw new SemanticException(
						"!(left instanceof ValueExpression) || !(right instanceof ValueExpression)");
			Satisfiability leftSatisfiability = satisfies((ValueExpression) left, pp, oracle);
			Satisfiability rightSatisfiability = satisfies((ValueExpression) right, pp, oracle);

			if (leftSatisfiability.equals(Satisfiability.SATISFIED)
					&& rightSatisfiability.equals(Satisfiability.SATISFIED))
				return Satisfiability.SATISFIED;
			else
				return Satisfiability.UNKNOWN;
		}

		return Satisfiability.UNKNOWN;

	}

	@Override
	public SubstringDomain pushScope(
			ScopeToken token)
			throws SemanticException {
		return new SubstringDomain(lattice.pushScope(token), mkNewFunction(function, true));
	}

	@Override
	public SubstringDomain popScope(
			ScopeToken token)
			throws SemanticException {
		return new SubstringDomain(lattice.popScope(token), mkNewFunction(function, true));
	}

	/*
	 * Extract the ordered expressions of a concatenation. If the expression is
	 * not a concatenation returns an empty list. Example: {@code x + y + "ab"}
	 * returns {@code x, y, "ab"} as List
	 * @param expression to extract
	 * @return List containing the sub-expressions
	 */
	private static List<ValueExpression> extr(
			ValueExpression expression) {

		List<ValueExpression> result = new ArrayList<>();
		if (expression instanceof BinaryExpression
				&& ((BinaryExpression) expression).getOperator() instanceof StringConcat) {
			BinaryExpression binaryExpression = (BinaryExpression) expression;

			ValueExpression left = (ValueExpression) binaryExpression.getLeft();
			ValueExpression right = (ValueExpression) binaryExpression.getRight();

			result.addAll(extr(left));
			result.addAll(extr(right));
		} else
			result.add(expression);

		return result;
	}

	/*
	 * Returns all the possible substring of a given expression.
	 * @param expression
	 * @param strType
	 * @return
	 */
	private Set<SymbolicExpression> extrPlus(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle,
			Type strType)
			throws SemanticException {
		List<ValueExpression> extracted = extr(expression);

		/*
		 * If there are more than 2 consecutive constants, merge them into a
		 * single constant
		 */
		List<List<ValueExpression>> sub_expressions = split(extracted, pp, oracle, strType);

		for (int i = 0; i < sub_expressions.size(); i++) {
			List<ValueExpression> list = sub_expressions.get(i);
			sub_expressions.set(i, mergeStringLiterals(list, strType));
		}

		Set<SymbolicExpression> result = new HashSet<>();

		// Iterate sublists splitted from StringReplace expressions
		for (List<ValueExpression> list : sub_expressions) {
			// Iterate over the length of the expression
			for (int l = 1; l <= list.size(); l++) {
				// Iterate from the start to the end according to the size of
				// the
				// expression to create
				for (int i = 0; i <= (list.size() - l); i++) {
					List<ValueExpression> subList = list.subList(i, i + l);
					result.add(composeExpression(subList));

					if (subList.size() == 1 && subList.get(0) instanceof Constant) {
						/*
						 * Case the expression to ass is a single constant ->
						 * add its substrings
						 */
						Set<Constant> substrings = getSubstrings((Constant) subList.get(0));

						for (Constant substring : substrings) {
							List<ValueExpression> newList = new ArrayList<>();
							newList.add(substring);

							result.add(composeExpression(newList));

						}
					} else if (subList.get(0) instanceof Constant) {
						/*
						 * Case the first expression of the expression to add is
						 * a constant -> add an expression for each suffix of
						 * the constant
						 */
						Set<Constant> suffixes = getSuffix((Constant) subList.get(0));

						for (Constant suffix : suffixes) {
							List<ValueExpression> newList = new ArrayList<>(subList);
							newList.set(0, suffix);

							if (subList.get(subList.size() - 1) instanceof Constant) {

								/*
								 * Case the last expression of the expression to
								 * add is a constant -> add an expression for
								 * each prefix of the constant
								 */
								Set<Constant> prefixes = getPrefix((Constant) subList.get(subList.size() - 1));

								for (Constant prefix : prefixes) {
									newList.set(newList.size() - 1, prefix);

									result.add(composeExpression(newList));
								}
							} else
								result.add(composeExpression(newList));

						}
					} else if (subList.get(subList.size() - 1) instanceof Constant) {

						/*
						 * Case the last expression of the expression to add is
						 * a constant -> add an expression for each prefix of
						 * the constant
						 */

						Set<Constant> prefixes = getPrefix((Constant) subList.get(subList.size() - 1));

						for (Constant prefix : prefixes) {
							List<ValueExpression> newList = new ArrayList<>(subList);
							newList.set(newList.size() - 1, prefix);

							result.add(composeExpression(newList));
						}
					}

				}
			}
		}
		return new HashSet<>(result);
	}

	private List<List<ValueExpression>> split(
			List<ValueExpression> extracted,
			ProgramPoint pp,
			SemanticOracle oracle,
			Type strType)
			throws SemanticException {
		List<List<ValueExpression>> result = new ArrayList<>();

		List<ValueExpression> temp = new ArrayList<>();

		for (ValueExpression s : extracted) {
			if (!(s instanceof Identifier || s instanceof Constant)) {
				if (s instanceof TernaryExpression) {
					TernaryExpression ternaryExpression = (TernaryExpression) s;

					// strrep(c1, c2, c3) where c1 or c2 or c3 can be a
					// concatenation of constants
					if (ternaryExpression.getOperator() instanceof StringReplace
							&& ternaryExpression.getLeft() instanceof ValueExpression
							&& ternaryExpression.getMiddle() instanceof ValueExpression
							&& ternaryExpression.getRight() instanceof ValueExpression
							&& hasOnlyConstants((ValueExpression) ternaryExpression.getLeft())
							&& hasOnlyConstants((ValueExpression) ternaryExpression.getMiddle())
							&& hasOnlyConstants((ValueExpression) ternaryExpression.getRight())) {
						// left, middle, right are only constants or
						// concatenation of constants

						List<ValueExpression> left = extr((ValueExpression) ternaryExpression.getLeft());
						left = mergeStringLiterals(left, strType);
						if (left.size() != 1 && !(left.get(0) instanceof Constant))
							throw new SemanticException("unexpected");

						Constant leftConstant = (Constant) left.get(0);

						List<ValueExpression> middle = extr((ValueExpression) ternaryExpression.getMiddle());
						middle = mergeStringLiterals(middle, strType);
						if (middle.size() != 1 && !(middle.get(0) instanceof Constant))
							throw new SemanticException("unexpected");

						Constant middleConstant = (Constant) middle.get(0);

						List<ValueExpression> right = extr((ValueExpression) ternaryExpression.getRight());
						right = mergeStringLiterals(right, strType);
						if (right.size() != 1 && !(right.get(0) instanceof Constant))
							throw new SemanticException("unexpected");

						Constant rightConstant = (Constant) right.get(0);

						String s1 = leftConstant.getValue() instanceof String ? (String) leftConstant.getValue()
								: leftConstant.getValue().toString();
						String s2 = middleConstant.getValue() instanceof String ? (String) middleConstant.getValue()
								: middleConstant.getValue().toString();
						String s3 = rightConstant.getValue() instanceof String ? (String) rightConstant.getValue()
								: rightConstant.getValue().toString();

						temp.add(new Constant(strType, s1.replace(s2, s3), SyntheticLocation.INSTANCE));
						continue;
					}

					// strsub(s1, i1, i2) where s1 can be a concatenation of
					// constants
					if (ternaryExpression.getOperator() instanceof StringSubstring
							&& ternaryExpression.getLeft() instanceof ValueExpression
							&& ternaryExpression.getMiddle() instanceof Constant
							&& ternaryExpression.getRight() instanceof Constant
							&& hasOnlyConstants((ValueExpression) ternaryExpression.getLeft())) {
						List<ValueExpression> left = extr((ValueExpression) ternaryExpression.getLeft());
						left = mergeStringLiterals(left, strType);
						if (left.size() != 1 && !(left.get(0) instanceof Constant))
							throw new SemanticException("unexpected");

						Constant leftConstant = (Constant) left.get(0);

						String s1 = leftConstant.getValue() instanceof String ? (String) leftConstant.getValue()
								: leftConstant.getValue().toString();
						Integer i1 = (Integer) ((Constant) ternaryExpression.getMiddle()).getValue();
						Integer i2 = (Integer) ((Constant) ternaryExpression.getRight()).getValue();

						temp.add(new Constant(strType, s1.substring(i1, i2), SyntheticLocation.INSTANCE));
						continue;
					}

				}

				// Split list
				result.add(new ArrayList<>(temp));
				temp.clear();

			} else {
				// s instance of Identifier || s instance of Constant
				temp.add(s);
			}
		}

		result.add(temp);

		return result;
	}

	/*
	 * Returns true iff the parameter is a Constant or a concatenation of
	 * Constant values
	 */
	private boolean hasOnlyConstants(
			ValueExpression expr) {
		if (expr instanceof Constant)
			return true;

		if (expr instanceof BinaryExpression && ((BinaryExpression) expr).getOperator() instanceof StringConcat) {
			BinaryExpression be = (BinaryExpression) expr;
			if (!(be.getLeft() instanceof ValueExpression && be.getRight() instanceof ValueExpression))
				return false;

			return hasOnlyConstants((ValueExpression) be.getLeft())
					&& hasOnlyConstants((ValueExpression) be.getRight());
		}

		return false;
	}

	/*
	 * Returns a list with no more than one consecutive constant where if {@code
	 * extracted} has consecutive constants, the returned value merges them
	 * @param extracted List to analyze
	 * @param strType
	 * @return The list without consecutive constants.
	 */
	private static List<ValueExpression> mergeStringLiterals(
			List<ValueExpression> extracted,
			Type strType) {
		List<ValueExpression> result = new ArrayList<>();
		StringBuilder recent = new StringBuilder();

		for (ValueExpression expr : extracted) {
			if (expr instanceof Constant) {
				// Update the string builder value adding the value of the
				// constant
				Constant c = (Constant) expr;
				recent.append(c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString());

			} else {
				/*
				 * Iterating a Variable
				 */
				if (recent.length() > 0) {
					// The previous value / values are constants; add to the
					// list the constant with value the built string
					result.add(new Constant(strType, recent.toString(), SyntheticLocation.INSTANCE));
					// Clear the string builder
					recent.delete(0, recent.length());
				}
				// Add the variable
				result.add(expr);
			}
		}

		// If the last element is a constant add the last built string
		if (recent.length() > 0)
			result.add(new Constant(strType, recent.toString(), SyntheticLocation.INSTANCE));

		return result;
	}

	/*
	 * Returns the set containing the substrings of a Constant
	 * @param c Constant to analyze
	 * @return The set containing the substrings of a Constant
	 */
	private static Set<Constant> getSubstrings(
			Constant c) {
		Set<Constant> result = new HashSet<>();

		String str = c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString();

		// Iterate over the length of the resulting string
		for (int l = 1; l < str.length(); l++) {
			// Iterate over the starting char
			for (int i = 0; i <= str.length() - l; i++) {
				Constant substring = new Constant(c.getStaticType(), str.substring(i, i + l),
						SyntheticLocation.INSTANCE);

				result.add(substring);
			}
		}

		return result;
	}

	/*
	 * Returns the set containing the prefixes of a Constant
	 * @param c Constant to analyze
	 * @return The set containing the prefixes of a Constant
	 */
	private static Set<Constant> getPrefix(
			Constant c) {
		Set<Constant> result = new HashSet<>();

		String str = c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString();
		// Iterate over the length
		for (int i = 1; i <= str.length(); i++) {
			Constant prefix = new Constant(c.getStaticType(), str.substring(0, i), SyntheticLocation.INSTANCE);

			result.add(prefix);
		}

		return result;
	}

	/*
	 * Returns the set containing the suffixes of a Constant
	 * @param c Constant to analyze
	 * @return The set containing the suffixes of a Constant
	 */
	private static Set<Constant> getSuffix(
			Constant c) {
		Set<Constant> result = new HashSet<>();

		String str = c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString();
		int length = str.length();
		// Iterate over the length
		for (int i = 1; i <= length; i++) {
			Constant suffix = new Constant(c.getStaticType(), str.substring(length - i, length),
					SyntheticLocation.INSTANCE);

			result.add(suffix);
		}

		return result;
	}

	/*
	 * Creates am expression given a list. The returned expression is the
	 * ordered concatenation of the expression in the list.
	 * @param expressions
	 * @return The expression representing concatenation of the expressions in
	 * the list.
	 */
	private static SymbolicExpression composeExpression(
			List<ValueExpression> expressions) {
		if (expressions.size() == 1)
			return expressions.get(0);

		return new BinaryExpression(expressions.get(0).getStaticType(), expressions.get(0),
				composeExpression(expressions.subList(1, expressions.size())), StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
	}

	/**
	 * Adds a set of expressions ({@code exprs}) for {@code id} to the this
	 * abstract value.
	 * 
	 * @param exprs the set of symbolic expressions
	 * @param id    the identifier
	 * 
	 * @return a new abstract value with the added expressions
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected SubstringDomain add(
			Set<SymbolicExpression> exprs,
			Identifier id)
			throws SemanticException {

		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		// Don't add the expressions that contain the key variable (ex: x -> x,
		// x -> x + y will not be added)
		Set<SymbolicExpression> expressionsToAdd = new HashSet<>();
		for (SymbolicExpression se : exprs) {
			if (!appears(id, se))
				expressionsToAdd.add(se);
		}

		if (expressionsToAdd.isEmpty())
			return this;

		ExpressionInverseSet newSet = new ExpressionInverseSet(expressionsToAdd);

		if (!(newFunction.get(id) == null))
			newSet = newSet.glb(newFunction.get(id));

		newFunction.put(id, newSet);

		return mk(lattice, newFunction);
	}

	/**
	 * Adds a single expression ({@code expr}) for {@code id} to the this
	 * abstract value.
	 * 
	 * @param expr the symbolic expression
	 * @param id   the identifier
	 * 
	 * @return a new abstract value with the added expression
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected SubstringDomain add(
			SymbolicExpression expr,
			Identifier id)
			throws SemanticException {
		Set<SymbolicExpression> expression = new HashSet<>();
		expression.add(expr);

		return add(expression, id);
	}

	/*
	 * First step of assignment, removing obsolete relations.
	 * @param extracted Expression assigned
	 * @param id Expression getting assigned
	 * @return Copy of the domain, with the holding relations after the
	 * assignment.
	 * @throws SemanticException if an error occurs during the computation
	 */
	private SubstringDomain remove(
			Set<SymbolicExpression> extracted,
			Identifier id) {
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		// If assignment is similar to x = x + ..., then we keep current
		// relations to x, otherwise we remove them.
		if (!extracted.contains(id)) {
			newFunction.remove(id);
		}

		// Remove relations containing id from the other entries
		for (Map.Entry<Identifier, ExpressionInverseSet> entry : newFunction.entrySet()) {
			ExpressionInverseSet newSet = removeFromSet(entry.getValue(), id);

			entry.setValue(newSet);
		}

		return mk(lattice, newFunction);
	}

	/*
	 * Returns a set without expressions containing id starting from source
	 */
	private static ExpressionInverseSet removeFromSet(
			ExpressionInverseSet source,
			Identifier id) {
		Set<SymbolicExpression> newSet = source.elements.stream()
				.filter(element -> !appears(id, element))
				.collect(Collectors.toSet());

		return newSet.isEmpty() ? new ExpressionInverseSet().top() : new ExpressionInverseSet(newSet);
	}

	/**
	 * Performs the inter-assignment phase
	 * 
	 * @param assignedId         Variable getting assigned
	 * @param assignedExpression Expression assigned
	 * 
	 * @return Copy of the domain with new relations following the
	 *             inter-assignment phase
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	private SubstringDomain interasg(
			Identifier assignedId,
			SymbolicExpression assignedExpression)
			throws SemanticException {
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		if (!knowsIdentifier(assignedId))
			return this;

		for (Map.Entry<Identifier, ExpressionInverseSet> entry : function.entrySet()) {
			// skip same entry
			if (entry.getKey().equals(assignedId))
				continue;

			if (entry.getValue().contains(assignedExpression)) {
				Set<SymbolicExpression> newRelation = new HashSet<>();
				newRelation.add(assignedId);

				ExpressionInverseSet newSet = newFunction.get(entry.getKey())
						.glb(new ExpressionInverseSet(newRelation));
				newFunction.put(entry.getKey(), newSet);
			}

		}

		return mk(lattice, newFunction);
	}

	/**
	 * Performs the closure over an identifier. The method adds to {@code id}
	 * the expressions found in the variables mapped to {@code id}
	 * 
	 * @return A copy of the domain with the added relations
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	private SubstringDomain closure(
			Identifier id)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		ExpressionInverseSet set = result.getState(id);

		for (SymbolicExpression se : set) {
			if (se instanceof Variable) {
				Variable variable = (Variable) se;

				// Variable found --> add the relations of variable to id

				if (result.knowsIdentifier(variable)) {
					Set<SymbolicExpression> add = new HashSet<>(result.getState(variable).elements);
					result = result.add(add, id);
				}
			}
		}

		return result;
	}

	/**
	 * Performs the closure over the domain.
	 * 
	 * @return A copy of the domain with the added relations
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	protected SubstringDomain closure() throws SemanticException {
		if (isTop() || isBottom())
			return this;

		SubstringDomain prev;
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		do {
			prev = mk(lattice, mkNewFunction(result.function, false));
			Set<Identifier> set = prev.function.keySet();

			for (Identifier id : set) {
				// Perform the closure on every identifier of the domain
				result = result.closure(id);
			}

			result = result.clear();

			// Some relations may be added; check that close is applied
			// correctly to the added relations
		} while (!prev.equals(result));

		return result;

	}

	private SubstringDomain clear() throws SemanticException {
		Map<Identifier, ExpressionInverseSet> newMap = mkNewFunction(function, false);

		newMap.entrySet().removeIf(entry -> entry.getValue().isTop());

		return new SubstringDomain(lattice, newMap);
	}

	private static boolean appears(
			Identifier id,
			SymbolicExpression expr) {
		if (expr instanceof Identifier)
			return id.equals(expr);

		if (expr instanceof Constant)
			return false;

		if (expr instanceof BinaryExpression) {
			BinaryExpression expression = (BinaryExpression) expr;
			SymbolicExpression left = expression.getLeft();
			SymbolicExpression right = expression.getRight();

			return appears(id, left) || appears(id, right);
		}

		return false;
	}

}
