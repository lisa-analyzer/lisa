package it.unive.lisa.test.imp.tutorial.intervals;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class Intv {

	private static final Intv TOP = new Intv() {
		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}

		@Override
		public int hashCode() {
			return "TOP".hashCode();
		}
	};

	private static final Intv BOTTOM = new Intv() {
		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}

		@Override
		public int hashCode() {
			return "BOTTOM".hashCode();
		}

		@Override
		public String toString() {
			return "BOTTOM";
		}
	};

	private final Integer low;

	private final Integer high;

	private Intv() {
		low = null;
		high = null;
	}

	public Intv(Integer low, Integer high) {
		this.low = low;
		this.high = high;
	}

	public Integer getLow() {
		return low;
	}

	public boolean lowIsMinusInfinity() {
		return low == null;
	}

	public Integer getHigh() {
		return high;
	}

	public static Intv mkTop() {
		return TOP;
	}

	public static Intv mkBottom() {
		return BOTTOM;
	}

	public boolean isTop() {
		return this == TOP;
	}

	public boolean isBottom() {
		return this == BOTTOM;
	}

	/**
	 * Yields true if and only if the upper bound of this interval is +infinity.
	 * 
	 * @return true only if that condition holds
	 */
	public boolean highIsPlusInfinity() {
		return high == null;
	}

	public Intv bottom() {
		return getBottom();
	}

	/**
	 * Yields the unique bottom element.
	 * 
	 * @return the bottom element
	 */
	public static Intv getBottom() {
		return BOTTOM;
	}

	public Intv top() {
		return getTop();
	}

	public static Intv getTop() {
		return TOP;
	}

	protected Intv lub(Intv other) {
		Integer newLow = lowIsMinusInfinity() || other.lowIsMinusInfinity() ? null : Math.min(low, other.low);
		Integer newHigh = highIsPlusInfinity() || other.highIsPlusInfinity() ? null : Math.max(high, other.high);
		return new Intv(newLow, newHigh);
	}

	protected Intv widening(Intv other) {
		Integer newLow, newHigh;
		if (other.highIsPlusInfinity() || (!highIsPlusInfinity() && other.high > high))
			newHigh = null;
		else
			newHigh = other.high;

		if (other.lowIsMinusInfinity() || (!lowIsMinusInfinity() && other.low < low))
			newLow = null;
		else
			newLow = other.low;

		return new Intv(newLow, newHigh);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((high == null) ? 0 : high.hashCode());
		result = prime * result + ((low == null) ? 0 : low.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Intv other = (Intv) obj;
		if (high == null) {
			if (other.high != null)
				return false;
		} else if (!high.equals(other.high))
			return false;
		if (low == null) {
			if (other.low != null)
				return false;
		} else if (!low.equals(other.low))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + (lowIsMinusInfinity() ? "-Inf" : low) + ", " + (highIsPlusInfinity() ? "+Inf" : high) + "]";
	}

	public Intv plus(Intv other) {
		Integer newLow, newHigh;

		if (lowIsMinusInfinity() || other.lowIsMinusInfinity())
			newLow = null;
		else
			newLow = low + other.low;

		if (highIsPlusInfinity() || other.highIsPlusInfinity())
			newHigh = null;
		else
			newHigh = high + other.high;

		return new Intv(newLow, newHigh);
	}

	public Intv diff(Intv other) {
		Integer newLow, newHigh;

		if (other.highIsPlusInfinity() || lowIsMinusInfinity())
			newLow = null;
		else
			newLow = low - other.high;

		if (other.lowIsMinusInfinity() || highIsPlusInfinity())
			newHigh = null;
		else
			newHigh = high - other.low;

		return new Intv(newLow, newHigh);
	}

	public Intv mul(Intv other) {
		// this = [l1, h1]
		// other = [l2, h2]

		SortedSet<Integer> boundSet = new TreeSet<>();
		Integer l1 = low;
		Integer h1 = high;
		Integer l2 = other.low;
		Integer h2 = other.high;

		AtomicBoolean lowInf = new AtomicBoolean(false), highInf = new AtomicBoolean(false);

		// l1 * l2
		multiplyBounds(boundSet, l1, l2, lowInf, highInf);

		// x1 * y2
		multiplyBounds(boundSet, l1, h2, lowInf, highInf);

		// x2 * y1
		multiplyBounds(boundSet, h2, l2, lowInf, highInf);

		// x2 * y2
		multiplyBounds(boundSet, h1, h2, lowInf, highInf);

		return new Intv(lowInf.get() ? null : boundSet.first(), highInf.get() ? null : boundSet.last());
	}

	public Intv div(Intv other) {
		// this = [l1, h1]
		// other = [l2, h2]

		SortedSet<Integer> boundSet = new TreeSet<>();
		Integer l1 = low;
		Integer h1 = high;
		Integer l2 = other.low;
		Integer h2 = other.high;

		AtomicBoolean lowInf = new AtomicBoolean(false), highInf = new AtomicBoolean(false);

		// l1 / l2
		divideBounds(boundSet, l1, l2, lowInf, highInf);

		// x1 / y2
		divideBounds(boundSet, l1, h2, lowInf, highInf);

		// x2 / y1
		divideBounds(boundSet, h2, l2, lowInf, highInf);

		// x2 / y2
		divideBounds(boundSet, h1, h2, lowInf, highInf);

		return new Intv(lowInf.get() ? null : boundSet.first(), highInf.get() ? null : boundSet.last());
	}

	private void multiplyBounds(SortedSet<Integer> boundSet, Integer i, Integer j, AtomicBoolean lowInf,
			AtomicBoolean highInf) {
		if (i == null) {
			if (j == null)
				// -inf * -inf = +inf
				highInf.set(true);
			else {
				if (j > 0)
					// -inf * positive
					lowInf.set(true);
				else if (j < 0)
					// -inf * negative
					highInf.set(true);
				else
					boundSet.add(0);
			}
		} else if (j == null) {
			if (i > 0)
				// -inf * positive
				lowInf.set(true);
			else if (i < 0)
				// -inf * negative
				highInf.set(true);
			else
				boundSet.add(0);
		} else
			boundSet.add(i * j);
	}

	private void divideBounds(SortedSet<Integer> boundSet, Integer i, Integer j, AtomicBoolean lowInf,
			AtomicBoolean highInf) {
		if (i == null) {
			if (j == null)
				// -inf * -inf = +inf
				highInf.set(true);
			else {
				if (j > 0)
					// -inf * positive
					lowInf.set(true);
				else if (j < 0)
					// -inf * negative
					highInf.set(true);

				// division by zero!
			}
		} else if (j == null) {
			if (i > 0)
				// -inf * positive
				lowInf.set(true);
			else if (i < 0)
				// -inf * negative
				highInf.set(true);
			else
				boundSet.add(0);
		} else if (j != 0) {
			boundSet.add((int) Math.ceil(i / (double) j));
			boundSet.add((int) Math.floor(i / (double) j));
		}
		// division by zero!
	}

	public boolean lessOrEqual(Intv other) {
		if (equals(other))
			return true;
		return lessOrEqualLow(low, other.low) && lessOrEqualHigh(high, other.high);
	}

	// l1 >= l2
	private boolean lessOrEqualLow(Integer l1, Integer l2) {
		if (l1 == null) {
			if (l2 == null)
				return true;
			else
				return false;
		} else {
			if (l2 == null)
				return true;
			else
				return l1 >= l2;
		}
	}

	// h1 <= h2
	private boolean lessOrEqualHigh(Integer h1, Integer h2) {
		if (h1 == null) {
			if (h2 == null)
				return true;
			else
				return false;
		} else {
			if (h2 == null)
				return false;
			else
				return h1 <= h2;
		}
	}
}