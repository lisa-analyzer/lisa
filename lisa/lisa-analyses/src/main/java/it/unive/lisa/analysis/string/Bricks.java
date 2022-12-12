package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.*;
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
public class Bricks extends BaseNonRelationalValueDomain<Bricks> {

	private List<Brick> bricks;

	private final static Bricks TOP = new Bricks();

	private final static Bricks BOTTOM = new Bricks(new ArrayList<>());

	private final static int kL = 10;

	private final static int kI = 10;

	private final static int kS = 20;

	/**
	 * Builds the top brick abstract element.
	 */
	public Bricks() {
		this(getTopList());
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
		this.padList(other);

		List<Brick> bricks = new ArrayList<>();

		for (int i = 0; i < this.bricks.size(); ++i)
			bricks.add(this.bricks.get(i).lubAux(other.bricks.get(i)));

		return new Bricks(bricks);
	}

	@Override
	public boolean lessOrEqualAux(Bricks other) throws SemanticException {
		this.padList(other);

		for (int i = 0; i < this.bricks.size(); ++i)
			if (!this.bricks.get(i).lessOrEqualAux(other.bricks.get(i)))
				return false;

		return true;
	}

	@Override
	public Bricks wideningAux(Bricks other) throws SemanticException {
		if (!this.lessOrEqualAux(other) &&
				!other.lessOrEqualAux(this))
			return TOP;

		if (this.bricks.size() > kL ||
				other.bricks.size() > kL)
			return TOP;

		return w(other);
	}

	private Bricks w(Bricks other) {
		this.padList(other);

		List<Brick> resultList = new ArrayList<>();

		for (int i = 0; i < this.bricks.size(); ++i) {
			Brick thisCurrent = this.bricks.get(i);
			Brick otherCurrent = other.bricks.get(i);

			if (thisCurrent.isTop() || otherCurrent.isTop()) {
				resultList.add(new Brick());
				break;
			}

			Set<String> resultSet = new TreeSet<>();

			resultSet.addAll(thisCurrent.getStrings());
			resultSet.addAll(otherCurrent.getStrings());

			int minOfMins = Math.min(thisCurrent.getMin(), otherCurrent.getMin());
			int maxOfMaxs = Math.max(thisCurrent.getMax(), otherCurrent.getMax());

			if (resultSet.size() > kS)
				resultList.add(new Brick());

			else if (maxOfMaxs - minOfMins > kI) {
				IntInterval interval = new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
				Brick resultBrick = new Brick(interval, resultSet);
				resultList.add(resultBrick);
			} else
				resultList.add(new Brick(minOfMins, maxOfMaxs, resultSet));
		}

		return new Bricks(resultList);
	}

	@Override
	public Bricks evalBinaryExpression(BinaryOperator operator, Bricks left, Bricks right, ProgramPoint pp)
			throws SemanticException {
		if (operator == StringConcat.INSTANCE) {
			List<Brick> list = new ArrayList<>();
			list.addAll(left.bricks);
			list.addAll(right.bricks);

			return new Bricks(list);
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

			if (!str.isEmpty()) {
				Set<String> strings = new TreeSet<>();
				strings.add(str);

				List<Brick> bricks = new ArrayList<>();

				bricks.add(new Brick(1, 1, strings));

				return new Bricks(bricks);
			}
		}
		return TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, Bricks left, Bricks right, ProgramPoint pp)
			throws SemanticException {
		if (left.isTop() || right.isBottom())
			return SemanticDomain.Satisfiability.UNKNOWN;

		if (operator == StringContains.INSTANCE)
			if (left.bricks.stream().anyMatch(brick -> right.bricks.contains(brick)))
				return Satisfiability.SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Bricks bricks1 = (Bricks) o;
		return Objects.equals(bricks, bricks1.bricks);
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
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(StringUtils.join(this.bricks, ",\n"));
	}

	private void rule2(int first, int second) {
		Brick firstBrick = this.bricks.get(first);
		Brick secondBrick = this.bricks.get(second);

		Set<String> resultSet = new TreeSet<>();

		firstBrick.getStrings()
				.forEach(string -> secondBrick.getStrings().forEach(otherStr -> resultSet.add(string + otherStr)));

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

		this.bricks.set(first, new Brick(firstBrick.getMin() + secondBrick.getMin(),
				firstBrick.getMax() + secondBrick.getMax(),
				firstBrick.getStrings()));

		this.bricks.remove(second);
	}

	private void rule5(int index) {
		Brick brick = this.bricks.get(index);

		Brick br = new Brick(brick.getMin(), brick.getMin(), brick.getStrings());

		this.bricks.set(index, new Brick(1, 1, br.getReps()));
		this.bricks.add(index + 1, new Brick(0, brick.getMax() - brick.getMin(), brick.getStrings()));
	}

	/**
	 * The normalization method of the bricks domain. Modify bricks to its
	 * normalized form.
	 */
	public void normBricks() {
		List<Brick> thisBricks = this.bricks;

		List<Brick> tempList = new ArrayList<>(thisBricks);

		thisBricks.removeIf(brick -> brick.getMin() == 0 &&
				brick.getMax() == 0 &&
				brick.getStrings().isEmpty());

		for (int i = 0; i < thisBricks.size(); ++i) {
			Brick currentBrick = thisBricks.get(i);
			Brick nextBrick = null;
			boolean lastBrick = i == thisBricks.size() - 1;

			if (!lastBrick)
				nextBrick = thisBricks.get(i + 1);

			if (!lastBrick)
				if (currentBrick.getMin() == 1 && currentBrick.getMax() == 1 &&
						nextBrick.getMin() == 1 && nextBrick.getMax() == 1) {

					rule2(i, i + 1);

					lastBrick = i == thisBricks.size() - 1;
				}

			if (currentBrick.getMin() == currentBrick.getMax())
				rule3(i);

			if (!lastBrick)
				if (currentBrick.getStrings().equals(nextBrick.getStrings()))

					rule4(i, i + 1);

			if (currentBrick.getMin() >= 1 &&
					currentBrick.getMin() != currentBrick.getMax())
				rule5(i);
		}

		if (!thisBricks.equals(tempList))
			normBricks();
	}

	private static List<Brick> getTopList() {
		List<Brick> bricks = new ArrayList<>();
		bricks.add(new Brick());

		return bricks;
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
	 * @throws IllegalArgumentException if the other brick list is longer than
	 *                                      the caller bricks object
	 */
	public List<Brick> padList(Bricks other) {
		if (this.bricks.size() == other.bricks.size())
			return null;

		List<Brick> shorter;
		List<Brick> longer;
		boolean thisShorter = false;

		if(this.bricks.size() > other.bricks.size()) {
			longer = this.bricks;
			shorter = other.bricks;
		}
		else{
			shorter = this.bricks;
			longer = other.bricks;
			thisShorter = true;
		}

		int diff = longer.size() - shorter.size();
		int emptyAdded = 0;

		List<Brick> newList = new ArrayList<>();

		for (Brick brick : longer) {
			if (emptyAdded >= diff) {
				newList.addAll(shorter);
				break;

			} else if (shorter.isEmpty() || shorter.get(0) != brick) {
				newList.add(new Brick(0, 0, new TreeSet<>()));
				emptyAdded++;
			} else {
				newList.add(shorter.get(0));
				shorter.remove(0);
			}
		}

		if(thisShorter)
			this.bricks = newList;
		else
			other.bricks = newList;

		return newList;
	}
}
