package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BoundedStringSet
		extends
		SetLattice<BoundedStringSet, String>
		implements
		SmashedSumStringDomain<BoundedStringSet> {

	public static int MAX_SIZE = 10;

	public BoundedStringSet() {
		super(Collections.emptySet(), true);
	}

	private BoundedStringSet(
			Set<String> elements) {
		super(elements.size() > MAX_SIZE ? Collections.emptySet() : elements, elements.size() > MAX_SIZE);
	}

	private BoundedStringSet(
			Set<String> elements,
			boolean isTop) {
		super(elements, isTop);
	}

	@Override
	public BoundedStringSet lubAux(
			BoundedStringSet other)
			throws SemanticException {
		BoundedStringSet lub = super.lubAux(other);
		if (lub.elements.size() > MAX_SIZE)
			return top();
		return lub;
	}

	@Override
	public BoundedStringSet top() {
		return new BoundedStringSet(Collections.emptySet(), true);
	}

	@Override
	public BoundedStringSet bottom() {
		return new BoundedStringSet(Collections.emptySet(), false);
	}

	@Override
	public BoundedStringSet mk(
			Set<String> set) {
		return new BoundedStringSet(set, false);
	}

	@Override
	public BoundedStringSet evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new BoundedStringSet(Collections.singleton(str));
		}

		return top();
	}

	@Override
	public BoundedStringSet evalBinaryExpression(
			BinaryOperator operator,
			BoundedStringSet left,
			BoundedStringSet right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (operator == StringConcat.INSTANCE) {
			if (left.isTop() || right.isTop())
				return top();

			Set<String> result = new HashSet<>();
			for (String l : left.elements)
				for (String r : right.elements)
					result.add(l + r);
			return new BoundedStringSet(result);
		}

		return top();
	}

	@Override
	public BoundedStringSet evalTernaryExpression(
			TernaryOperator operator,
			BoundedStringSet left,
			BoundedStringSet middle,
			BoundedStringSet right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == StringReplace.INSTANCE) {
			if (left.isTop() || right.isTop() || middle.isTop()
			// if we have more search/replace strings than one, we cannot
			// guarantee what replacement will happen
					|| middle.elements.size() != 1 || right.elements.size() != 1)
				return top();

			String replace = middle.elements.iterator().next();
			String string = right.elements.iterator().next();

			Set<String> result = new HashSet<>();
			for (String target : left.elements)
				result.add(target.replace(replace, string));

			return new BoundedStringSet(result);
		}

		return top();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryOperator operator,
			BoundedStringSet left,
			BoundedStringSet right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
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
			char c)
			throws SemanticException {
		if (isTop())
			return Satisfiability.UNKNOWN;
		if (isBottom())
			return Satisfiability.BOTTOM;

		boolean all = true, one = false;
		for (String str : elements)
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
	public IntInterval length() throws SemanticException {
		if (isTop())
			return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		if (isBottom())
			return null;

		int minLength = Integer.MAX_VALUE;
		int maxLength = 0;
		for (String str : elements) {
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
			BoundedStringSet s)
			throws SemanticException {
		if (isBottom() || s.isBottom())
			return null;
		if (isTop() || s.isTop())
			return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);

		int minIndex = Integer.MAX_VALUE;
		int maxIndex = -1;

		for (String str : elements)
			for (String sub : s.elements) {
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
	public BoundedStringSet substring(
			long begin,
			long end)
			throws SemanticException {
		if (isBottom() || isTop())
			return this;

		Set<String> result = new HashSet<>();
		for (String str : elements) {
			if (end < str.length())
				result.add(str.substring((int) begin, (int) end));
		}

		return new BoundedStringSet(result);
	}
}
