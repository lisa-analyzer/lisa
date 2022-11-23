package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class Bricks extends BaseNonRelationalValueDomain<Bricks> {

	private final List<Brick> bricks;

	private final static Bricks TOP = new Bricks();

	private final static Bricks BOTTOM = new Bricks(new ArrayList<>());

	public Bricks(){
		this(getTopList());
	}

	public Bricks(List<Brick> bricks) {
		this.bricks = bricks;
	}

	@Override
	public Bricks lubAux(Bricks other) throws SemanticException { // TODO
		return null;
	}

	@Override
	public boolean lessOrEqualAux(Bricks other) throws SemanticException { // TODO
		return false;
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

		firstBrick.getStrings().forEach(string ->
				secondBrick.getStrings().forEach(otherStr ->
						resultSet.add(string + otherStr)));

		this.bricks.set(first, new Brick(1, 1, resultSet));
		this.bricks.remove(second);
	}

	private void rule3(int index) {
		Brick brick = this.bricks.get(index);

		this.bricks.set(index, new Brick(1, 1, brick.getReps()));
	}

	private void rule4(int first, int second){
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


	public void normBricks() { // Applies the 5 normalization rules of the Bricks domain
		List<Brick> thisBricks = this.bricks;

		List<Brick> tempList = new ArrayList<>(thisBricks);

		thisBricks.removeIf(brick -> brick.getMin() == 0 && //Rule 1
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
						nextBrick.getMin() == 1 && nextBrick.getMax() == 1) { //Rule 2
					rule2(i, i + 1);

					lastBrick = i == thisBricks.size() - 1;
				}

			if (currentBrick.getMin() == currentBrick.getMax()) //Rule 3
				rule3(i);

			if (!lastBrick)
				if (currentBrick.getStrings().equals(nextBrick.getStrings()))//Rule 4
					rule4(i, i + 1);

			if (currentBrick.getMin() >= 1 &&
					currentBrick.getMin() != currentBrick.getMax()) //Rule 5
				rule5(i);
		}
		
		if(!thisBricks.equals(tempList))
			normBricks();
	}

	private static List<Brick> getTopList(){
		List<Brick> bricks = new ArrayList<>();
		bricks.add(new Brick());

		return bricks;
	}

	public Bricks padList(Bricks other){
		int thisSize = this.bricks.size();
		int otherSize = other.bricks.size();

		List<Brick> shorter = other.bricks;
		List<Brick> longer = this.bricks;

		if(thisSize < otherSize) {
			shorter = this.bricks;
			longer = other.bricks;
		}

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
		return new Bricks(newList);
	}
}

