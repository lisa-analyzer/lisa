package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class Brick extends BaseNonRelationalValueDomain<Brick> {
    private final Collection<String> strings;
    private final IntInterval brickInterval;

    private final static Brick TOP = new Brick();

    private final static Brick BOTTOM = new Brick(new IntInterval(0,0), new HashSet<>());

    public Brick() {
        this(new IntInterval(new MathNumber(0), MathNumber.PLUS_INFINITY),getAlphabet());
    }

    public Brick(int min, int max, Collection<String> strings) {
        this.brickInterval = new IntInterval(min,max);
        this.strings = strings;
    }

    public Brick(IntInterval interval, Collection<String> strings){
        this.brickInterval = interval;
        this.strings = strings;
    }

    @Override
    public Brick lubAux(Brick other) throws SemanticException {
        return null;
    }

    @Override
    public boolean lessOrEqualAux(Brick other) throws SemanticException {
        if(this.isBottom())
            return true;

        if(other.isTop())
            return true;

        if(this.strings.size() > other.strings.size())
            return false;

        if(other.strings.containsAll(this.strings))
            if(this.getMin() >= other.getMin())
                return this.getMax() <= other.getMax();

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Brick brick = (Brick) o;
        return Objects.equals(strings, brick.strings) && Objects.equals(brickInterval, brick.brickInterval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strings, brickInterval);
    }

    public int getMin() {
        return this.brickInterval.getLow().getNumber().intValue();
    }

    public int getMax() {
        return this.brickInterval.getHigh().getNumber().intValue();
    }

    public Collection<String> getStrings() {
        return strings;
    }

    public Brick top(){
        return TOP;
    }

    public Brick bottom(){
        return BOTTOM;
    }

    public Collection<String> getReps() {
        Collection<String> reps = new HashSet<>();

        if (this.strings.size() == 1) {
            String element = this.strings.iterator().next();
            reps.add(element.repeat(this.getMin()));
            reps.add(element.repeat(this.getMax()));
            return reps;
        }
        this.recGetReps(reps, this.getMin(), 0, "");

        return reps;
    }

    //Recursive function that gets all the possible combinations of the set between min and max
    private void recGetReps(Collection<String> reps, int min, int numberOfReps, String currentStr) {
        if (min > this.getMax() && numberOfReps >= this.getMin())//If the number of reps (starting from min) exceeds the max, then stops the recursion
            reps.add(currentStr);
        else {
            for (String string : this.strings) {
                if ((!currentStr.equals("") || this.getMin() == 0) && numberOfReps >= this.getMin()) //numberOfReps has to be ALWAYS greater or equal than the min
                    reps.add(currentStr);

                recGetReps(reps, min + 1, numberOfReps + 1, currentStr + string);
            }
        }
    }

	private String formatRepresentation() {
		return "[ {min: " +
				this.getMin() +
				"}, {max: " +
				this.getMax() +
				"}, {strings: " +
				StringUtils.join(this.strings, ", ") +
				"} ]";
	}

    private static Collection<String> getAlphabet() {
        Collection<String> alphabet = new HashSet<>();

        for (char c = 'a'; c <= 'z'; c++) {
            alphabet.add(String.valueOf(c));
        }

        return alphabet;
    }

    @Override
    public DomainRepresentation representation() {
        if (isBottom())
            return Lattice.bottomRepresentation();
        if (isTop())
            return Lattice.topRepresentation();

        return new StringRepresentation(formatRepresentation());
    }
}
