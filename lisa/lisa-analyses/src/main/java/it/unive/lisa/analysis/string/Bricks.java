package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.IntegerConstantPropagation;
import it.unive.lisa.analysis.value.StringAbstraction;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.IntegerConstant;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringCharAt;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringEqualsIgnoreCase;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringIsPrefixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIsSuffixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringLastIndexOfChar;
import it.unive.lisa.symbolic.value.operator.binary.StringMatches;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfCharFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringLastIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringStartsWithFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.StringReverse;
import it.unive.lisa.symbolic.value.operator.unary.StringToLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.StringToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.StringTrim;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/**
 * The bricks string abstract domain.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio
 *             Salvatore Evola</a>
 *
 * @see <a href=
 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
 */
public class Bricks
		implements
		StringAbstraction<ValueEnvironment<Bricks.BrickList>>,
		SmashedSumStringDomain<Bricks.BrickList> {

	/**
	 * A single brick, containing a set of strings repeated a given number of
	 * times.
	 *
	 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
	 * @author <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio
	 *             Salvatore Evola</a>
	 *
	 * @see <a href=
	 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
	 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
	 */
	public class Brick
			implements
			BaseLattice<Brick> {

		private final Set<String> strings;

		private final IntInterval interval;

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
		 * @param min     a MathNumber that represents the minimum
		 *                    concatenations of the strings set
		 * @param max     a MathNumber that represents the maximum
		 *                    concatenations of the strings set
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
		 * @param interval an interval that yields the minimum of the brick and
		 *                     the maximum of the brick respectively
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

			return new Brick(this.getMin().min(other.getMin()), this.getMax().max(other.getMax()), resultStrings);

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
			if (resultSet.size() > kS)
				return top();
			else if (maxOfMaxs.subtract(minOfMins).geq(new MathNumber(kI))) {
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
			return new Brick();
		}

		@Override
		public boolean isTop() {
			return interval.equals(new IntInterval(new MathNumber(0), MathNumber.PLUS_INFINITY)) && strings == null;
		}

		@Override
		public Brick bottom() {
			return new Brick(new IntInterval(1, 1), new TreeSet<>());
		}

		@Override
		public boolean isBottom() {
			return interval.is(1) && strings != null && strings.isEmpty();
		}

		/**
		 * Helper method to determine if the maximum of the Brick is Finite or
		 * not.
		 *
		 * @return true if the maximum of the Brick is Finite, false otherwise.
		 */
		public boolean isFinite() {
			return getMax().isFinite() && strings != null;
		}

		/**
		 * Yields all the possible concatenations between min and max of the
		 * strings set.
		 * 
		 * @return the set of strings with all possible concatenations between
		 *             min and max.
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

			return new StringRepresentation(
					"[{"
							+ (strings == null ? Lattice.TOP_STRING : StringUtils.join(this.strings, ", "))
							+ "}]("
							+ interval.getLow()
							+ ","
							+ interval.getHigh()
							+ ")");
		}

		/**
		 * Applies the given operator to all the strings in the set of this
		 * brick, yielding a new brick with the same interval and the new set of
		 * strings.
		 * 
		 * @param operator the operator to apply to all the strings in the set
		 *                     of this brick
		 * 
		 * @return a new brick with the same interval and the new set of strings
		 */
		public Brick onAllStrings(
				java.util.function.UnaryOperator<String> operator) {
			if (strings == null)
				return this;

			Set<String> newStrings = new TreeSet<>();
			for (String s : strings)
				newStrings.add(operator.apply(s));

			return new Brick(interval, newStrings);
		}

		/**
		 * Yields the length of the strings represented by this brick.
		 *
		 * @return the interval defining the length
		 */
		public IntInterval len() {
			if (isTop())
				return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
			if (isBottom())
				return IntInterval.BOTTOM;
			if (strings == null || strings.isEmpty())
				return IntInterval.ZERO;
			String min = Collections.min(strings, (
					s1,
					s2) -> Integer.compare(s1.length(), s2.length()));
			String max = Collections.max(strings, (
					s1,
					s2) -> Integer.compare(s1.length(), s2.length()));
			return new IntInterval(min.length(), max.length()).mul(interval);
		}
	}

	/**
	 * A list of bricks, containing a sequence of {@link Brick}s.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public class BrickList
			implements
			BaseLattice<BrickList> {

		private final List<Brick> bricks;

		/**
		 * Builds the top brick abstract element.
		 */
		public BrickList() {
			this.bricks = new ArrayList<>(1);
			bricks.add(new Brick().top());
		}

		/**
		 * Builds a bricks abstract element.
		 *
		 * @param bricks the list of brick
		 */
		public BrickList(
				List<Brick> bricks) {
			this.bricks = bricks;
		}

		@Override
		public BrickList lubAux(
				BrickList other)
				throws SemanticException {
			List<Brick> thisPaddedList = this.bricks;
			List<Brick> otherPaddedList = other.bricks;

			if (this.bricks.size() < other.bricks.size())
				thisPaddedList = this.padList(other);
			else if (other.bricks.size() < this.bricks.size())
				otherPaddedList = other.padList(this);

			List<Brick> resultBricks = new ArrayList<>(thisPaddedList.size());

			for (int i = 0; i < thisPaddedList.size(); ++i)
				resultBricks.add(thisPaddedList.get(i).lub(otherPaddedList.get(i)));

			BrickList result = new BrickList(resultBricks);
			result.normBricks();
			return result;
		}

		@Override
		public boolean lessOrEqualAux(
				BrickList other)
				throws SemanticException {
			List<Brick> thisPaddedList = this.bricks;
			List<Brick> otherPaddedList = other.bricks;

			if (this.bricks.size() < other.bricks.size())
				thisPaddedList = this.padList(other);
			else if (other.bricks.size() < this.bricks.size())
				otherPaddedList = other.padList(this);

			for (int i = 0; i < thisPaddedList.size(); ++i) {
				Brick first = thisPaddedList.get(i);
				Brick second = otherPaddedList.get(i);
				if (!first.lessOrEqual(second))
					return false;
			}

			return true;
		}

		@Override
		public BrickList wideningAux(
				BrickList other)
				throws SemanticException {
			boolean rel = this.lessOrEqual(other);
			if (!rel && !other.lessOrEqual(this))
				return top();

			if (this.bricks.size() > kL || other.bricks.size() > kL)
				return top();

			if (rel)
				return w(other);
			else
				return other.w(this);
		}

		private BrickList w(
				BrickList other)
				throws SemanticException {
			List<Brick> thisPaddedList = this.bricks;
			List<Brick> otherPaddedList = other.bricks;

			if (this.bricks.size() < other.bricks.size())
				thisPaddedList = this.padList(other);

			else if (other.bricks.size() < this.bricks.size())
				otherPaddedList = other.padList(this);

			List<Brick> resultList = new ArrayList<>();

			for (int i = 0; i < thisPaddedList.size(); ++i) {
				Brick thisCurrent = thisPaddedList.get(i);
				Brick otherCurrent = otherPaddedList.get(i);
				resultList.add(thisCurrent.widening(otherCurrent));
			}

			BrickList result = new BrickList(resultList);
			result.normBricks();
			return result;
		}

		@Override
		public boolean equals(
				Object object) {
			if (this == object)
				return true;
			if (object == null || getClass() != object.getClass())
				return false;
			BrickList bricks1 = (BrickList) object;
			return Objects.equals(bricks, bricks1.bricks);
		}

		@Override
		public int hashCode() {
			return Objects.hash(bricks);
		}

		@Override
		public BrickList top() {
			return new BrickList();
		}

		@Override
		public boolean isTop() {
			return bricks.size() == 1 && bricks.get(0).isTop();
		}

		@Override
		public BrickList bottom() {
			return new BrickList(new ArrayList<>());
		}

		@Override
		public boolean isBottom() {
			return bricks.isEmpty() || (bricks.size() == 1 && bricks.get(0).isBottom());
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

			return new StringRepresentation(StringUtils.join(this.bricks, " "));
		}

		private void rule2(
				int first,
				int second) {
			Brick firstBrick = this.bricks.get(first);
			Brick secondBrick = this.bricks.get(second);

			Set<String> resultSet;
			if (firstBrick.getStrings() == null || secondBrick.getStrings() == null)
				resultSet = null;
			else {
				resultSet = new TreeSet<>();
				firstBrick.getStrings()
						.forEach(string -> secondBrick.getStrings()
								.forEach(otherStr -> resultSet.add(string + otherStr)));
			}

			this.bricks.set(first, new Brick(1, 1, resultSet));
			this.bricks.remove(second);
		}

		private void rule3(
				int index) {
			Brick brick = this.bricks.get(index);

			this.bricks.set(index, new Brick(1, 1, brick.getReps()));
		}

		private void rule4(
				int first,
				int second) {
			Brick firstBrick = this.bricks.get(first);
			Brick secondBrick = this.bricks.get(second);

			this.bricks.set(
					first,
					new Brick(
							firstBrick.getMin().add(secondBrick.getMin()),
							firstBrick.getMax().add(secondBrick.getMax()),
							firstBrick.getStrings()));

			this.bricks.remove(second);
		}

		private void rule5(
				int index) {
			Brick brick = this.bricks.get(index);

			Brick br = new Brick(brick.getMin(), brick.getMin(), brick.getStrings());

			this.bricks.set(index, new Brick(1, 1, br.getReps()));
			this.bricks.add(
					index + 1,
					new Brick(MathNumber.ZERO, brick.getMax().subtract(brick.getMin()), brick.getStrings()));
		}

		/**
		 * The normalization method of the bricks domain. Modify bricks to its
		 * normalized form.
		 */
		public void normBricks() {
			if (isTop())
				return;

			List<Brick> thisBricks = this.bricks;
			List<Brick> tempList = new ArrayList<>(thisBricks);

			thisBricks.removeIf(
					brick -> brick.getMin().equals(MathNumber.ZERO)
							&& brick.getMax().equals(MathNumber.ZERO)
							&& brick.getStrings() != null
							&& brick.getStrings().isEmpty());

			for (int i = 0; i < thisBricks.size(); ++i) {
				Brick currentBrick = thisBricks.get(i);
				Brick nextBrick = null;
				boolean lastBrick = i == thisBricks.size() - 1;

				if (!lastBrick)
					nextBrick = thisBricks.get(i + 1);

				if (!lastBrick)
					if (currentBrick.getMin().equals(MathNumber.ONE)
							&& currentBrick.getMax().equals(MathNumber.ONE)
							&& nextBrick.getMin().equals(MathNumber.ONE)
							&& nextBrick.getMax().equals(MathNumber.ONE)) {

						rule2(i, i + 1);

						lastBrick = i == thisBricks.size() - 1;
					}

				if (currentBrick.getMin().equals(currentBrick.getMax())
						&& !currentBrick.getMin().equals(MathNumber.ONE)
						&& !currentBrick.getMax().equals(MathNumber.ONE)
						&& currentBrick.getStrings() != null)
					rule3(i);

				if (!lastBrick)
					if (currentBrick.getStrings() != null && currentBrick.getStrings().equals(nextBrick.getStrings()))
						rule4(i, i + 1);

				if (MathNumber.ONE.lt(currentBrick.getMin())
						&& !currentBrick.getMin().equals(currentBrick.getMax())
						&& currentBrick.getStrings() != null)
					rule5(i);
			}

			if (!thisBricks.equals(tempList))
				normBricks();
		}

		/**
		 * Pads the shortest brick list and adds empty brick elements to it, in
		 * order to make it the same size of the longer brick list, while
		 * maintaining the same position of equals elements between the two
		 * lists.
		 *
		 * @param other the other bricks object, which has to yield the longer
		 *                  list
		 *
		 * @return the shorter list with empty brick in it
		 *
		 * @throws IllegalArgumentException if the other brick list is longer or
		 *                                      equal than the caller bricks
		 *                                      object
		 */
		public List<Brick> padList(
				final BrickList other) {
			if (this.bricks.size() >= other.bricks.size())
				throw new IllegalArgumentException("Other bricks list is longer or equal");

			List<Brick> l1 = new ArrayList<>(this.bricks), l2 = new ArrayList<>(other.bricks);
			Brick e = new Brick(0, 0, new TreeSet<>());
			int n1 = l1.size();
			int n2 = l2.size();
			int n = n2 - n1;
			List<Brick> lnew = new ArrayList<>();
			int emptyBricksAdded = 0;

			for (int i = 0; i < n2; i++)
				if (emptyBricksAdded >= n) {
					lnew.add(l1.get(0));
					l1.remove(0);
				} else if (l1.isEmpty() || !l1.get(0).equals(l2.get(i))) {
					lnew.add(e);
					emptyBricksAdded++;
				} else {
					lnew.add(l1.get(0));
					l1.remove(0);
				}

			return lnew;
		}

		private Satisfiability contains(
				BrickList right) {
			if (right.bricks.size() != 1)
				return Satisfiability.UNKNOWN;

			if (!right.bricks.get(0).isFinite())
				return Satisfiability.UNKNOWN;

			Set<String> strings = right.bricks.get(0).getStrings();
			if (strings.size() != 1)
				return Satisfiability.UNKNOWN;

			if (strings.iterator().next().length() != 1)
				return Satisfiability.UNKNOWN;

			String c = strings.iterator().next();

			boolean res = bricks.stream()
					.filter(b -> b.getMin().gt(MathNumber.ZERO))
					.map(b -> b.getStrings())
					.anyMatch(set -> set == null || set.stream().allMatch(s -> s.contains(c)));
			if (res)
				return Satisfiability.SATISFIED;

			res = bricks.stream()
					.map(b -> b.getStrings())
					.allMatch(set -> set != null && set.stream().allMatch(s -> !s.contains(c)));
			if (res)
				return Satisfiability.NOT_SATISFIED;

			return Satisfiability.UNKNOWN;
		}

		/**
		 * Applies the given operator to all the strings in the set of each
		 * brick in this list, yielding a new brick list with the same bricks
		 * and the new set of strings.
		 *
		 * @param operator the operator to apply to all the strings in the set
		 *                     of each brick in this list
		 *
		 * @return a new brick list with the same bricks and the new set of
		 *             strings
		 */
		public BrickList onAllStrings(
				java.util.function.UnaryOperator<String> operator) {
			List<Brick> newBricks = new ArrayList<>();
			for (Brick b : bricks)
				newBricks.add(b.onAllStrings(operator));
			return new BrickList(newBricks);
		}

		/**
		 * Helper method to determine if the list represents a finite set of
		 * strings or not.
		 *
		 * @return true if the list represents a finite set of strings, false
		 *             otherwise.
		 */
		public boolean isFinite() {
			for (Brick b : bricks)
				if (!b.isFinite())
					return false;
			return true;
		}

		/**
		 * Yields all strings represented by this brick list, if it is finite.
		 * 
		 * @return the set of strings
		 * 
		 * @throws IllegalStateException if the brick is not finite.
		 */
		public Set<String> getReps() {
			if (!isFinite())
				throw new IllegalStateException("Brick list must be finite.");

			Set<String> reps = new TreeSet<>();
			for (Brick b : bricks)
				if (reps.isEmpty()) {
					reps.addAll(b.getReps());
				} else {
					Set<String> newReps = new TreeSet<>();
					for (String s1 : reps)
						for (String s2 : b.getReps())
							newReps.add(s1 + s2);
					reps = newReps;
				}

			return reps;
		}

		/**
		 * Yields the length of the strings represented by this brick list.
		 *
		 * @return the interval defining the length
		 */
		public IntInterval len() {
			if (isTop())
				return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
			if (isBottom())
				return IntInterval.BOTTOM;
			IntInterval result = IntInterval.ZERO;
			for (Brick b : bricks)
				result = result.plus(b.len());
			return result;
		}
	}

	/**
	 * The integer domain that we use to process numerical constraints.
	 */
	private final IntegerConstantPropagation intDomain = new IntegerConstantPropagation();

	/**
	 * The maximum length of a bricks list used in the widening.
	 */
	private final int kL;

	/**
	 * The maximum number of repetitions of a single brick used in the widening.
	 */
	private final int kI;

	/**
	 * The maximum number of strings in the set of a brick used in the widening.
	 */
	private final int kS;

	/**
	 * Builds the domain, using {@code 20} as the maximum length of a bricks
	 * list, {@code 20} as the maximum number of repetitions of a single brick
	 * and {@code 50} as the maximum number of strings in the set of a soingle
	 * brick.
	 */
	public Bricks() {
		this(20, 20, 50);
	}

	/**
	 * Builds the domain, using {@code kL} as the maximum length of a bricks
	 * list, {@code kI} as the maximum number of repetitions of a single brick
	 * and {@code kS} as the maximum number of strings in the set of a single
	 * brick.
	 * 
	 * @param kL the maximum length of a bricks list used in the widening
	 * @param kI the maximum number of repetitions of a single brick used in the
	 *               widening
	 * @param kS the maximum number of strings in the set of a single brick used
	 *               in the widening
	 */
	public Bricks(
			int kL,
			int kI,
			int kS) {
		this.kL = kL;
		this.kI = kI;
		this.kS = kS;
	}

	@Override
	public BrickList evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();

			Set<String> strings = new TreeSet<>();
			strings.add(str);

			List<Brick> resultList = new ArrayList<>();

			resultList.add(new Brick(1, 1, strings));

			return new BrickList(resultList);
		}
		return top();
	}

	@Override
	public BrickList evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (pushAny instanceof PushFromConstraints)
			return generate(((PushFromConstraints) pushAny).getConstraints(), pp, oracle);
		return SmashedSumStringDomain.super.evalPushAny(pushAny, pp, oracle);
	}

	@Override
	public BrickList evalUnaryExpression(
			UnaryExpression expression,
			BrickList arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();

		if (oracle.hasWholeValueAnlysis() && operator == NumericToString.INSTANCE) {
			Set<BinaryExpression> constraints = oracle.constraints(this, expression, pp);
			return generate(constraints, pp, oracle);
		}

		if (arg.isTop())
			return top();

		if (operator == StringReverse.INSTANCE)
			// not handled for now
			return top();
		else if (operator == StringToLowerCase.INSTANCE)
			return arg.onAllStrings(String::toLowerCase);
		else if (operator == StringToUpperCase.INSTANCE)
			return arg.onAllStrings(String::toUpperCase);
		else if (operator == StringTrim.INSTANCE)
			// not handled for now
			return top();

		return top();
	}

	@Override
	public BrickList evalBinaryExpression(
			BinaryExpression expression,
			BrickList left,
			BrickList right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (left.isTop())
			return left;

		if (oracle.hasWholeValueAnlysis()
				&& (operator == StringCharAt.INSTANCE
						|| operator == StringSubstringToEnd.INSTANCE)) {
			Set<BinaryExpression> constraints = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntegerConstant val = intDomain.generate(constraints, pp, oracle);
			if (val.isBottom())
				return bottom();
			if (val.isTop())
				return top();
			if (operator == StringCharAt.INSTANCE) {
				left.normBricks();

				Brick first = left.bricks.get(0);
				TreeSet<String> result = new TreeSet<>();

				if (first.getMin().equals(MathNumber.ONE)
						&& first.getMax().equals(MathNumber.ONE)
						&& first.getStrings() != null
						&& !first.getStrings().isEmpty()) {
					first.getStrings().forEach(s -> {
						if (s.length() > val.value)
							result.add("" + s.charAt((int) val.value));
					});
				}

				if (result.size() == first.getStrings().size()) {
					List<Brick> resultList = new ArrayList<>();
					resultList.add(new Brick(new IntInterval(1, 1), result));
					return new BrickList(resultList);
				}

				return top();
			}
			if (operator == StringSubstringToEnd.INSTANCE) {
				left.normBricks();

				Brick first = left.bricks.get(0);
				TreeSet<String> result = new TreeSet<>();

				if (first.getMin().equals(MathNumber.ONE)
						&& first.getMax().equals(MathNumber.ONE)
						&& first.getStrings() != null
						&& !first.getStrings().isEmpty()) {
					first.getStrings().forEach(s -> {
						if (s.length() >= val.value)
							result.add(s.substring((int) val.value));
					});
				}

				if (result.size() == first.getStrings().size()) {
					List<Brick> resultList = new ArrayList<>();
					resultList.add(new Brick(new IntInterval(1, 1), result));
					return new BrickList(resultList);
				}

				return top();
			}
		}

		if (right.isTop())
			return right;

		if (expression.getOperator() == StringConcat.INSTANCE) {
			List<Brick> resultList = new ArrayList<>(left.bricks);
			resultList.addAll(right.bricks);
			return new BrickList(resultList);
		}

		return left.top();
	}

	@Override
	public BrickList evalTernaryExpression(
			TernaryExpression expression,
			BrickList left,
			BrickList middle,
			BrickList right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();

		if (left.isTop())
			return top();

		if (oracle.hasWholeValueAnlysis() && operator == StringSubstring.INSTANCE) {
			Set<BinaryExpression> cM = oracle.constraints(this, (ValueExpression) expression.getMiddle(), pp);
			IntegerConstant mid = intDomain.generate(cM, pp, oracle);
			Set<BinaryExpression> cR = oracle.constraints(this, (ValueExpression) expression.getRight(), pp);
			IntegerConstant rig = intDomain.generate(cR, pp, oracle);
			if (mid.isBottom() || rig.isBottom())
				return bottom();
			if (mid.isTop() || rig.isTop())
				return top();

			left.normBricks();
			Brick first = left.bricks.get(0);
			TreeSet<String> result = new TreeSet<>();

			if (first.getMin().equals(MathNumber.ONE)
					&& first.getMax().equals(MathNumber.ONE)
					&& first.getStrings() != null
					&& !first.getStrings().isEmpty()) {
				first.getStrings().forEach(s -> {
					if (s.length() >= rig.value)
						result.add(s.substring((int) mid.value, (int) rig.value));
				});
			}

			if (result.size() == first.getStrings().size()) {
				List<Brick> resultList = new ArrayList<>();
				resultList.add(new Brick(new IntInterval(1, 1), result));
				return new BrickList(resultList);
			}

			return top();
		}

		if (right.isTop() || middle.isTop())
			return top();

		// if (operator instanceof StringReplace)
		// if (operator == StringReplaceAll.INSTANCE)
		// if (operator == StringReplaceFirst.INSTANCE)
		// not handled for now

		return top();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			BrickList left,
			BrickList right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE)
			return left.contains(right).and(right.contains(left));
		else if (operator == ComparisonNe.INSTANCE)
			return left.contains(right).negate().or(right.contains(left).negate());
		else if (operator == StringContains.INSTANCE)
			return left.contains(right);
		else if (operator == StringEndsWith.INSTANCE)
			return Satisfiability.UNKNOWN;
		else if (operator == StringEquals.INSTANCE)
			return left.contains(right).and(right.contains(left));
		else if (operator == StringEqualsIgnoreCase.INSTANCE) {
			BrickList leftLower = left.onAllStrings(String::toLowerCase);
			BrickList rightLower = right.onAllStrings(String::toLowerCase);
			return leftLower.contains(rightLower).and(rightLower.contains(leftLower));
		} else if (operator == StringMatches.INSTANCE)
			return Satisfiability.UNKNOWN;
		else if (operator == StringStartsWith.INSTANCE)
			return Satisfiability.UNKNOWN;
		else if (operator == StringIsPrefixOf.INSTANCE)
			return Satisfiability.UNKNOWN;
		else if (operator == StringIsSuffixOf.INSTANCE)
			return Satisfiability.UNKNOWN;
		else
			return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<BrickList> assumeBinaryExpression(
			ValueEnvironment<BrickList> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		BinaryOperator operator = expression.getOperator();
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (operator == ComparisonEq.INSTANCE) {
			if (left instanceof Identifier) {
				if (!canProcess(right, src, oracle))
					// the expression does not have a string value, we do not
					// assume anything on it
					return environment;
				BrickList eval = eval(environment, right, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				// If eval is TOP, the rhs is unknown. Any abstract value of lhs
				// satisfies lhs == TOP, so the lhs abstract value is preserved
				// and no refinement is needed.
				if (!eval.isTop())
					return environment.putState((Identifier) left, eval);
			} else if (right instanceof Identifier) {
				if (!canProcess(left, src, oracle))
					// the expression does not have a string value, we do not
					// assume anything on it
					return environment;
				BrickList eval = eval(environment, left, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				// Same reasoning as above, symmetric case.
				if (!eval.isTop())
					return environment.putState((Identifier) right, eval);
			}
		}
		return environment;
	}

	@Override
	public BrickList substring(
			BrickList current,
			long e,
			long b) {
		current.normBricks();

		Brick first = current.bricks.get(0);

		TreeSet<String> result = new TreeSet<>();

		if (first.getMin().equals(MathNumber.ONE)
				&& first.getMax().equals(MathNumber.ONE)
				&& first.getStrings() != null
				&& !first.getStrings().isEmpty()) {
			first.getStrings().forEach(s -> {
				boolean allGreater = s.length() >= e;

				if (allGreater)
					result.add(s.substring((int) e, (int) b));
			});
		}

		if (result.size() == first.getStrings().size()) {
			List<Brick> resultList = new ArrayList<>();

			resultList.add(new Brick(new IntInterval(1, 1), result));

			return new BrickList(resultList);
		}

		return current.top();
	}

	@Override
	public IntInterval length(
			BrickList current) {
		return current.len();
	}

	@Override
	public IntInterval indexOf(
			BrickList current,
			BrickList s) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			BrickList current,
			char c)
			throws SemanticException {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;

		Satisfiability sat = Satisfiability.BOTTOM;

		for (Brick b : current.bricks) {
			// surely a string of the brick is contained
			if (b.getMin().geq(MathNumber.ONE)) {
				Satisfiability bricksat = Satisfiability.BOTTOM;
				for (String s : b.getStrings())
					if (!s.contains(String.valueOf(c)))
						bricksat = bricksat.lub(Satisfiability.NOT_SATISFIED);
					else
						bricksat = bricksat.lub(Satisfiability.SATISFIED);

				if (bricksat == Satisfiability.SATISFIED)
					return bricksat;
				else
					sat = sat.lub(bricksat);
			} else if (b.isTop())
				sat = sat.lub(Satisfiability.UNKNOWN);
			else {
				// the brick can be missing
				for (String s : b.getStrings())
					if (s.contains(String.valueOf(c)))
						sat = sat.lub(Satisfiability.UNKNOWN);
					else
						sat = sat.lub(Satisfiability.NOT_SATISFIED);
			}
		}

		return sat;
	}

	@Override
	public BrickList top() {
		return new BrickList();
	}

	@Override
	public BrickList bottom() {
		return new BrickList().bottom();
	}

	private BrickList generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constraints == null)
			return bottom();

		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant) {
				String val = ((Constant) expr.getLeft()).getValue().toString();
				if (expr.getOperator() instanceof ComparisonEq) {
					Set<String> strings = new TreeSet<>();
					strings.add(val);
					List<Brick> resultList = new ArrayList<>();
					resultList.add(new Brick(1, 1, strings));
					return new BrickList(resultList);
				}
			}

		return top();
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<BrickList> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Collections.emptySet();
		if (state.isBottom())
			return null;

		if ((e instanceof UnaryExpression && ((UnaryExpression) e).getOperator() == LogicalNegation.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalAnd.INSTANCE)
				|| (e instanceof BinaryExpression && ((BinaryExpression) e).getOperator() == LogicalOr.INSTANCE)) {
			Satisfiability sat = satisfies(state, e, pp, oracle);
			if (sat == Satisfiability.SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
			else if (sat == Satisfiability.NOT_SATISFIED)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
			else if (sat == Satisfiability.UNKNOWN)
				return Collections.emptySet();
			else
				return null;
		}

		if (e instanceof UnaryExpression) {
			UnaryOperator operator = ((UnaryExpression) e).getOperator();
			if (operator == StringLength.INSTANCE) {
				ValueExpression arg = (ValueExpression) ((UnaryExpression) e).getExpression();
				BrickList value = eval(state, arg, pp, oracle);
				IntInterval len = value.len();
				if (value.isTop() || len.isTop())
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							0,
							null,
							e,
							pp);
				if (value.isBottom() || len.isBottom())
					return null;
				try {
					return ValueDomain.makeRangeConstraints(
							pp.getProgram().getTypes().getIntegerType(),
							len.getLow().toInt(),
							len.highIsPlusInfinity() ? null : len.getHigh().toInt(),
							e,
							pp);
				} catch (MathNumberConversionException e1) {
					// should not happen
					throw new SemanticException("Cannot convert math number to int", e1);
				}
			}
		}

		if (e instanceof BinaryExpression) {
			BinaryOperator operator = ((BinaryExpression) e).getOperator();
			if (operator == ComparisonEq.INSTANCE
					|| operator == ComparisonNe.INSTANCE
					|| operator == StringContains.INSTANCE
					|| operator == StringEndsWith.INSTANCE
					|| operator == StringEquals.INSTANCE
					|| operator == StringEqualsIgnoreCase.INSTANCE
					|| operator == StringMatches.INSTANCE
					|| operator == StringStartsWith.INSTANCE
					|| operator == StringIsPrefixOf.INSTANCE
					|| operator == StringIsSuffixOf.INSTANCE) {
				Satisfiability sat = satisfies(state, e, pp, oracle);
				if (sat == Satisfiability.SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
				else if (sat == Satisfiability.NOT_SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
				else if (sat == Satisfiability.UNKNOWN)
					return Collections.emptySet();
				else
					return null;
			} else if (operator == StringIndexOfChar.INSTANCE
					|| operator == StringLastIndexOfChar.INSTANCE
					|| operator == StringIndexOf.INSTANCE
					|| operator == StringLastIndexOf.INSTANCE
					|| operator == ValueComparison.INSTANCE) {
				return Collections.emptySet();
			}
		}

		if (e instanceof TernaryExpression) {
			TernaryOperator operator = ((TernaryExpression) e).getOperator();
			if (operator == StringIndexOfCharFromIndex.INSTANCE
					|| operator == StringIndexOfFromIndex.INSTANCE
					|| operator == StringLastIndexOfCharFromIndex.INSTANCE
					|| operator == StringLastIndexOfFromIndex.INSTANCE) {
				return Collections.emptySet();
			}
			if (operator == StringStartsWithFromIndex.INSTANCE) {
				Satisfiability sat = satisfies(state, e, pp, oracle);
				if (sat == Satisfiability.SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), true, e, pp);
				else if (sat == Satisfiability.NOT_SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(), false, e, pp);
				else if (sat == Satisfiability.UNKNOWN)
					return Collections.emptySet();
				else
					return null;
			}
		}

		BrickList value = eval(state, e, pp, oracle);
		if (value.isTop() || !value.isFinite() || value.getReps().size() != 1)
			return Collections.emptySet();
		if (value.isBottom())
			return null;
		return ValueDomain.makeEqConstraint(
				pp.getProgram().getTypes().getStringType(),
				value.getReps().iterator().next(),
				e,
				pp);
	}

}
