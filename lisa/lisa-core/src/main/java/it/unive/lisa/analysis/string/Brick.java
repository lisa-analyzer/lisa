package it.unive.lisa.analysis.string;

import it.unive.lisa.util.numeric.IntInterval;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class Brick {
    private final Collection<String> strings;
    private final IntInterval brickInterval;

    public Brick(int min, int max, Collection<String> strings) {
        this.brickInterval = new IntInterval(min,max);
        this.strings = strings;
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

	@Override
	public String toString() {
		return "[ {min: " +
				this.getMin() +
				"}, {max: " +
				this.getMax() +
				"}, {strings: " +
				StringUtils.join(this.strings, ", ") +
				"} ]";
	}
}
