package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValueElement;
import it.unive.lisa.analysis.combination.constraints.WholeValueStringDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/**
 * A domain computing bounded set of strings, where the maximum number of
 * elements is defined by {@link #max_size}. If the number of elements exceeds
 * this limit, the set is considered to be top. The domain is defined
 * <a href="https://link.springer.com/chapter/10.1007/978-3-642-54807-9_12">in
 * this paper</a>.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BoundedStringSet
		implements
		SmashedSumStringDomain<BoundedStringSet.BSS>,
		WholeValueStringDomain<BoundedStringSet.BSS> {

	/**
	 * A bounded set of strings, where the maximum number of elements is defined
	 * by {@link #max_size}. If the number of elements exceeds this limit, the
	 * set is considered to be top. The domain is defined <a href=
	 * "https://link.springer.com/chapter/10.1007/978-3-642-54807-9_12">in this
	 * paper</a>.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public class BSS extends SetLattice<BSS, String> implements WholeValueElement<BSS> {

		/**
		 * Builds the top abstract value.
		 */
		public BSS() {
			super(Collections.emptySet(), true);
		}

		private BSS(
				Set<String> elements) {
			super(elements.size() > max_size ? Collections.emptySet() : elements, elements.size() > max_size);
		}

		private BSS(
				Set<String> elements,
				boolean isTop) {
			super(elements, isTop);
		}

		@Override
		public BSS lubAux(
				BSS other)
				throws SemanticException {
			BSS lub = super.lubAux(other);
			if (lub.elements.size() > max_size)
				return top();
			return lub;
		}

		@Override
		public BSS top() {
			return new BSS(Collections.emptySet(), true);
		}

		@Override
		public BSS bottom() {
			return new BSS(Collections.emptySet(), false);
		}

		@Override
		public BSS mk(
				Set<String> set) {
			return new BSS(set, false);
		}

		@Override
		public Set<BinaryExpression> constraints(
				ValueExpression e,
				ProgramPoint pp)
				throws SemanticException {
			if (isBottom())
				return null;

			BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();
			UnaryExpression strlen = new UnaryExpression(
				pp.getProgram().getTypes().getIntegerType(),
				e,
				StringLength.INSTANCE,
				pp.getLocation());

			if (isTop())
				return Collections.singleton(
					new BinaryExpression(
						booleanType,
						new Constant(pp.getProgram().getTypes().getIntegerType(), 0, pp.getLocation()),
						strlen,
						ComparisonLe.INSTANCE,
						e.getCodeLocation()));

			int min = Integer.MAX_VALUE, max = 0;
			String gcs = null, gcp = null;
			for (String str : elements) {
				min = Math.min(min, str.length());
				max = Math.max(max, str.length());
				if (gcp == null)
					gcp = str;
				else
					gcp = StringUtilities.gcp(gcp, StringUtils.reverse(str));
				if (gcs == null)
					gcs = StringUtils.reverse(str);
				else
					gcs = StringUtilities.gcp(gcs, StringUtils.reverse(str));
			}

			return Set.of(
				new BinaryExpression(
					booleanType,
					new Constant(pp.getProgram().getTypes().getIntegerType(), min, pp.getLocation()),
					strlen,
					ComparisonLe.INSTANCE,
					e.getCodeLocation()),
				new BinaryExpression(
					booleanType,
					new Constant(pp.getProgram().getTypes().getIntegerType(), max, pp.getLocation()),
					strlen,
					ComparisonGe.INSTANCE,
					e.getCodeLocation()),
				new BinaryExpression(
					booleanType,
					new Constant(pp.getProgram().getTypes().getStringType(), gcp, pp.getLocation()),
					e,
					StringStartsWith.INSTANCE,
					e.getCodeLocation()),
				new BinaryExpression(
					booleanType,
					new Constant(pp.getProgram().getTypes().getStringType(), gcs, pp.getLocation()),
					e,
					StringEndsWith.INSTANCE,
					e.getCodeLocation()));
		}

		@Override
		public BSS generate(
				Set<BinaryExpression> constraints,
				ProgramPoint pp)
				throws SemanticException {
			if (constraints == null)
				return bottom();

			for (BinaryExpression expr : constraints)
				if (expr.getOperator() instanceof ComparisonEq
						&& expr.getLeft() instanceof Constant
						&& ((Constant) expr.getLeft()).getValue() instanceof String)
					return new BSS(Collections.singleton(((Constant) expr.getLeft()).getValue().toString()));

			return top();
		}

	}

	/**
	 * The maximum number of elements that instances of this domain can contain
	 * before being considered top.
	 */
	private final int max_size;

	/**
	 * Builds the domain, using {@code 10} as the maximum number of strings to
	 * track before going to top.
	 */
	public BoundedStringSet() {
		this(10);
	}

	/**
	 * Builds the domain, using {@code max_size} as the maximum number of
	 * strings to track before going to top.
	 *
	 * @param max_size the maximum number of strings to track before going to
	 *                     top
	 */
	public BoundedStringSet(
			int max_size) {
		this.max_size = max_size;
	}

	@Override
	public BSS evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new BSS(Collections.singleton(str));
		}

		return top();
	}

	@Override
	public BSS evalBinaryExpression(
			BinaryExpression expression,
			BSS left,
			BSS right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == StringConcat.INSTANCE) {
			if (left.isTop() || right.isTop())
				return top();

			Set<String> result = new TreeSet<>();
			for (String l : left.elements)
				for (String r : right.elements)
					result.add(l + r);
			return new BSS(result);
		}

		return top();
	}

	@Override
	public BSS evalTernaryExpression(
			TernaryExpression expression,
			BSS left,
			BSS middle,
			BSS right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringReplace.INSTANCE) {
			if (left.isTop() || right.isTop() || middle.isTop()
			// if we have more search/replace strings than one, we cannot
			// guarantee what replacement will happen
					|| middle.elements.size() != 1
					|| right.elements.size() != 1)
				return top();

			String replace = middle.elements.iterator().next();
			String string = right.elements.iterator().next();

			Set<String> result = new TreeSet<>();
			for (String target : left.elements)
				result.add(target.replace(replace, string));

			return new BSS(result);
		}

		return top();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			BSS left,
			BSS right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();
		if (operator == StringContains.INSTANCE) {
			if (left.isTop() || right.isTop())
				return Satisfiability.UNKNOWN;

			boolean all = true;
			boolean none = true;
			for (String l : left.elements)
				for (String r : right.elements)
					if (l.contains(r))
						none = false;
					else
						all = false;

			if (none)
				return Satisfiability.NOT_SATISFIED;
			if (all)
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		if (operator == StringEquals.INSTANCE) {
			if (left.isTop() || right.isTop() || left.elements.size() != 1 || right.elements.size() != 1)
				return Satisfiability.UNKNOWN;

			return Satisfiability.fromBoolean(left.elements.iterator().next().equals(right.elements.iterator().next()));
		}
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability containsChar(
			BSS current,
			char c)
			throws SemanticException {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;

		boolean all = true, one = false;
		for (String str : current.elements)
			if (str.indexOf(c) >= 0)
				one = true;
			else
				all = false;
		if (all)
			return Satisfiability.SATISFIED;
		if (one)
			return Satisfiability.UNKNOWN;
		return Satisfiability.NOT_SATISFIED;
	}

	@Override
	public IntInterval length(
			BSS current)
			throws SemanticException {
		if (current.isTop())
			return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		if (current.isBottom())
			return null;

		int minLength = Integer.MAX_VALUE;
		int maxLength = 0;
		for (String str : current.elements) {
			int len = str.length();
			if (len < minLength)
				minLength = len;
			if (len > maxLength)
				maxLength = len;
		}

		return new IntInterval(minLength, maxLength);
	}

	@Override
	public IntInterval indexOf(
			BSS current,
			BSS other)
			throws SemanticException {
		if (current.isBottom() || other.isBottom())
			return null;
		if (current.isTop() || other.isTop())
			return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);

		int minIndex = Integer.MAX_VALUE;
		int maxIndex = -1;

		for (String str : current.elements)
			for (String sub : other.elements) {
				int index = str.indexOf(sub);
				if (index >= 0) {
					if (index < minIndex)
						minIndex = index;
					if (index > maxIndex)
						maxIndex = index;
				}
			}

		if (minIndex == Integer.MAX_VALUE)
			return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
		return new IntInterval(minIndex, maxIndex);
	}

	@Override
	public BSS substring(
			BSS current,
			long begin,
			long end)
			throws SemanticException {
		if (current.isBottom() || current.isTop())
			return current;

		Set<String> result = new TreeSet<>();
		for (String str : current.elements) {
			if (end <= str.length())
				result.add(str.substring((int) begin, (int) end));
		}

		return new BSS(result);
	}

	@Override
	public BSS substring(
			BSS current,
			Set<BinaryExpression> a1,
			Set<BinaryExpression> a2,
			ProgramPoint pp)
			throws SemanticException {
		if (current.isBottom() || a1 == null || a2 == null)
			return bottom();

		Integer minI = null, maxI = null;
		for (BinaryExpression expr : a1)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					minI = maxI = val;
				else if (expr.getOperator() instanceof ComparisonGe)
					maxI = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					minI = val;
			}
		if (minI == null || minI < 0)
			minI = 0;
		if (maxI != null && maxI < minI)
			maxI = minI;

		Integer minJ = null, maxJ = null;
		for (BinaryExpression expr : a2)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					minJ = maxJ = val;
				else if (expr.getOperator() instanceof ComparisonGe)
					maxJ = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					minJ = val;
			}
		if (minJ == null || minJ < 0)
			minJ = 0;
		if (maxJ != null && maxJ < minJ)
			maxJ = minJ;

		if (maxI == null || maxJ == null || (maxJ - minJ) * (maxI - minI) * current.elements.size() > max_size)
			return top();

		Set<String> el = new TreeSet<>();
		for (String str : current.elements)
			for (int i = minI; i <= maxI; i++)
				for (int j = minJ; j <= maxJ; j++)
					if (i <= j && j <= str.length())
						if (j <= str.length())
							el.add(str.substring(i, j));
						else
							el.add(str.substring(i));
		return new BSS(el);
	}

	@Override
	public Set<BinaryExpression> indexOf_constr(
			BinaryExpression expression,
			BSS current,
			BSS other,
			ProgramPoint pp)
			throws SemanticException {
		if (current.isBottom() || other.isBottom())
			return null;

		IntInterval indexes = indexOf(current, other);
		BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();

		Set<BinaryExpression> constr = new HashSet<>();
		try {
			constr.add(
				new BinaryExpression(
					booleanType,
					new Constant(
						pp.getProgram().getTypes().getIntegerType(),
						indexes.getLow().toInt(),
						pp.getLocation()),
					expression,
					ComparisonLe.INSTANCE,
					pp.getLocation()));
			if (indexes.getHigh().isFinite())
				constr.add(
					new BinaryExpression(
						booleanType,
						new Constant(
							pp.getProgram().getTypes().getIntegerType(),
							indexes.getHigh().toInt(),
							pp.getLocation()),
						expression,
						ComparisonGe.INSTANCE,
						pp.getLocation()));
		} catch (MathNumberConversionException e1) {
			throw new SemanticException("Cannot convert stirng indexof bound to int", e1);
		}
		return constr;
	}

	@Override
	public BSS top() {
		return new BSS();
	}

	@Override
	public BSS bottom() {
		return new BSS(Collections.emptySet(), false);
	}

}
