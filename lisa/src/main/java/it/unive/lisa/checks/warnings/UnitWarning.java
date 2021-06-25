package it.unive.lisa.checks.warnings;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.Unit;

/**
 * A warning reported by LiSA on one of the Units under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UnitWarning extends WarningWithLocation {

	/**
	 * The unit where this warning was reported on
	 */
	private final Unit unit;

	/**
	 * Builds the warning.
	 * 
	 * @param unit    the unit where this warning was reported on
	 * @param message the message of this warning
	 */
	public UnitWarning(Unit unit, String message) {
		super(unit instanceof CompilationUnit ? ((CompilationUnit) unit).getLocation() : SyntheticLocation.INSTANCE,
				message);
		this.unit = unit;
	}

	/**
	 * Yields the unit where this warning was reported on.
	 * 
	 * @return the unit
	 */
	public final Unit getUnit() {
		return unit;
	}

	@Override
	public int compareTo(Warning o) {
		if (!(o instanceof UnitWarning))
			return super.compareTo(o);

		UnitWarning other = (UnitWarning) o;
		int cmp;

		if ((cmp = StringUtils.compare(unit.getName(), other.unit.getName())) != 0)
			return cmp;

		return super.compareTo(other);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
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
		UnitWarning other = (UnitWarning) obj;
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
		return true;
	}

	@Override
	public String getTag() {
		return "UNIT";
	}

	@Override
	public String toString() {
		return getLocationWithBrackets() + " on '" + unit.getName() + "': "
				+ getTaggedMessage();
	}
}