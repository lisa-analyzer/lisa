package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Bricks extends BaseNonRelationalValueDomain<Bricks> {

	private List<Brick> bricks;

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

	private Brick concatenate(Brick brick1, Brick brick2) { // TODO
		return null;
	}

	private void normalize() { // WIP
		List<Brick> thisBricks = this.bricks;
		thisBricks.removeIf(brick -> brick.getMin() == 0 &&
				brick.getMax() == 0 &&
				brick.getStrings().isEmpty());

		for (Brick brick : thisBricks) {
			Brick nextBrick = thisBricks.get(thisBricks.indexOf(brick) + 1);

			if (brick.getMin() == 1 && brick.getMax() == 1 &&
					nextBrick.getMin() == 1 && nextBrick.getMax() == 1) {

				Brick newBrick = new Brick(1, 1, new HashSet<>());
			}
		}
	}
}
