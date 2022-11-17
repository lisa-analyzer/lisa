package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Bricks extends BaseNonRelationalValueDomain<Bricks> {

	private List<Brick> bricks;

	@Override
	public Bricks lubAux(Bricks other) throws SemanticException {
		return null;
	}

	@Override
	public boolean lessOrEqualAux(Bricks other) throws SemanticException {
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
		return null;
	}

	@Override
	public Bricks bottom() {
		return null;
	}

	@Override
	public DomainRepresentation representation() {
		return null;
	}

    private Brick concatenate(Brick brick1, Brick brick2){
		return null;
    }

	private void normalize() {
        List<Brick> thisBricks = this.bricks;
        thisBricks.removeIf(brick -> brick.getMin() == 0 &&
				brick.getMax() == 0 &&
				brick.getStrings().isEmpty());

        for(Brick brick: thisBricks){
            Brick nextBrick = thisBricks.get(thisBricks.indexOf(brick) + 1);

            if(brick.getMin() == 1 && brick.getMax() == 1 &&
                    nextBrick.getMin() == 1 && nextBrick.getMax() == 1){

                Brick newBrick = new Brick(1,1,new HashSet<String>());
            }
        }
	}
}
