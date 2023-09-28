package it.unive.lisa.analysis.string.bricks;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/**
 * The brick string abstract domain.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio
 *             Salvatore Evola</a>
 *
 * @see <a href=
 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
 */
public class Brick implements BaseNonRelationalValueDomain<Brick> {

	private final Set<String> strings;
	private final IntInterval interval;

	private final static Brick TOP = new Brick();

	private final static Brick BOTTOM = new Brick(new IntInterval(1, 1), new TreeSet<>());

	/**
	 * Builds the top brick abstract element.
	 */
	public Brick() {
		this(new IntInterval(new MathNumber(0), MathNumber.PLUS_INFINITY), null);
	}

	/**
	 * Builds a brick abstract element.
	 * 
	 * @param min     a positive integer that represents the minimum
	 *                    concatenations of the strings set
	 * @param max     a positive integer that represents the maximum
	 *                    concatenations of the strings set
	 * @param strings the set of strings
	 * 
	 * @throws IllegalArgumentException if min or max are negative numbers.
	 */
	public Brick(
			int min,
			int max,
			Set<String> strings) {
		if (min < 0 || max < 0)
			throw new IllegalArgumentException();
		this.interval = new IntInterval(min, max);
		this.strings = strings;
	}

	/**
	 * Builds a brick abstract element.
	 *
	 * @param min     a MathNumber that represents the minimum concatenations of
	 *                    the strings set
	 * @param max     a MathNumber that represents the maximum concatenations of
	 *                    the strings set
	 * @param strings the set of strings
	 */
	public Brick(
			MathNumber min,
			MathNumber max,
			Set<String> strings) {
		this.interval = new IntInterval(min, max);
		this.strings = strings;
	}

	/**
	 * Builds a brick abstract element.
	 *
	 * @param interval an interval that yields the minimum of the brick and the
	 *                     maximum of the brick respectively
	 * @param strings  the set of strings
	 */
	public Brick(
			IntInterval interval,
			Set<String> strings) {
		this.interval = interval;
		this.strings = strings;
	}

	@Override
	public Brick lubAux(
			Brick other)
			throws SemanticException {
		Set<String> resultStrings;
		if (strings == null || other.strings == null)
			resultStrings = null;
		else {
			resultStrings = new TreeSet<>();
			resultStrings.addAll(strings);
			resultStrings.addAll(other.strings);
		}

		return new Brick(this.getMin().min(other.getMin()),
				this.getMax().max(other.getMax()),
				resultStrings);

	}

	@Override
	public boolean lessOrEqualAux(
			Brick other)
			throws SemanticException {
		if (this.getMin().lt(other.getMin()))
			return false;
		if (this.getMax().gt(other.getMax()))
			return false;

		if (other.strings == null)
			return true;
		if (strings == null)
			return false;
		if (this.strings.size() > other.strings.size())
			return false;
		return other.strings.containsAll(this.strings);
	}

	@Override
	public Brick wideningAux(
			Brick other)
			throws SemanticException {
		MathNumber minOfMins = getMin().min(other.getMin());
		MathNumber maxOfMaxs = getMax().max(other.getMax());

		Set<String> resultSet = new TreeSet<>(getStrings());
		resultSet.addAll(other.getStrings());
		if (resultSet.size() > Bricks.kS)
			return TOP;
		else if (maxOfMaxs.subtract(minOfMins).geq(new MathNumber(Bricks.kI))) {
			IntInterval interval = new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
			return new Brick(interval, resultSet);
		} else
			return new Brick(minOfMins, maxOfMaxs, resultSet);
	}

	@Override
	public boolean equals(
			Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Brick brick = (Brick) o;
		return Objects.equals(strings, brick.strings) && Objects.equals(interval, brick.interval);
	}

	@Override
	public int hashCode() {
		return Objects.hash(strings, interval);
	}

	/**
	 * Yields the min of this abstract value.
	 * 
	 * @return the min of this abstract value
	 */
	public MathNumber getMin() {
		return this.interval.getLow();
	}

	/**
	 * Yields the max of this abstract value.
	 * 
	 * @return the max of this abstract value
	 */
	public MathNumber getMax() {
		return this.interval.getHigh();
	}

	/**
	 * Yields the set of strings of this abstract value.
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

	@Override
	public boolean isTop() {
		return this == TOP || equals(TOP);
	}

	/**
	 * Helper method to determine if the maximum of the Brick is Finite or not.
	 *
	 * @return true if the maximum of the Brick is Finite, false otherwise.
	 */
	public boolean isFinite() {
		return getMax().isFinite() && strings != null;
	}

	/**
	 * Yields all the possible concatenations between min and max of the strings
	 * set.
	 * 
	 * @return the set of strings with all possible concatenations between min
	 *             and max.
	 * 
	 * @throws IllegalStateException if the brick is not finite.
	 */
	public Set<String> getReps() {
		if (!isFinite())
			throw new IllegalStateException("Brick must be finite.");

		Set<String> reps = new TreeSet<>();

		try {
			if (this.strings.size() == 1) {
				String element = this.strings.iterator().next();
				reps.add(element.repeat(this.getMin().toInt()));
				reps.add(element.repeat(this.getMax().toInt()));
				return reps;
			}

			this.recGetReps(reps, this.getMin().toInt(), 0, "");
		} catch (MathNumberConversionException e) {
			throw new IllegalStateException("Brick must be finite.");
		}

		return reps;
	}

	// Recursive function that gets all the possible combinations of the set
	// between min and max
	private void recGetReps(
			Set<String> reps,
			int min,
			int numberOfReps,
			String currentStr)
			throws MathNumberConversionException {
		if (!isFinite())
			throw new IllegalStateException("Brick must be finite.");

		if (min > this.getMax().toInt() && numberOfReps >= this.getMin().toInt())
			reps.add(currentStr);
		else {
			for (String string : this.strings) {
				if ((!currentStr.equals("") || this.getMin().toInt() == 0) && numberOfReps >= this.getMin().toInt())
					reps.add(currentStr);

				recGetReps(reps, min + 1, numberOfReps + 1, currentStr + string);
			}
		}
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation("[{"
				+ (strings == null ? Lattice.TOP_STRING : StringUtils.join(this.strings, ", "))
				+ "}]("
				+ interval.getLow()
				+ ","
				+ interval.getHigh()
				+ ")");
	}
}
