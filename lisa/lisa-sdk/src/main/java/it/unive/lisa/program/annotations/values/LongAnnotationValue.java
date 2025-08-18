package it.unive.lisa.program.annotations.values;

/**
 * A long annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class LongAnnotationValue implements BasicAnnotationValue {

	private final long l;

	/**
	 * Builds a long annotation value.
	 * 
	 * @param l the long value
	 */
	public LongAnnotationValue(
			long l) {
		this.l = l;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (l ^ (l >>> 32));
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LongAnnotationValue other = (LongAnnotationValue) obj;
		if (l != other.l)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(l);
	}

	@Override
	public int compareTo(
			AnnotationValue o) {
		if (!(o instanceof LongAnnotationValue))
			return getClass().getName().compareTo(o.getClass().getName());

		LongAnnotationValue other = (LongAnnotationValue) o;
		return Long.compare(l, other.l);
	}

}
