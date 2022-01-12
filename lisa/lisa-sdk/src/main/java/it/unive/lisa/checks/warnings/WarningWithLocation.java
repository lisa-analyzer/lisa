package it.unive.lisa.checks.warnings;

import it.unive.lisa.program.cfg.CodeLocation;
import java.util.Objects;

/**
 * A warning reported by LiSA on the program under analysis. This warning is
 * tied to a location, i.e. it might have information about source file, line
 * number and column. This does not mean that it will always have them, since
 * CFGs and statements might have been built without that information.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class WarningWithLocation extends Warning {

	private final CodeLocation location;

	/**
	 * Builds the warning.
	 * 
	 * @param location the location in the program where this warning is located
	 * @param message  the message of this warning
	 */
	protected WarningWithLocation(CodeLocation location, String message) {
		super(message);
		Objects.requireNonNull(location, "The location of a warning with location cannot be null");
		this.location = location;
	}

	/**
	 * Yields the location where this warning was reported.
	 * 
	 * @return the location of this warning
	 */
	public final CodeLocation getLocation() {
		return location;
	}

	/**
	 * Yields the string representation of the location where this warning was
	 * reported, surrounded by square brackets.
	 * 
	 * @return the location of this warning surrounded by brackets
	 */
	public final String getLocationWithBrackets() {
		return "[" + getLocation() + "]";
	}

	@Override
	public int compareTo(Warning o) {
		if (!(o instanceof WarningWithLocation))
			return super.compareTo(o);

		WarningWithLocation other = (WarningWithLocation) o;
		int cmp;

		if ((cmp = location.compareTo(other.location)) != 0)
			return cmp;

		return super.compareTo(other);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		WarningWithLocation other = (WarningWithLocation) obj;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		return true;
	}

	@Override
	public abstract String toString();
}
