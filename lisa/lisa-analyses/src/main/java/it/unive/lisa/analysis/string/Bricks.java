package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.ArrayList;
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

	}

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
	public BrickList evalBinaryExpression(
			BinaryExpression expression,
			BrickList left,
			BrickList right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringConcat.INSTANCE) {
			List<Brick> resultList = new ArrayList<>(left.bricks);
			resultList.addAll(right.bricks);
			return new BrickList(resultList);
		}
		return left.top();
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
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			BrickList left,
			BrickList right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop())
			return Satisfiability.UNKNOWN;

		if (expression.getOperator() == StringContains.INSTANCE)
			return left.contains(right);

		return Satisfiability.UNKNOWN;
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
		return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
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

}
