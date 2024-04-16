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
import it.unive.lisa.type.StringType;
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
		return isTop() ? this : new SubstringDomain(lattice.top());
	}

	@Override
	public SubstringDomain bottom() {
		return isBottom() ? this : new SubstringDomain(lattice.bottom());
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

		/**
		 * If the assigned expression is not dynamically typed as a string (or untyped) return this.
		 */
		if (oracle.getRuntimeTypesOf(expression, pp, oracle).stream().allMatch(t -> !t.isStringType() && !t.isUntyped()))
			return this;
		
		/*
		 * The string type is unique and can be retrieved from the type system.
		 */
		Type strType = pp.getProgram().getTypes().getStringType();
		
		Set<SymbolicExpression> identifiers = extrPlus(expression, strType);

		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		result = result.remove(identifiers, id);

		result = result.add(identifiers, id);

		result = result.interasg(id, expression);

		result = result.closure();

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

		StringType strType = src.getProgram().getTypes().getStringType();
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		if (expression instanceof BinaryExpression) {

			BinaryExpression binaryExpression = (BinaryExpression) expression;
			BinaryOperator binaryOperator = binaryExpression.getOperator();

			SymbolicExpression left = binaryExpression.getLeft();
			SymbolicExpression right = binaryExpression.getRight();

			if (binaryOperator instanceof StringContains
					|| binaryOperator instanceof StringStartsWith
					|| binaryOperator instanceof StringEndsWith) {

				if (!(left instanceof Identifier))
					return this;

				if (!(right instanceof ValueExpression))
					throw new SemanticException("instanceof right");

				Set<SymbolicExpression> extracted = extrPlus((ValueExpression) right, strType);

				result = result.add(extracted, (Identifier) left);

				result = result.closure();

			} else if (binaryOperator instanceof StringEquals) {
				// both are identifiers
				if ((left instanceof Identifier) && (right instanceof Identifier)) {
					result = result.add(extrPlus((ValueExpression) left, strType), (Identifier) right);
					result = result.add(extrPlus((ValueExpression) right, strType), (Identifier) left);

					result = result.closure();
				} // one is identifier
				else if ((left instanceof Identifier) || (right instanceof Identifier)) {

					if (right instanceof Identifier) { // left instance of
														// Identifier, right
														// SymbolicExpression
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
			// *******
			if (!(left instanceof Variable) || !(right instanceof Variable))
				return Satisfiability.UNKNOWN;

			return (getState((Identifier) left).contains(right)) && (getState((Identifier) right).contains(left))
					? Satisfiability.SATISFIED
					: Satisfiability.UNKNOWN;
			// ******
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

	private static List<SymbolicExpression> extr(
			ValueExpression expression)
			throws SemanticException {

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

	private static Set<SymbolicExpression> extrPlus(
			ValueExpression expression,
			Type strType)
			throws SemanticException {
		List<SymbolicExpression> extracted = extr(expression);

		extracted = mergeStringLiterals(extracted, strType);

		Set<SymbolicExpression> result = new HashSet<>();

		for (int l = 1; l <= extracted.size(); l++) {
			for (int i = 0; i <= (extracted.size() - l); i++) {
				List<SymbolicExpression> subList = extracted.subList(i, i + l);
				result.add(composeExpression(subList));

				if (subList.size() == 1 && subList.get(0) instanceof Constant) {
					Set<SymbolicExpression> substrings = getSubstrings((Constant) subList.get(0));

					for (SymbolicExpression substring : substrings) {
						List<SymbolicExpression> newList = new ArrayList<>();
						newList.add(substring);

						result.add(composeExpression(newList));

					}
				} else if (subList.get(0) instanceof Constant) {
					Set<SymbolicExpression> suffixes = getSuffix((Constant) subList.get(0));

					for (SymbolicExpression suffix : suffixes) {
						List<SymbolicExpression> newList = new ArrayList<>(subList);
						newList.set(0, suffix);

						if (subList.get(subList.size() - 1) instanceof Constant) {
							Set<SymbolicExpression> prefixes = getPrefix((Constant) subList.get(subList.size() - 1));

							for (SymbolicExpression prefix : prefixes) {
								newList.set(newList.size() - 1, prefix);

								result.add(composeExpression(newList));
							}
						} else
							result.add(composeExpression(newList));

					}
				} else if (subList.get(subList.size() - 1) instanceof Constant) {
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

	private static List<SymbolicExpression> mergeStringLiterals(
			List<SymbolicExpression> extracted,
			Type strType) {
		List<SymbolicExpression> result = new ArrayList<>();
		StringBuilder recent = new StringBuilder();

		for (SymbolicExpression expr : extracted) {
			if (expr instanceof Constant) {
				Constant c = (Constant) expr;
				recent.append(c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString());

			} else {
				if (!recent.isEmpty()) {
					result.add(new Constant(strType, recent.toString(), SyntheticLocation.INSTANCE));
					recent.delete(0, recent.length());
				}
				result.add(expr);
			}
		}

		if (!recent.isEmpty())
			result.add(new Constant(strType, recent.toString(), SyntheticLocation.INSTANCE));

		return result;
	}

	private static Set<SymbolicExpression> getSubstrings(
			Constant c) {
		Set<SymbolicExpression> result = new HashSet<>();

		String str = c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString();
		for (int l = 1; l < str.length(); l++) {
			for (int i = 0; i <= str.length() - l; i++) {
				ValueExpression substring = new Constant(c.getStaticType(), str.substring(i, i + l),
						SyntheticLocation.INSTANCE);

				result.add(substring);
			}
		}

		return result;
	}

	private static Set<SymbolicExpression> getPrefix(
			Constant c) {
		Set<SymbolicExpression> result = new HashSet<>();

		String str = c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString();
		for (int i = 1; i <= str.length(); i++) {
			ValueExpression prefix = new Constant(c.getStaticType(), str.substring(0, i), SyntheticLocation.INSTANCE);

			result.add(prefix);
		}

		return result;
	}

	private static Set<SymbolicExpression> getSuffix(
			Constant c) {
		Set<SymbolicExpression> result = new HashSet<>();

		String str = c.getValue() instanceof String ? (String) c.getValue() : c.getValue().toString();
		int length = str.length();
		for (int i = 1; i <= length; i++) {
			ValueExpression suffix = new Constant(c.getStaticType(), str.substring(length - i, length),
					SyntheticLocation.INSTANCE);

			result.add(suffix);
		}

		return result;
	}

	private static SymbolicExpression composeExpression(
			List<SymbolicExpression> expressions) {
		if (expressions.size() == 1)
			return expressions.get(0);

		return new BinaryExpression(expressions.get(0).getStaticType(), expressions.get(0),
				composeExpression(expressions.subList(1, expressions.size())), StringConcat.INSTANCE,
				SyntheticLocation.INSTANCE);
	}

	private SubstringDomain add(
			Set<SymbolicExpression> symbolicExpressions,
			Identifier id)
			throws SemanticException {

		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		Set<SymbolicExpression> expressionsToRemove = new HashSet<>();
		for (SymbolicExpression se : symbolicExpressions) {
			if (!appears(id, se))
				expressionsToRemove.add(se);
		}
		// symbolicExpressions.remove(id);

		if (expressionsToRemove.isEmpty())
			return this;

		ExpressionInverseSet newSet = new ExpressionInverseSet(expressionsToRemove);

		if (!(newFunction.get(id) == null))
			newSet = newSet.glb(newFunction.get(id));

		newFunction.put(id, newSet);

		return mk(lattice, newFunction);
	}

	private SubstringDomain remove(
			Set<SymbolicExpression> extracted,
			Identifier id)
			throws SemanticException {
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		if (!extracted.contains(id)) { // x = x + ..... --> keep relations for x
			newFunction.remove(id);
		}

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

	private SubstringDomain interasg(
			Identifier assignedId,
			SymbolicExpression assignedExpression)
			throws SemanticException {
		Map<Identifier, ExpressionInverseSet> newFunction = mkNewFunction(function, false);

		if (!knowsIdentifier(assignedId))
			return this;

		for (Map.Entry<Identifier, ExpressionInverseSet> entry : function.entrySet()) {
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

	private SubstringDomain closure(
			Identifier id)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;

		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		ExpressionInverseSet toModify;
		ExpressionInverseSet iterate;

		do {
			toModify = result.getState(id);
			iterate = result.getState(id);

			for (SymbolicExpression se : toModify) {
				if (se instanceof Variable) {
					Variable variable = (Variable) se;

					if (result.knowsIdentifier(variable)) {
						Set<SymbolicExpression> add = new HashSet<>(result.getState(variable).elements);
						result = result.add(add, id);
					}
				}
			}
		} while (!iterate.equals(toModify));

		return result;
	}

	private SubstringDomain closure() throws SemanticException {
		if (isTop() || isBottom())
			return this;

		SubstringDomain prev;
		SubstringDomain result = mk(lattice, mkNewFunction(function, false));

		do {
			prev = mk(lattice, mkNewFunction(result.function, false));
			Set<Identifier> set = prev.function.keySet();

			for (Identifier id : set) {
				result = result.closure(id);
			}

			result = result.clear();
		} while (!prev.equals(result));

		return result;

	}

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
