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
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SubstringDomain extends FunctionalLattice<SubstringDomain, Identifier, ExpressionInverseSet>
		implements
		ValueDomain<SubstringDomain> {
	
	private static final SubstringDomain TOP = new SubstringDomain(new ExpressionInverseSet().top());
	private static final SubstringDomain BOTTOM = new SubstringDomain(new ExpressionInverseSet().bottom());

	public SubstringDomain(
			ExpressionInverseSet lattice,
			Map<Identifier, ExpressionInverseSet> function) {
		super(lattice, function);
	}

	public SubstringDomain(
			ExpressionInverseSet lattice) {
		super(lattice);
	}

	public SubstringDomain() {
		this(new ExpressionInverseSet());
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

		Set<SymbolicExpression> identifiers = extrPlus(expression, strType);

		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		result = result.remove(identifiers, id);

		result = result.add(identifiers, id);

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

		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

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

				Set<SymbolicExpression> extracted = extrPlus((ValueExpression) right, strType);

				result = result.add(extracted, (Identifier) left);

				result = result.closure();

			} else if (binaryOperator instanceof StringEquals) {
				/*
				 * Case both operands are identifiers
				 */
				if ((left instanceof Identifier) && (right instanceof Identifier)) {
					result = result.add(extrPlus((ValueExpression) left, strType), (Identifier) right);
					result = result.add(extrPlus((ValueExpression) right, strType), (Identifier) left);

					result = result.closure();
				}

				/*
				 * Case where only one is an identifier
				 */
				else if ((left instanceof Identifier) || (right instanceof Identifier)) {

					if (right instanceof Identifier) {
						/*
						 * Make left the identifier
						 */
						SymbolicExpression temp = left;
						left = right;
						right = temp;
					}

					if (!(right instanceof ValueExpression))
						throw new SemanticException("instanceof right != ValueExpression.class");

					Set<SymbolicExpression> add = extrPlus((ValueExpression) right, strType);

					result = result.add(add, (Identifier) left);

					result = result.closure();

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
					result = leftDomain.lub(rightDomain);
				} else {
					result = leftDomain.glb(rightDomain);
				}
			}

		}

		return result.clear();
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		if (id == null || function == null)
			return false;

		if (function.containsKey(id))
			return true;

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

		return mk(lattice, newFunction);
	}

	@Override
	public SubstringDomain forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		if (function == null || function.keySet().isEmpty())
			return this;

		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false); // function
																							// !=
																							// null

		Set<Identifier> keys = newFunction.keySet().stream().filter(test::test).collect(Collectors.toSet());

		keys.forEach(newFunction::remove);

		return mk(lattice, newFunction);
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
	 * 
	 * @param expression to extract
	 * 
	 * @return List containing the sub-expressions
	 */
	private static List<SymbolicExpression> extr(
			ValueExpression expression) {

		List<SymbolicExpression> result = new ArrayList<>();
		if (expression instanceof BinaryExpression) {
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			BinaryOperator binaryOperator = binaryExpression.getOperator();
			if (!(binaryOperator instanceof StringConcat))
				return Collections.emptyList();

			ValueExpression left = (ValueExpression) binaryExpression.getLeft();
			ValueExpression right = (ValueExpression) binaryExpression.getRight();

			result.addAll(extr(left));
			result.addAll(extr(right));
		} else if (expression instanceof Variable || expression instanceof Constant) {
			result.add(expression);
		}

		return result;
	}

	/*
	 * Returns all the possible substring of a given expression.
	 * 
	 * @param expression
	 * @param strType
	 * 
	 * @return
	 */
	private static Set<SymbolicExpression> extrPlus(
			ValueExpression expression,
			Type strType) {
		List<SymbolicExpression> extracted = extr(expression);

		/*
		 * If there are more than 2 consecutive constants, merge them into a
		 * single constant
		 */
		extracted = mergeStringLiterals(extracted, strType);

		Set<SymbolicExpression> result = new HashSet<>();

		// Iterate over the length of the expression
		for (int l = 1; l <= extracted.size(); l++) {
			// Iterate from the start to the end according to the size of the
			// expression to create
			for (int i = 0; i <= (extracted.size() - l); i++) {
				List<SymbolicExpression> subList = extracted.subList(i, i + l);
				result.add(composeExpression(subList));

				if (subList.size() == 1 && subList.get(0) instanceof Constant) {
					/*
					 * Case the expression to ass is a single constant -> add
					 * its substrings
					 */
					Set<SymbolicExpression> substrings = getSubstrings((Constant) subList.get(0));

					for (SymbolicExpression substring : substrings) {
						List<SymbolicExpression> newList = new ArrayList<>();
						newList.add(substring);

						result.add(composeExpression(newList));

					}
				} else if (subList.get(0) instanceof Constant) {
					/*
					 * Case the first expression of the expression to add is a
					 * constant -> add an expression for each suffix of the
					 * constant
					 */
					Set<SymbolicExpression> suffixes = getSuffix((Constant) subList.get(0));

					for (SymbolicExpression suffix : suffixes) {
						List<SymbolicExpression> newList = new ArrayList<>(subList);
						newList.set(0, suffix);

						if (subList.get(subList.size() - 1) instanceof Constant) {

							/*
							 * Case the last expression of the expression to add
							 * is a constant -> add an expression for each
							 * prefix of the constant
							 */
							Set<SymbolicExpression> prefixes = getPrefix((Constant) subList.get(subList.size() - 1));

							for (SymbolicExpression prefix : prefixes) {
								newList.set(newList.size() - 1, prefix);

								result.add(composeExpression(newList));
							}
						} else
							result.add(composeExpression(newList));

					}
				} else if (subList.get(subList.size() - 1) instanceof Constant) {

					/*
					 * Case the last expression of the expression to add is a
					 * constant -> add an expression for each prefix of the
					 * constant
					 */

					Set<SymbolicExpression> prefixes = getPrefix((Constant) subList.get(subList.size() - 1));

					for (SymbolicExpression prefix : prefixes) {
						List<SymbolicExpression> newList = new ArrayList<>(subList);
						newList.set(newList.size() - 1, prefix);

						result.add(composeExpression(newList));
					}
				}

			}
		}

		return new HashSet<>(result);
	}

	/*
	 * Returns a list with no more than one consecutive constant where if
	 * {@code extracted} has consecutive constants, the returned value merges
	 * them
	 * 
	 * @param extracted List to analyze
	 * @param strType
	 * 
	 * @return The list without consecutive constants.
	 */
	private static List<SymbolicExpression> mergeStringLiterals(
			List<SymbolicExpression> extracted,
			Type strType) {
		List<SymbolicExpression> result = new ArrayList<>();
		StringBuilder recent = new StringBuilder();

		for (SymbolicExpression expr : extracted) {
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
	 * 
	 * @param c Constant to analyze
	 * 
	 * @return The set containing the substrings of a Constant
	 */
	private static Set<SymbolicExpression> getSubstrings(
			Constant c) {
		Set<SymbolicExpression> result = new HashSet<>();

		String str = c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString();

		// Iterate over the length of the resulting string
		for (int l = 1; l < str.length(); l++) {
			// Iterate over the starting char
			for (int i = 0; i <= str.length() - l; i++) {
				ValueExpression substring = new Constant(c.getStaticType(), str.substring(i, i + l),
						SyntheticLocation.INSTANCE);

				result.add(substring);
			}
		}

		return result;
	}

	/*
	 * Returns the set containing the prefixes of a Constant
	 * 
	 * @param c Constant to analyze
	 * 
	 * @return The set containing the prefixes of a Constant
	 */
	private static Set<SymbolicExpression> getPrefix(
			Constant c) {
		Set<SymbolicExpression> result = new HashSet<>();

		String str = c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString();
		// Iterate over the length
		for (int i = 1; i <= str.length(); i++) {
			ValueExpression prefix = new Constant(c.getStaticType(), str.substring(0, i), SyntheticLocation.INSTANCE);

			result.add(prefix);
		}

		return result;
	}

	/*
	 * Returns the set containing the suffixes of a Constant
	 * 
	 * @param c Constant to analyze
	 * 
	 * @return The set containing the suffixes of a Constant
	 */
	private static Set<SymbolicExpression> getSuffix(
			Constant c) {
		Set<SymbolicExpression> result = new HashSet<>();

		String str = c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString();
		int length = str.length();
		// Iterate over the length
		for (int i = 1; i <= length; i++) {
			ValueExpression suffix = new Constant(c.getStaticType(), str.substring(length - i, length),
					SyntheticLocation.INSTANCE);

			result.add(suffix);
		}

		return result;
	}

	/*
	 * Creates am expression given a list. The returned expression is the
	 * ordered concatenation of the expression in the list.
	 * 
	 * @param expressions
	 * 
	 * @return The expression representing concatenation of the expressions in
	 *             the list.
	 */
	private static SymbolicExpression composeExpression(
			List<SymbolicExpression> expressions) {
		if (expressions.size() == 1)
			return expressions.get(0);

		return new BinaryExpression(expressions.get(0).getStaticType(), expressions.get(0),
				composeExpression(expressions.subList(1, expressions.size())), StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
	}

	/*
	 * Adds a set of expressions to the domain.
	 * 
	 * @param symbolicExpressions
	 * @param id
	 * 
	 * @return A new domain with the added expressions
	 * 
	 * @throws SemanticException
	 */
	protected SubstringDomain add(
			Set<SymbolicExpression> symbolicExpressions,
			Identifier id)
			throws SemanticException {

		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		// Don't add the expressions that contain the key variable (ex: x -> x,
		// x -> x + y will not be added)
		Set<SymbolicExpression> expressionsToRemove = new HashSet<>();
		for (SymbolicExpression se : symbolicExpressions) {
			if (!appears(id, se))
				expressionsToRemove.add(se);
		}

		if (expressionsToRemove.isEmpty())
			return this;

		ExpressionInverseSet newSet = new ExpressionInverseSet(expressionsToRemove);

		if (!(newFunction.get(id) == null))
			newSet = newSet.glb(newFunction.get(id));

		newFunction.put(id, newSet);

		return mk(lattice, newFunction);
	}

	/*
	 * First step of assignment, removing obsolete relations.
	 * 
	 * @param extracted Expression assigned
	 * @param id        Expression getting assigned
	 * 
	 * @return Copy of the domain, with the holding relations after the
	 *             assignment.
	 * 
	 * @throws SemanticException
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
			Set<SymbolicExpression> newSet = entry.getValue().elements.stream()
					.filter(element -> !appears(id, element))
					.collect(Collectors.toSet());

			ExpressionInverseSet value = newSet.isEmpty() ? new ExpressionInverseSet().top()
					: new ExpressionInverseSet(newSet);

			entry.setValue(value);

		}

		return mk(lattice, newFunction);
	}

	/*
	 * Performs the inter-assignment phase
	 * 
	 * @param assignedId         Variable getting assigned
	 * @param assignedExpression Expression assigned
	 * 
	 * @return Copy of the domain with new relations following the
	 *             inter-assignment phase
	 * 
	 * @throws SemanticException
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

	/*
	 * Performs the closure over an identifier. The method adds to {@code id}
	 * the expressions found in the variables mapped to {@code id}
	 * 
	 * @return A copy of the domain with the added relations
	 * 
	 * @throws SemanticException
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

	/*
	 * Performs the closure over the domain.
	 * 
	 * @return A copy of the domain with the added relations
	 * 
	 * @throws SemanticException
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

	/*
	 * Removes values mapped to empty set (top)
	 * 
	 * @return A copy of the domain without variables mapped to empty set
	 * 
	 * @throws SemanticException
	 */
	private SubstringDomain clear() throws SemanticException {
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		Map<Identifier, ExpressionInverseSet> iterate = mkNewFunction(result.function, false);
		for (Map.Entry<Identifier, ExpressionInverseSet> entry : iterate.entrySet()) {
			if (entry.getValue().isTop()) {
				result = result.forgetIdentifier(entry.getKey());
			}

		}

		return result;
	}

	/*
	 * Checks if a variable appears in an expression
	 * 
	 * @param id   Variable to check
	 * @param expr expression to analyze
	 * 
	 * @return {@code true} if {@code id} appears, {@code false} otherwise
	 */
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
