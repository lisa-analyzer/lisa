package it.unive.lisa.program.annotations.values;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

/**
 * An array annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class ArrayAnnotationValue implements AnnotationValue {

	private final BasicAnnotationValue[] arr;

	/**
	 * Builds an array annotation value.
	 * 
	 * @param arr the array of basic annotation values
	 */
	public ArrayAnnotationValue(BasicAnnotationValue[] arr) {
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
	public boolean equals(Object obj) {
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
}
