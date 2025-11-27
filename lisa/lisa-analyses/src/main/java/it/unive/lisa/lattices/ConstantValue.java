package it.unive.lisa.lattices;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class ConstantValue
		implements
		BaseLattice<ConstantValue> {

	public static final ConstantValue TOP = new ConstantValue(false);
	public static final ConstantValue BOTTOM = new ConstantValue(true);

	private final boolean isBottom;

	private final Object value;

	public ConstantValue() {
		this(null, false);
	}

	private ConstantValue(
			boolean isBottom) {
		this(null, isBottom);
	}

	private ConstantValue(
			Object value,
			boolean isBottom) {
		this.value = value;
		this.isBottom = isBottom;
	}

	/**
	 * Builds the abstract value for the given constant.
	 * 
	 * @param value the constant
	 */
	public ConstantValue(
			Object value) {
		this(value, false);
	}

	public Object getValue() {
		return value;
	}

	public <T> boolean is(
			Class<T> type) {
		return type.isInstance(getValue());
	}

	public <T> T as(
			Class<T> type) {
		return type.cast(getValue());
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

		return new StringRepresentation(value.toString());
	}

	@Override
	public ConstantValue top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return value == null && !isBottom;
	}

	@Override
	public ConstantValue bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return value == null && isBottom;
	}

	@Override
	public ConstantValue lubAux(
			ConstantValue other)
			throws SemanticException {
		return TOP;
	}

	@Override
	public ConstantValue wideningAux(
			ConstantValue other)
			throws SemanticException {
		return lubAux(other);
	}

	@Override
	public boolean lessOrEqualAux(
			ConstantValue other)
			throws SemanticException {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isBottom ? 1231 : 1237);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		ConstantValue other = (ConstantValue) obj;
		if (isBottom != other.isBottom)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	public boolean isNumeric() {
		return value instanceof Long || value instanceof Integer || value instanceof Double || value instanceof Float
				|| value instanceof Short;
	}
}