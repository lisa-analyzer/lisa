package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
		return null;
	}

	private void normalize() { // Applies the 5 normalization rules of the Bricks domain TODO
		List<Brick> thisBricks = this.bricks;

		thisBricks.removeIf(brick -> brick.getMin() == 0 && //Rule 1
				brick.getMax() == 0 &&
				brick.getStrings().isEmpty());

		for (Brick brick : thisBricks) { //Rule 3
			if(brick.getMin() == brick.getMax()) {
				brick.setStrings(brick.getReps());
				brick.setMin(1);
				brick.setMax(1);
			}

			Brick nextBrick = thisBricks.get(thisBricks.indexOf(brick) + 1);
			if (brick.getMin() == 1 && brick.getMax() == 1 &&
					nextBrick.getMin() == 1 && nextBrick.getMax() == 1) { //Rule 2

				Brick newBrick = brick.merge(nextBrick);

				thisBricks.remove(brick);
				thisBricks.remove(nextBrick);
				thisBricks.add(newBrick);
			}
			if(brick.getStrings().equals(nextBrick.getStrings())){ //Rule 4
				brick.setMin(brick.getMin() + nextBrick.getMin());
				brick.setMax(brick.getMax() + nextBrick.getMax());

				thisBricks.remove(nextBrick);
			}
		}
	}
}
