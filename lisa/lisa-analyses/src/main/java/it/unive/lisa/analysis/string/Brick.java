package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Set;
import java.util.TreeSet;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * * The brick string abstract domain. * * @author
 * <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a> * @author
 * <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio * Salvatore
 * Evola</a> * * @see <a href= *
 * "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34"> *
 * https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
 */
public class Brick extends BaseNonRelationalValueDomain<Brick> {
	private final Set<String> strings;
	private final IntInterval brickInterval;

	private final static Brick TOP = new Brick();

	private final static Brick BOTTOM = new Brick(new IntInterval(1, 1), new TreeSet<>());

	/**
	 * Builds the top brick abstract element.
	 */
	public Brick() {
		this(new IntInterval(new MathNumber(0), MathNumber.PLUS_INFINITY), getAlphabet());
	}

	/**
	 * Builds a brick abstract element
	 * 
	 * @param min     a positive integer that represents the minimum
	 *                    concatenations of the strings set
	 * @param max     a positive integer that represents the maximum
	 *                    concatenations of the strings set
	 * @param strings the set of strings
	 * 
	 * @throws IllegalArgumentException if min or max are negative numbers.
	 */
	public Brick(int min, int max, Set<String> strings) {
		if (min < 0 || max < 0)
			throw new IllegalArgumentException();
		this.brickInterval = new IntInterval(min, max);
		this.strings = strings;
	}

	/**
	 * Builds a brick abstract element.
	 *
	 * @param interval an interval that yields the minimum of the brick and the
	 *                     maximum of the brick respectively
	 * @param strings  the set of strings
	 */
	public Brick(IntInterval interval, Set<String> strings) {
		this.brickInterval = interval;
		this.strings = strings;
	}

	@Override
	public Brick lubAux(Brick other) throws SemanticException {
		Set<String> resultStrings = new TreeSet<>();

		resultStrings.addAll(this.strings);
		resultStrings.addAll(other.strings);

		return new Brick(Math.min(this.getMin(), other.getMin()),
				Math.max(this.getMax(), other.getMax()),
				resultStrings);

	}

	@Override
	public boolean lessOrEqualAux(Brick other) throws SemanticException {
		if (this.strings.size() > other.strings.size())
			return false;

		if (other.strings.containsAll(this.strings))
			if (this.getMin() >= other.getMin())
				return this.getMax() <= other.getMax();

		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Brick brick = (Brick) o;
		return Objects.equals(strings, brick.strings) && Objects.equals(brickInterval, brick.brickInterval);
	}

	@Override
	public int hashCode() {
		return Objects.hash(strings, brickInterval);
	}

	/**
	 * Yields the min of this abstract value
	 * 
	 * @return the min of this abstract value
	 */
	public int getMin() {
		return this.brickInterval.getLow().getNumber().intValue();
	}

	/**
	 * Yields the max of this abstract value
	 * 
	 * @return the max of this abstract value
	 */
	public int getMax() {
		return this.brickInterval.getHigh().getNumber().intValue();
	}

	/**
	 * Yields the set of strings of this abstract value
	 * 
	 * @return the set of strings of this abstract value
	 */
	public Set<String> getStrings() {
		return strings;
	}

	@Override
	public Brick top() {
		return TOP;
	}

	@Override
	public Brick bottom() {
		return BOTTOM;
	}

	/**
	 * Yields all the possible concatenations between min and max of the strings
	 * set
	 * 
	 * @return the set of strings with all possible concatenations between min
	 *             and max
	 */
	public Set<String> getReps() {
		Set<String> reps = new TreeSet<>();

		if (this.strings.size() == 1) {
			String element = this.strings.iterator().next();
			reps.add(element.repeat(this.getMin()));
			reps.add(element.repeat(this.getMax()));
			return reps;
		}
		this.recGetReps(reps, this.getMin(), 0, "");

		return reps;
	}

	// Recursive function that gets all the possible combinations of the set
	// between min and max
	private void recGetReps(Set<String> reps, int min, int numberOfReps, String currentStr) {
		if (min > this.getMax() && numberOfReps >= this.getMin())
			reps.add(currentStr);
		else {
			for (String string : this.strings) {
				if ((!currentStr.equals("") || this.getMin() == 0) && numberOfReps >= this.getMin())
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

	private static Set<String> getAlphabet() {
		Set<String> alphabet = new TreeSet<>();

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
