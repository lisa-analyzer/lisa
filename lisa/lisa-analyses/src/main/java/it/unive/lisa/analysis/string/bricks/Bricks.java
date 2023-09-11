package it.unive.lisa.analysis.string.bricks;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.string.ContainsCharProvider;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.DomainRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/**
 * The bricks string abstract domain.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio
 *             Salvatore Evola</a>
 *
 * @see <a href=
 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
 */
public class Bricks implements BaseNonRelationalValueDomain<Bricks>, ContainsCharProvider {

	private final List<Brick> bricks;

	private final static Bricks TOP = new Bricks();

	private final static Bricks BOTTOM = new Bricks(new ArrayList<>());
	/**
	 * The length of the bricks list used in the widening.
	 */
	public static int kL = 20;
	/**
	 * The indices range of a brick used in the widening.
	 */
	public static int kI = 20;
	/**
	 * The number of strings in the set of a brick used in the widening.
	 */
	public static int kS = 50;

	/**
	 * Builds the top brick abstract element.
	 */
	public Bricks() {
		this.bricks = new ArrayList<>(1);
		bricks.add(new Brick());
	}

	/**
	 * Builds a bricks abstract element.
	 *
	 * @param bricks the list of brick
	 */
	public Bricks(List<Brick> bricks) {
		this.bricks = bricks;
	}

	@Override
	public Bricks lubAux(Bricks other) throws SemanticException {
		List<Brick> thisPaddedList = this.bricks;
		List<Brick> otherPaddedList = other.bricks;

		if (this.bricks.size() < other.bricks.size())
			thisPaddedList = this.padList(other);
		else if (other.bricks.size() < this.bricks.size())
			otherPaddedList = other.padList(this);

		List<Brick> resultBricks = new ArrayList<>(thisPaddedList.size());

		for (int i = 0; i < thisPaddedList.size(); ++i)
			resultBricks.add(thisPaddedList.get(i).lub(otherPaddedList.get(i)));

		Bricks result = new Bricks(resultBricks);
		result.normBricks();
		return result;
	}

	@Override
	public boolean lessOrEqualAux(Bricks other) throws SemanticException {
		List<Brick> thisPaddedList = this.bricks;
		List<Brick> otherPaddedList = other.bricks;

		if (this.bricks.size() < other.bricks.size())
			thisPaddedList = this.padList(other);
		else if (other.bricks.size() < this.bricks.size())
			otherPaddedList = other.padList(this);

		for (int i = 0; i < thisPaddedList.size(); ++i) {
			Brick first = thisPaddedList.get(i);
			Brick second = otherPaddedList.get(i);
			if (!first.lessOrEqual(second))
				return false;
		}

		return true;
	}

	@Override
	public Bricks wideningAux(Bricks other) throws SemanticException {
		boolean rel = this.lessOrEqual(other);
		if (!rel && !other.lessOrEqual(this))
			return TOP;

		if (this.bricks.size() > kL || other.bricks.size() > kL)
			return TOP;

		if (rel)
			return w(other);
		else
			return other.w(this);
	}

	private Bricks w(Bricks other) throws SemanticException {
		List<Brick> thisPaddedList = this.bricks;
		List<Brick> otherPaddedList = other.bricks;

		if (this.bricks.size() < other.bricks.size())
			thisPaddedList = this.padList(other);

		else if (other.bricks.size() < this.bricks.size())
			otherPaddedList = other.padList(this);

		List<Brick> resultList = new ArrayList<>();

		for (int i = 0; i < thisPaddedList.size(); ++i) {
			Brick thisCurrent = thisPaddedList.get(i);
			Brick otherCurrent = otherPaddedList.get(i);
			resultList.add(thisCurrent.widening(otherCurrent));
		}

		Bricks result = new Bricks(resultList);
		result.normBricks();
		return result;
	}

	@Override
	public Bricks evalBinaryExpression(BinaryOperator operator, Bricks left, Bricks right, ProgramPoint pp)
			throws SemanticException {
		if (operator == StringConcat.INSTANCE) {
			List<Brick> resultList = new ArrayList<>(left.bricks);
			resultList.addAll(right.bricks);

			return new Bricks(resultList);
		} else if (operator == StringContains.INSTANCE ||
				operator == StringEndsWith.INSTANCE ||
				operator == StringEquals.INSTANCE ||
				operator == StringIndexOf.INSTANCE ||
				operator == StringStartsWith.INSTANCE) {
			return TOP;
		}
		return TOP;
	}

	@Override
	public Bricks evalNonNullConstant(Constant constant, ProgramPoint pp) throws SemanticException {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();

			Set<String> strings = new TreeSet<>();
			strings.add(str);

			List<Brick> resultList = new ArrayList<>();

			resultList.add(new Brick(1, 1, strings));

			return new Bricks(resultList);
		}
		return TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Bricks left, Bricks right, ProgramPoint pp)
			throws SemanticException {
		if (left.isTop() || right.isBottom())
			return SemanticDomain.Satisfiability.UNKNOWN;

		if (operator == StringContains.INSTANCE)
			return left.contains(right);

		return Satisfiability.UNKNOWN;
	}

	private Satisfiability contains(Bricks right) {
		if (right.bricks.size() != 1)
			return Satisfiability.UNKNOWN;

		if (!right.bricks.get(0).isFinite())
			return Satisfiability.UNKNOWN;

		Set<String> strings = right.bricks.get(0).getStrings();
		if (strings.size() != 1)
			return Satisfiability.UNKNOWN;

		if (strings.iterator().next().length() != 1)
			return Satisfiability.UNKNOWN;

		String c = strings.iterator().next();

		boolean res = bricks.stream()
				.filter(b -> b.getMin().gt(MathNumber.ZERO))
				.map(b -> b.getStrings())
				.anyMatch(set -> set == null || set.stream().allMatch(s -> s.contains(c)));
		if (res)
			return Satisfiability.SATISFIED;

		res = bricks.stream()
				.map(b -> b.getStrings())
				.allMatch(set -> set != null && set.stream().allMatch(s -> !s.contains(c)));
		if (res)
			return Satisfiability.NOT_SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object == null || getClass() != object.getClass())
			return false;
		Bricks bricks1 = (Bricks) object;
		return java.util.Objects.equals(bricks, bricks1.bricks);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bricks);
	}

	@Override
	public Bricks top() {
		return TOP;
	}

	@Override
	public Bricks bottom() {
		return BOTTOM;
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(StringUtils.join(this.bricks, " "));
	}

	private void rule2(int first, int second) {
		Brick firstBrick = this.bricks.get(first);
		Brick secondBrick = this.bricks.get(second);

		Set<String> resultSet;
		if (firstBrick.getStrings() == null || secondBrick.getStrings() == null)
			resultSet = null;
		else {
			resultSet = new TreeSet<>();
			firstBrick.getStrings()
					.forEach(string -> secondBrick.getStrings().forEach(otherStr -> resultSet.add(string + otherStr)));
		}

		this.bricks.set(first, new Brick(1, 1, resultSet));
		this.bricks.remove(second);
	}

	private void rule3(int index) {
		Brick brick = this.bricks.get(index);

		this.bricks.set(index, new Brick(1, 1, brick.getReps()));
	}

	private void rule4(int first, int second) {
		Brick firstBrick = this.bricks.get(first);
		Brick secondBrick = this.bricks.get(second);

		this.bricks.set(first, new Brick(firstBrick.getMin().add(secondBrick.getMin()),
				firstBrick.getMax().add(secondBrick.getMax()),
				firstBrick.getStrings()));

		this.bricks.remove(second);
	}

	private void rule5(int index) {
		Brick brick = this.bricks.get(index);

		Brick br = new Brick(brick.getMin(), brick.getMin(), brick.getStrings());

		this.bricks.set(index, new Brick(1, 1, br.getReps()));
		this.bricks.add(index + 1,
				new Brick(MathNumber.ZERO, brick.getMax().subtract(brick.getMin()), brick.getStrings()));
	}

	/**
	 * The normalization method of the bricks domain. Modify bricks to its
	 * normalized form.
	 */
	public void normBricks() {
		if (isTop())
			return;

		List<Brick> thisBricks = this.bricks;
		List<Brick> tempList = new ArrayList<>(thisBricks);

		thisBricks.removeIf(brick -> brick.getMin().equals(MathNumber.ZERO)
				&& brick.getMax().equals(MathNumber.ZERO)
				&& brick.getStrings() != null
				&& brick.getStrings().isEmpty());

		for (int i = 0; i < thisBricks.size(); ++i) {
			Brick currentBrick = thisBricks.get(i);
			Brick nextBrick = null;
			boolean lastBrick = i == thisBricks.size() - 1;

			if (!lastBrick)
				nextBrick = thisBricks.get(i + 1);

			if (!lastBrick)
				if (currentBrick.getMin().equals(MathNumber.ONE) && currentBrick.getMax().equals(MathNumber.ONE) &&
						nextBrick.getMin().equals(MathNumber.ONE) && nextBrick.getMax().equals(MathNumber.ONE)) {

					rule2(i, i + 1);

					lastBrick = i == thisBricks.size() - 1;
				}

			if (currentBrick.getMin().equals(currentBrick.getMax())
					&& !currentBrick.getMin().equals(MathNumber.ONE)
					&& !currentBrick.getMax().equals(MathNumber.ONE)
					&& currentBrick.getStrings() != null)
				rule3(i);

			if (!lastBrick)
				if (currentBrick.getStrings() != null && currentBrick.getStrings().equals(nextBrick.getStrings()))
					rule4(i, i + 1);

			if (MathNumber.ONE.lt(currentBrick.getMin())
					&& !currentBrick.getMin().equals(currentBrick.getMax())
					&& currentBrick.getStrings() != null)
				rule5(i);
		}

		if (!thisBricks.equals(tempList))
			normBricks();
	}

	/**
	 * The substring method of the bricks domain.
	 *
	 * @param e The beginning index of the substring
	 * @param b The ending index of the substring
	 *
	 * @return A new Bricks with all possible substrings if the conditions are
	 *             met or TOP.
	 */
	public Bricks substring(long e, long b) {
		this.normBricks();

		Brick first = this.bricks.get(0);

		TreeSet<String> result = new TreeSet<>();

		if (first.getMin().equals(MathNumber.ONE)
				&& first.getMax().equals(MathNumber.ONE)
				&& first.getStrings() != null
				&& !first.getStrings().isEmpty()) {
			first.getStrings().forEach(s -> {
				boolean allGreater = s.length() >= e;

				if (allGreater)
					result.add(s.substring((int) e, (int) b));
			});
		}

		if (result.size() == first.getStrings().size()) {
			List<Brick> resultList = new ArrayList<>();

			resultList.add(new Brick(new IntInterval(1, 1), result));

			return new Bricks(resultList);
		}

		return TOP;
	}

	/**
	 * Pads the shortest brick list and adds empty brick elements to it, in
	 * order to make it the same size of the longer brick list, while
	 * maintaining the same position of equals elements between the two lists.
	 *
	 * @param other the other bricks object, which has to yield the longer list
	 *
	 * @return the shorter list with empty brick in it
	 *
	 * @throws IllegalArgumentException if the other brick list is longer or
	 *                                      equal than the caller bricks object
	 */
	public List<Brick> padList(final Bricks other) {
		if (this.bricks.size() >= other.bricks.size())
			throw new IllegalArgumentException("Other bricks list is longer or equal");

		List<Brick> l1 = new ArrayList<>(this.bricks), l2 = new ArrayList<>(other.bricks);
		Brick e = new Brick(0, 0, new TreeSet<>());
		int n1 = l1.size();
		int n2 = l2.size();
		int n = n2 - n1;
		List<Brick> lnew = new ArrayList<>();
		int emptyBricksAdded = 0;

		for (int i = 0; i < n2; i++)
			if (emptyBricksAdded >= n) {
				lnew.add(l1.get(0));
				l1.remove(0);
			} else if (l1.isEmpty() || !l1.get(0).equals(l2.get(i))) {
				lnew.add(e);
				emptyBricksAdded++;
			} else {
				lnew.add(l1.get(0));
				l1.remove(0);
			}

		return lnew;
	}

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum length
	 * of this abstract value.
	 * 
	 * @return the minimum and maximum length of this abstract value
	 */
	public IntInterval length() {
		return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
	}

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum index
	 * of {@code s} in {@code this}.
	 *
	 * @param s the string to be searched
	 * 
	 * @return the minimum and maximum index of {@code s} in {@code this}
	 */
	public IntInterval indexOf(Bricks s) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(char c) throws SemanticException {
		if (isTop())
			return Satisfiability.UNKNOWN;
		if (isBottom())
			return Satisfiability.BOTTOM;

		Satisfiability sat = Satisfiability.BOTTOM;

		for (Brick b : this.bricks) {
			// surely a string of the brick is contained
			if (b.getMin().geq(MathNumber.ONE)) {
				Satisfiability bricksat = Satisfiability.BOTTOM;
				for (String s : b.getStrings())
					if (!s.contains(String.valueOf(c)))
						bricksat = bricksat.lub(Satisfiability.NOT_SATISFIED);
					else
						bricksat = bricksat.lub(Satisfiability.SATISFIED);

				if (bricksat == Satisfiability.SATISFIED)
					return bricksat;
				else
					sat = sat.lub(bricksat);
			} else {
				// the brick can be missing
				for (String s : b.getStrings())
					if (s.contains(String.valueOf(c)))
						sat = sat.lub(Satisfiability.UNKNOWN);
					else
						sat = sat.lub(Satisfiability.NOT_SATISFIED);
			}
		}

		return sat;
	}
}
