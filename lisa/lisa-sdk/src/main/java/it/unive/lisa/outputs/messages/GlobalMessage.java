package it.unive.lisa.outputs.messages;

import it.unive.lisa.program.Global;
import it.unive.lisa.program.Unit;
import org.apache.commons.lang3.StringUtils;

/**
 * A message reported by LiSA on one of the Globals under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class GlobalMessage
		extends
		MessageWithLocation {

	/**
	 * The nit containing the global where this message was reported on
	 */
	private final Unit unit;

	/**
	 * The global where this message was reported on
	 */
	private final Global global;

	/**
	 * Builds the message.
	 * 
	 * @param unit    the unit containing the global where this message was
	 *                    reported on
	 * @param global  the global where this message was reported on
	 * @param message the message of this message
	 */
	public GlobalMessage(
			Unit unit,
			Global global,
			String message) {
		super(global.getLocation(), message);
		this.unit = unit;
		this.global = global;
	}

	/**
	 * Yields the unit containing the global where this message was reported on.
	 * 
	 * @return the global
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * Yields the global where this message was reported on.
	 * 
	 * @return the global
	 */
	public Global getGlobal() {
		return global;
	}

	@Override
	public int compareTo(
			Message o) {
		int cmp;
		if ((cmp = super.compareTo(o)) != 0)
			return cmp;

		if (!(o instanceof GlobalMessage))
			return getClass().getName().compareTo(o.getClass().getName());

		GlobalMessage other = (GlobalMessage) o;
		if ((cmp = StringUtils.compare(unit.getName(), other.unit.getName())) != 0)
			return cmp;
		if ((cmp = StringUtils.compare(global.getName(), other.global.getName())) != 0)
			return cmp;

		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		result = prime * result + ((global == null) ? 0 : global.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GlobalMessage other = (GlobalMessage) obj;
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
		if (global == null) {
			if (other.global != null)
				return false;
		} else if (!global.equals(other.global))
			return false;
		return true;
	}

	@Override
	public String getTag() {
		return "GLOBAL";
	}

	@Override
	public String toString() {
		return getLocationWithBrackets()
				+ " on '"
				+ unit.getName()
				+ "::"
				+ global.getName()
				+ "': "
				+ getTaggedMessage();
	}

}
