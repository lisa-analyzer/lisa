package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(StringUtils.join(this.bricks, ",\n"));
	}

	private List<Brick> rule5(Brick brick){
		List<Brick> list = new ArrayList<>();
		Brick br = new Brick(brick.getStrings().size(),brick.getMin(),brick.getStrings());

		list.add(new Brick(0, brick.getMax() - brick.getMin(),brick.getStrings()));
		list.add(new Brick(1,1, br.getReps()));

		return list;
	}


	public void normalize() { // Applies the 5 normalization rules of the Bricks domain TODO
		List<Brick> thisBricks = this.bricks;

		thisBricks.removeIf(brick -> brick.getMin() == 0 && //Rule 1
				brick.getMax() == 0 &&
				brick.getStrings().isEmpty());

		for(int i = 0; i < thisBricks.size(); ++i) { //Rule 3
			Brick brick = thisBricks.get(i);

			if(brick.getMin() == brick.getMax()) { //Rule 2
				brick.setStrings(brick.getReps());
				brick.setMin(1);
				brick.setMax(1);
			}

			if(brick.getMin() >= 1 && brick.getMin() != brick.getMax()) { //Rule 5
				List<Brick> list = rule5(brick);

				thisBricks.set(i, list.get(0));
				thisBricks.add(i + 1, list.get(1));
			}

			if(i != thisBricks.size() - 1) {
				Brick nextBrick = thisBricks.get(i + 1);

				if (brick.getMin() == 1 && brick.getMax() == 1 &&
						nextBrick.getMin() == 1 && nextBrick.getMax() == 1) { //Rule 2

					Brick br = brick.merge(nextBrick);

					thisBricks.set(i, br);
					thisBricks.remove(nextBrick);
				}

				if (brick.getStrings().equals(nextBrick.getStrings())) { //Rule 4
					brick.setMin(brick.getMin() + nextBrick.getMin());
					brick.setMax(brick.getMax() + nextBrick.getMax());

					thisBricks.remove(nextBrick);
				}
			}
		}
	}
}
