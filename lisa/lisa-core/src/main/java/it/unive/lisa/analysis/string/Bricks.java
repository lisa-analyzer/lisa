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
	public Bricks top() { // TODO
		return null;
	}

	@Override
	public Bricks bottom() { // TODO
		return null;
	}

	@Override
	public DomainRepresentation representation() { // TODO
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

		for (String string : firstBrick.getStrings()) {
			for (String otherStr : secondBrick.getStrings()) {
				resultSet.add(string + otherStr);
			}
		}

		this.bricks.set(first, new Brick(1, 1, resultSet));
		this.bricks.remove(secondBrick);
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

		Brick br = new Brick(brick.getStrings().size(), brick.getMin(), brick.getStrings());

		this.bricks.set(index, new Brick(1, 1, br.getReps()));
		this.bricks.add(index + 1, new Brick(0, brick.getMax() - brick.getMin(), brick.getStrings()));

	}


	public void normBricks() { // Applies the 5 normalization rules of the Bricks domain TODO
		List<Brick> thisBricks = this.bricks;

		thisBricks.removeIf(brick -> brick.getMin() == 0 && //Rule 1
				brick.getMax() == 0 &&
				brick.getStrings().isEmpty());

		for (int i = 0; i < thisBricks.size(); ++i) {
			boolean lastBrick = i == thisBricks.size() - 1;
			Brick nextBrick = null;

			Brick brick = thisBricks.get(i);

			if (!lastBrick)
				nextBrick = thisBricks.get(i + 1);

			if (!lastBrick)
				if (brick.getMin() == 1 && brick.getMax() == 1 &&
						nextBrick.getMin() == 1 && nextBrick.getMax() == 1) //Rule 2
					rule2(i, i + 1);


			if (brick.getMin() == brick.getMax()) //Rule 3
				rule3(i);


			if (!lastBrick)
				if (brick.getStrings().equals(nextBrick.getStrings()))//Rule 4
					rule4(i, i + 1);


			if (brick.getMin() >= 1 &&
					brick.getMin() != brick.getMax()) //Rule 5
				rule5(i);
		}
	}
}

