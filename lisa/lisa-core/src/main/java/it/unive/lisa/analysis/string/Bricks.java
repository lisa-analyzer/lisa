package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

public class Bricks extends BaseNonRelationalValueDomain<Bricks> {

	private List<Brick> bricks;

	private final static Bricks TOP = new Bricks();

	private final static Bricks BOTTOM = new Bricks(new ArrayList<>());

	public Bricks() {
		this(getTopList());
	}

	public Bricks(List<Brick> bricks) {
		this.bricks = bricks;
	}

	@Override
	public Bricks lubAux(Bricks other) throws SemanticException {
		if (this.bricks.size() != other.bricks.size())
			if (this.bricks.size() < other.bricks.size())
				this.bricks = this.padList(other);
			else
				other.bricks = other.padList(this);

		List<Brick> bricks = new ArrayList<>();

		for (int i = 0; i < this.bricks.size(); ++i)
			bricks.add(this.bricks.get(i).lubAux(other.bricks.get(i)));

		return new Bricks(bricks);
	}

	@Override
	public boolean lessOrEqualAux(Bricks other) throws SemanticException {
		if (this.bricks.size() != other.bricks.size())
			if (this.bricks.size() < other.bricks.size())
				this.bricks = this.padList(other);
			else
				other.bricks = other.padList(this);

		for (int i = 0; i < this.bricks.size(); ++i)
			if (!this.bricks.get(i).lessOrEqualAux(other.bricks.get(i)))
				return false;

		return true;
	}

	@Override
	public Bricks evalBinaryExpression(BinaryOperator operator, Bricks left, Bricks right, ProgramPoint pp) // TODO
			throws SemanticException {
		return super.evalBinaryExpression(operator, left, right, pp);
	}

	@Override
	public Bricks evalNonNullConstant(Constant constant, ProgramPoint pp) throws SemanticException {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();

			if (!str.isEmpty()) {
				Collection<String> strings = new HashSet<>();
				strings.add(str);

				List<Brick> bricks = new ArrayList<>();

				bricks.add(new Brick(1, 1, strings));

				return new Bricks(bricks);
			}
		}
		return TOP;
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

		Collection<String> resultSet = new HashSet<>();

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

	public List<Brick> padList(Bricks other) {
		if (this.bricks.size() >= other.bricks.size())
			throw new IllegalArgumentException();

		List<Brick> shorter = this.bricks;
		List<Brick> longer = other.bricks;

		int diff = longer.size() - shorter.size();
		int emptyAdded = 0;

		List<Brick> newList = new ArrayList<>();

		for (Brick brick : longer) {
			if (emptyAdded >= diff) {
				newList.addAll(shorter);
				break;

			} else if (shorter.isEmpty() || shorter.get(0) != brick) {
				newList.add(new Brick(0, 0, new HashSet<>()));
				emptyAdded++;
			} else {
				newList.add(shorter.get(0));
				shorter.remove(0);
			}
		}
		return newList;
	}

}
