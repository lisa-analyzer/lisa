package it.unive.lisa.checks.warnings;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.program.ClassUnit;
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
		super(unit instanceof ClassUnit ? ((ClassUnit) unit).getLocation() : SyntheticLocation.INSTANCE,
				message);
		this.unit = unit;
	}

	/**
	 * Yields the unit where this warning was reported on.
	 * 
	 * @return the unit
	 */
	public Unit getUnit() {
		return unit;
	}

	@Override
	public int compareTo(Warning o) {
		int cmp;
		if ((cmp = super.compareTo(o)) != 0)
			return cmp;

		if (!(o instanceof UnitWarning))
			return getClass().getName().compareTo(o.getClass().getName());

		UnitWarning other = (UnitWarning) o;
		if ((cmp = StringUtils.compare(unit.getName(), other.unit.getName())) != 0)
			return cmp;

		return 0;
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
