package it.unive.lisa.checks.warnings;

import it.unive.lisa.program.cfg.CodeLocation;

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
	 * @param location the location in the source file where this warning is
	 *                     located. If unknown, use {@code, null}
	 * @param message  the message of this warning
	 */
	public WarningWithLocation(CodeLocation location, String message) {
		super(message);
		this.location = location;
	}

	/**
	 * Yields the location where this warning was reported.
	 * 
	 * @return the location of this warning
	 */
	public final String getLocation() {
		return location.toString();
	}

	/**
	 * Yields the location where this warning was reported, surrounded by square
	 * brackets.
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

		if ((cmp = location.compareTo(location)) != 0)
			return cmp;

		return super.compareTo(other);
	}

	@Override
	public abstract String toString();
}
