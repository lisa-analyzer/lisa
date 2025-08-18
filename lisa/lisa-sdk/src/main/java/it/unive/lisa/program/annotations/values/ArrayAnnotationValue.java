package it.unive.lisa.program.annotations.values;

import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * An array annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class ArrayAnnotationValue implements AnnotationValue {

	private final BasicAnnotationValue[] arr;

	/**
	 * Builds an array annotation value.
	 * 
	 * @param arr the array of basic annotation values
	 */
	public ArrayAnnotationValue(
			BasicAnnotationValue[] arr) {
		this.arr = arr;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(arr);
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
		ArrayAnnotationValue other = (ArrayAnnotationValue) obj;
		if (!Arrays.equals(arr, other.arr))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return arr == null ? "[]" : "[" + StringUtils.join(arr, ", ") + "]";
	}

	@Override
	public int compareTo(
			AnnotationValue o) {
		if (!(o instanceof ArrayAnnotationValue))
			return getClass().getName().compareTo(o.getClass().getName());

		ArrayAnnotationValue other = (ArrayAnnotationValue) o;
		int cmp;
		if ((cmp = Integer.compare(arr.length, other.arr.length)) != 0)
			return cmp;

		CollectionsDiffBuilder<BasicAnnotationValue> builder = new CollectionsDiffBuilder<>(
			BasicAnnotationValue.class,
			List.of(arr),
			List.of(other.arr));
		builder.compute(BasicAnnotationValue::compareTo);

		if (builder.sameContent())
			return 0;

		return builder.getOnlyFirst().iterator().next().compareTo(builder.getOnlySecond().iterator().next());
	}

}
