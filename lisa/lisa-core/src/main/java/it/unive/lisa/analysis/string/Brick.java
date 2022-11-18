package it.unive.lisa.analysis.string;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class Brick {
	private int min;
	private int max;
	private Collection<String> strings;

	public Brick(int min, int max, Collection<String> strings) {
		this.min = min;
		this.max = max;
		this.strings = strings;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Brick brick = (Brick) o;
		return min == brick.min && max == brick.max && Objects.equals(strings, brick.strings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(min, max, strings);
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public void setMin(int min){
		this.min = min;
	}
	public void setMax(int max){
		this.max = max;
	}

	public void setStrings(Collection<String> strings){
		this.strings = strings;
	}

	public Collection<String> getStrings() {
		return strings;
	}

	public Brick merge(Brick other) {
		Collection<String> resultSet = new HashSet<>();

		for(String string: this.strings){
			for(String otherStr: other.strings){
				resultSet.add(string + otherStr);
			}
		}

		return new Brick(1,1,resultSet);
	}

	public Collection<String> getReps() {
		HashSet<String> reps = new HashSet<>();

		if(this.strings.size() == 1) {
			String element = this.strings.iterator().next();
			reps.add(element.repeat(this.min));
			reps.add(element.repeat(this.max));
			return reps;
		}
		this.recGetReps(reps, this.min,0,"");

		return reps;
	}

	//Recursive function that gets all the possible combinations of the set between min and max
	private void recGetReps(HashSet<String> reps, int min, int numberOfReps, String currentStr) {
		if (min > this.max && numberOfReps >= this.min)//If the number of reps (starting from min) exceeds the max, then stops the recursion
			reps.add(currentStr);
		else {
			for (String string : this.strings) {
				if((!currentStr.equals("") || this.min == 0) && numberOfReps >= this.min) //numberOfReps has to be ALWAYS greater or equal than the min
					reps.add(currentStr);

				recGetReps(reps, min + 1, numberOfReps + 1,currentStr + string);
				}
			}
		}
}
