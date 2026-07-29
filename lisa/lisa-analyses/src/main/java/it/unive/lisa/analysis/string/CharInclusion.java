package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.value.StringAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.IntegerConstant;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringCharAt;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringEqualsIgnoreCase;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringIsPrefixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIsSuffixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringMatches;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplaceAll;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplaceFirst;
import it.unive.lisa.symbolic.value.operator.ternary.StringStartsWithFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.StringReverse;
import it.unive.lisa.symbolic.value.operator.unary.StringToLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.StringToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.StringTrim;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * The character inclusion abstract domain.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio
 *             Salvatore Evola</a>
 *
 * @see <a href=
 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
 */
public class CharInclusion
		implements
		StringAbstraction<ValueEnvironment<CharInclusion.CI>>,
		SmashedSumStringDomain<CharInclusion.CI> {

	/**
	 * A lattice structure tracking characters that are surely included in a
	 * string, and characters that might be included in a string.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @see <a href=
	 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
	 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
	 */
	public static class CI
			implements
			BaseLattice<CI> {

		private static final CI TOP = new CI();

		private static final CI BOTTOM = new CI(null, null);

		private final Set<Character> certainlyContained;

		private final Set<Character> maybeContained;

		/**
		 * Builds the top char inclusion abstract element.
		 */
		public CI() {
			this(new TreeSet<>(), null);
		}

		/**
		 * Builds a char inclusion abstract element.
		 *
		 * @param certainlyContained the set of certainly contained characters
		 * @param maybeContained     the set of maybe contained characters
		 */
		public CI(
				Set<Character> certainlyContained,
				Set<Character> maybeContained) {
			this.certainlyContained = certainlyContained;
			this.maybeContained = maybeContained;
		}

		private CI(
				String str) {
			Set<Character> charsSet = str.chars()
					.mapToObj(e -> (char) e)
					.collect(Collectors.toCollection(TreeSet::new));
			this.certainlyContained = charsSet;
			this.maybeContained = charsSet;
		}

		@Override
		public CI lubAux(
				CI other)
				throws SemanticException {
			Set<Character> lubAuxCertainly = new TreeSet<>();

			Set<Character> lubAuxMaybe;
			if (maybeContained == null || other.maybeContained == null)
				lubAuxMaybe = null;
			else {
				lubAuxMaybe = new TreeSet<>();
				lubAuxMaybe.addAll(maybeContained);
				lubAuxMaybe.addAll(other.maybeContained);
			}

			for (Character certainlyContainedChar : this.certainlyContained)
				if (other.certainlyContained.contains(certainlyContainedChar))
					lubAuxCertainly.add(certainlyContainedChar);

			return new CI(lubAuxCertainly, lubAuxMaybe);
		}

		@Override
		public boolean lessOrEqualAux(
				CI other)
				throws SemanticException {
			if (this.certainlyContained.size() > other.certainlyContained.size())
				return false;
			if (!other.certainlyContained.containsAll(certainlyContained))
				return false;
			if (other.maybeContained == null)
				return true;
			if (maybeContained == null)
				return false;
			if (this.maybeContained.size() > other.maybeContained.size())
				return false;
			return other.maybeContained.containsAll(this.maybeContained);
		}

		@Override
		public boolean equals(
				Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			CI that = (CI) o;
			return Objects.equals(certainlyContained, that.certainlyContained)
					&& Objects.equals(maybeContained, that.maybeContained);
		}

		@Override
		public int hashCode() {
			return Objects.hash(certainlyContained, maybeContained);
		}

		@Override
		public CI top() {
			return TOP;
		}

		@Override
		public CI bottom() {
			return BOTTOM;
		}

		@Override
		public StructuredRepresentation representation() {
			if (isBottom())
				return Lattice.bottomRepresentation();
			if (isTop())
				return Lattice.topRepresentation();

			return new StringRepresentation(formatRepresentation());
		}

		/**
		 * Yields the set of certainly contained characters of this abstract
		 * value.
		 *
		 * @return the set of certainly contained characters of this abstract
		 *             value.
		 */
		public Set<Character> getCertainlyContained() {
			return this.certainlyContained;
		}

		/**
		 * Yields the set of maybe contained characters of this abstract value.
		 *
		 * @return the set of maybe contained characters of this abstract value,
		 *             or {@code null} if the whole alphabet might be part of
		 *             the string
		 */
		public Set<Character> getMaybeContained() {
			return this.maybeContained;
		}

		/**
		 * Checks whether this char inclusion abstract value models the empty
		 * string, i.e., the sets of the maybe and certainly contained are both
		 * empty.
		 *
		 * @return whether this char inclusion abstract value models the empty
		 *             string
		 */
		private boolean isEmptyString() {
			return (maybeContained != null && maybeContained.isEmpty()) && certainlyContained.isEmpty();
		}

		private String formatRepresentation() {
			return "CertainlyContained: {"
					+ StringUtils.join(this.certainlyContained, ", ")
					+ "}, MaybeContained: {"
					+ (maybeContained == null ? "Σ" : StringUtils.join(this.maybeContained, ", "))
					+ "}";
		}

		@Override
		public String toString() {
			return representation().toString();
		}

	}

	/**
	 * The integer domain that we use to process numerical constraints.
	 */
	private final IntegerConstantPropagation intDomain = new IntegerConstantPropagation();

	@Override
	public CI evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String)
			return new CI(constant.getValue().toString());

		return CI.TOP;
	}

	@Override
	public CI evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumStringDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public CI evalUnaryExpression(
			UnaryExpression expression,
			CI arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis() && operator == NumericToString.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, expression, pp);
			return generate(constraints, pp, oracle);
		}

		if (arg.isTop())
			return top();

		if (operator == StringReverse.INSTANCE)
			return arg;
		else if (operator == StringToLowerCase.INSTANCE)
			return new CI(
					arg.certainlyContained.stream().map(Character::toLowerCase).collect(Collectors.toSet()),
					arg.maybeContained == null
							? null
							: arg.maybeContained.stream().map(Character::toLowerCase).collect(Collectors.toSet()));
		else if (operator == StringToUpperCase.INSTANCE)
			return new CI(
					arg.certainlyContained.stream().map(Character::toUpperCase).collect(Collectors.toSet()),
					arg.maybeContained == null
							? null
							: arg.maybeContained.stream().map(Character::toUpperCase).collect(Collectors.toSet()));
		else if (operator == StringTrim.INSTANCE)
			return arg;

		return CI.TOP;
	}

	@Override
	public CI evalBinaryExpression(
			BinaryExpression expression,
			CI left,
			CI right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis()
				&& (operator == StringCharAt.INSTANCE
						|| operator == StringSubstringToEnd.INSTANCE)) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntegerConstant val = intDomain.generate(constraints, pp, oracle);
			if (val.isBottom())
				return CI.BOTTOM;
			// we do not know where the substring will begin,
			// so all characters are possibly included but none for sure
			Set<Character> all = new TreeSet<>(left.certainlyContained);
			if (left.maybeContained != null)
				all.addAll(left.maybeContained);
			return new CI(new TreeSet<>(), all);
		}

		if (operator == StringConcat.INSTANCE) {
			Set<Character> resultCertainlyContained = new TreeSet<>(left.certainlyContained);
			resultCertainlyContained.addAll(right.certainlyContained);

			Set<Character> resultMaybeContained;
			if (left.maybeContained == null || right.maybeContained == null)
				resultMaybeContained = null;
			else {
				resultMaybeContained = new TreeSet<>(left.maybeContained);
				resultMaybeContained.addAll(right.maybeContained);
			}

			return new CI(resultCertainlyContained, resultMaybeContained);
		}

		return CI.TOP;
	}

	@Override
	public CI evalTernaryExpression(
			TernaryExpression expression,
			CI left,
			CI middle,
			CI right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();

		if (left.isTop())
			return CI.TOP;

		if (oracle.hasWholeValueAnlysis() && operator == StringSubstring.INSTANCE) {
			Set<BinaryExpression> cM = oracle.constraints(this, (ValueExpression) expression.getMiddle(), pp);
			IntegerConstant mid = intDomain.generate(cM, pp, oracle);
			Set<BinaryExpression> cR = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntegerConstant rig = intDomain.generate(cR, pp, oracle);
			if (mid.isBottom() || rig.isBottom())
				return CI.BOTTOM;
			// we do not know where the substring will begin,
			// so all characters are possibly included but none for sure
			Set<Character> all = new TreeSet<>(left.certainlyContained);
			if (left.maybeContained != null)
				all.addAll(left.maybeContained);
			return new CI(new TreeSet<>(), all);
		}

		if (right.isTop() || middle.isTop())
			return CI.TOP;

		if (operator == StringReplace.INSTANCE || operator == StringReplaceAll.INSTANCE) {
			if (!left.certainlyContained.containsAll(middle.certainlyContained))
				// no replace for sure
				return left;

			Set<Character> included = new TreeSet<>(left.certainlyContained);
			Set<Character> possibly = new TreeSet<>(left.maybeContained);
			// since we do not know if the replace will happen, we move
			// everything to the possibly included characters
			included.removeAll(middle.certainlyContained);
			possibly.addAll(middle.certainlyContained);

			included.removeAll(middle.maybeContained);
			Set<Character> tmp = new TreeSet<>(middle.maybeContained);
			// just the ones that we removed before
			tmp.retainAll(left.certainlyContained);
			possibly.addAll(tmp);

			// add the second string
			possibly.addAll(right.certainlyContained);
			possibly.addAll(right.maybeContained);

			return new CI(included, possibly);
		}

		if (operator == StringReplaceFirst.INSTANCE) {
			// since the replacement happens through a regex, we cannot be sure
			// that the replacement will happen or what will be replaced so all
			// characters are possibly included but none for sure
			Set<Character> all = new TreeSet<>(left.certainlyContained);
			if (left.maybeContained != null)
				all.addAll(left.maybeContained);
			all.addAll(right.certainlyContained);
			if (right.maybeContained != null)
				all.addAll(right.maybeContained);
			return new CI(new TreeSet<>(), all);
		}

		return CI.TOP;

	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			CI left,
			CI right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		Boolean b;
		if (right.isEmptyString())
			if (operator == ComparisonEq.INSTANCE)
				b = left.isEmptyString() ? true : null;
			else if (operator == ComparisonNe.INSTANCE)
				b = left.isEmptyString() ? false : null;
			else if (operator == StringContains.INSTANCE)
				b = true;
			else if (operator == StringEndsWith.INSTANCE)
				b = true;
			else if (operator == StringEquals.INSTANCE)
				b = left.isEmptyString() ? true : null;
			else if (operator == StringEqualsIgnoreCase.INSTANCE)
				b = left.isEmptyString() ? true : null;
			else if (operator == StringMatches.INSTANCE)
				b = true;
			else if (operator == StringStartsWith.INSTANCE)
				b = true;
			else if (operator == StringIsPrefixOf.INSTANCE)
				b = left.isEmptyString() ? true : null;
			else if (operator == StringIsSuffixOf.INSTANCE)
				b = left.isEmptyString() ? true : null;
			else
				return Satisfiability.UNKNOWN;
		else if (left.isEmptyString())
			if (operator == ComparisonEq.INSTANCE)
				b = right.isEmptyString() ? true : null;
			else if (operator == ComparisonNe.INSTANCE)
				b = right.isEmptyString() ? false : null;
			else if (operator == StringContains.INSTANCE)
				b = false;
			else if (operator == StringEndsWith.INSTANCE)
				b = false;
			else if (operator == StringEquals.INSTANCE)
				b = right.isEmptyString() ? true : null;
			else if (operator == StringEqualsIgnoreCase.INSTANCE)
				b = right.isEmptyString() ? true : null;
			else if (operator == StringMatches.INSTANCE)
				b = null;
			else if (operator == StringStartsWith.INSTANCE)
				b = false;
			else if (operator == StringIsPrefixOf.INSTANCE)
				b = right.isEmptyString() ? true : null;
			else if (operator == StringIsSuffixOf.INSTANCE)
				b = right.isEmptyString() ? true : null;
			else
				return Satisfiability.UNKNOWN;
		else
			return Satisfiability.UNKNOWN;

		if (b == null)
			return Satisfiability.UNKNOWN;
		return Satisfiability.fromBoolean(b);
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			CI left,
			CI middle,
			CI right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || middle.isTop())
			return Satisfiability.UNKNOWN;

		if (oracle.hasWholeValueAnlysis() && expression.getOperator() == StringStartsWithFromIndex.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntegerConstant val = intDomain.generate(constraints, pp, oracle);
			if (val.isBottom())
				return Satisfiability.BOTTOM;
			if (middle.isEmptyString())
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}

		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<CI> assumeBinaryExpression(
			ValueEnvironment<CI> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		BinaryOperator operator = expression.getOperator();
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (operator == ComparisonEq.INSTANCE) {
			if (left instanceof Identifier) {
				if (!canProcess(right, src, oracle))
					// the expression does not have a string value, we do not
					// assume anything on it
					return environment;
				CI eval = eval(environment, right, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				// If eval is TOP, the rhs is unknown. Any abstract value of lhs
				// satisfies lhs == TOP, so the lhs abstract value is preserved
				// and no refinement is needed.
				if (!eval.isTop())
					return environment.putState((Identifier) left, eval);
			} else if (right instanceof Identifier) {
				if (!canProcess(left, src, oracle))
					// the expression does not have a string value, we do not
					// assume anything on it
					return environment;
				CI eval = eval(environment, left, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				// Same reasoning as above, symmetric case.
				if (!eval.isTop())
					return environment.putState((Identifier) right, eval);
			}
		}
		return environment;
	}

	@Override
	public CI substring(
			CI current,
			long begin,
			long end) {
		if (current.isTop() || current.isBottom())
			return current;
		return new CI(new TreeSet<>(), current.maybeContained);
	}

	@Override
	public IntInterval length(
			CI current) {
		return new IntInterval(new MathNumber(current.certainlyContained.size()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			CI current,
			CI other) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			CI current,
			char c) {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;
		if (current.certainlyContained.contains(c))
			return Satisfiability.SATISFIED;
		else if (current.maybeContained == null || current.maybeContained.contains(c))
			return Satisfiability.UNKNOWN;
		else
			return Satisfiability.NOT_SATISFIED;
	}

	@Override
	public CI top() {
		return CI.TOP;
	}

	@Override
	public CI bottom() {
		return CI.BOTTOM;
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<CI> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Collections.emptySet();
		if (state.isBottom())
			return null;

		if ((e instanceof UnaryExpression && ((UnaryExpression) e).getOperator() == LogicalNegation.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalAnd.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalOr.INSTANCE)) {
			Satisfiability sat = satisfies(state, e, pp, oracle);
			if (sat == Satisfiability.SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
			else if (sat == Satisfiability.NOT_SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
			else if (sat == Satisfiability.UNKNOWN)
				return Collections.emptySet();
			else
				return null;
		}

		if (e instanceof UnaryExpression) {
			UnaryOperator operator = ((UnaryExpression) e).getOperator();
			if (operator == StringLength.INSTANCE) {
				ValueExpression arg = (ValueExpression) ((UnaryExpression) e).getExpression();
				CI value = eval(state, arg, pp, oracle);
				if (value.isTop())
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							0,
							null,
							e,
							pp);
				if (value.isBottom())
					return null;
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						value.certainlyContained.size(),
						null,
						e,
						pp);
			}
		}

		BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();
		if (e instanceof BinaryExpression) {
			BinaryOperator operator = ((BinaryExpression) e).getOperator();
			if (operator == ComparisonEq.INSTANCE
					|| operator == ComparisonNe.INSTANCE
					|| operator == StringContains.INSTANCE
					|| operator == StringEndsWith.INSTANCE
					|| operator == StringEquals.INSTANCE
					|| operator == StringEqualsIgnoreCase.INSTANCE
					|| operator == StringMatches.INSTANCE
					|| operator == StringStartsWith.INSTANCE
					|| operator == StringIsPrefixOf.INSTANCE
					|| operator == StringIsSuffixOf.INSTANCE) {
				Satisfiability sat = satisfies(state, e, pp, oracle);
				if (sat == Satisfiability.SATISFIED)
					return ValueDomain.makeEqConstraint(booleanType, true, e, pp);
				else if (sat == Satisfiability.NOT_SATISFIED)
					return ValueDomain.makeEqConstraint(booleanType, false, e, pp);
				else if (sat == Satisfiability.UNKNOWN)
					return Collections.emptySet();
				else
					return null;
			} else if (operator == StringIndexOfChar.INSTANCE
					|| operator == StringLastIndexOfChar.INSTANCE
					|| operator == StringIndexOf.INSTANCE
					|| operator == StringLastIndexOf.INSTANCE
					|| operator == ValueComparison.INSTANCE) {
				CI left = eval(state, (ValueExpression) ((BinaryExpression) e).getLeft(), pp, oracle);
				CI right = eval(state, (ValueExpression) ((BinaryExpression) e).getRight(), pp, oracle);
				if (left.isBottom() || right.isBottom())
					return null;
				if (operator == ValueComparison.INSTANCE)
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							-1,
							1,
							e,
							pp);
				if (left.isTop() || right.isTop() || right.certainlyContained.size() != 1)
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							-1,
							null,
							e,
							pp);
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						left.certainlyContained.containsAll(right.certainlyContained) ? 0 : -1,
						null,
						e,
						pp);
			}
		}

		if (e instanceof TernaryExpression) {
			TernaryOperator operator = ((TernaryExpression) e).getOperator();
			if (operator == StringIndexOfCharFromIndex.INSTANCE
					|| operator == StringIndexOfFromIndex.INSTANCE
					|| operator == StringLastIndexOfCharFromIndex.INSTANCE
					|| operator == StringLastIndexOfFromIndex.INSTANCE) {
				CI left = eval(state, (ValueExpression) ((TernaryExpression) e).getLeft(), pp, oracle);
				CI middle = eval(state, (ValueExpression) ((TernaryExpression) e).getMiddle(), pp, oracle);
				Set<BinaryExpression> constraints = oracle.constraints(
						this,
						(ValueExpression) ((TernaryExpression) e).getRight(),
						pp);
				IntegerConstant right = intDomain.generate(constraints, pp, oracle);
				if (left.isBottom() || middle.isBottom() || right.isBottom())
					return null;
				if (left.isTop() || middle.isTop() || right.isTop() || middle.certainlyContained.size() != 1)
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							-1,
							null,
							e,
							pp);
				return ValueDomain.makeRangeConstraints(
						pp.getProgram().getTypes().getIntegerType(),
						left.certainlyContained.containsAll(middle.certainlyContained) ? 0 : -1,
						null,
						e,
						pp);
			}
			if (operator == StringStartsWithFromIndex.INSTANCE) {
				Satisfiability sat = satisfies(state, e, pp, oracle);
				if (sat == Satisfiability.SATISFIED)
					return ValueDomain.makeEqConstraint(booleanType, true, e, pp);
				else if (sat == Satisfiability.NOT_SATISFIED)
					return ValueDomain.makeEqConstraint(booleanType, false, e, pp);
				else if (sat == Satisfiability.UNKNOWN)
					return Collections.emptySet();
				else
					return null;
			}
		}

		CI value = eval(state, e, pp, oracle);
		if (value.isTop())
			return Collections.emptySet();
		if (value.isBottom())
			return null;
		Set<BinaryExpression> constr = new HashSet<>();
		StringType stringType = pp.getProgram().getTypes().getStringType();
		for (Character c : value.certainlyContained) {
			constr.add(
					new BinaryExpression(
							booleanType,
							new Constant(
									stringType,
									String.valueOf(c),
									pp.getLocation()),
							e,
							StringContains.INSTANCE,
							pp.getLocation()));
		}
		return constr;
	}

	private CI generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return bottom();

		CI acc = CI.BOTTOM;
		for (BinaryExpression expr : constraints)
			if (expr.getOperator() instanceof ComparisonEq
					&& expr.getLeft() instanceof Constant
					&& ((Constant) expr.getLeft()).getValue() instanceof String)
				return new CI(((Constant) expr.getLeft()).getValue().toString());
			else if ((expr.getOperator() instanceof StringStartsWith
					|| expr.getOperator() instanceof StringContains
					|| expr.getOperator() instanceof StringEndsWith)
					&& expr.getLeft() instanceof Constant
					&& ((Constant) expr.getLeft()).getValue() instanceof String)
				acc = acc.lub(new CI(((Constant) expr.getLeft()).getValue().toString()));

		return acc;
	}

}
