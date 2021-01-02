package it.unive.lisa.util.collections;

import java.util.Comparator;

public class Utils {
	public static <T> int nullSafeCompare(boolean nullFirst, T left, T right, Comparator<T> comparator) {
		if (left == null && right != null)
			return nullFirst ? -1 : 1;
		
		if (left != null && right == null)
			return nullFirst ? 1 : -1;
		
		if (left == null)
			return 0;
		
		return comparator.compare(left, right);
	}
}
