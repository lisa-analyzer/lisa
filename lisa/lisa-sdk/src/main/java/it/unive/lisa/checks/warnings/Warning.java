package it.unive.lisa.checks.warnings;

import org.apache.commons.lang3.StringUtils;

/**
 * A warning reported by LiSA on the program under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Warning
		implements
		Comparable<Warning> {

	/**
	 * The message of this warning
	 */
	private final String message;

	/**
	 * Builds the warning.
	 * 
	 * @param message the message of this warning
	 */
	public Warning(
			String message) {
		this.message = message;
	}

	/**
	 * Yields the message of this warning.
	 * 
	 * @return the message of this warning
	 */
	public final String getMessage() {
		return message;
	}

	/**
	 * Yields the tag of this warning.
	 * 
	 * @return the tag of this warning
	 */
	public String getTag() {
		return "GENERIC";
	}

	/**
	 * Yields the message of this warning, preceeded by the tag under square
	 * brackets.
	 * 
	 * @return the tag and message of this warning
	 */
	public final String getTaggedMessage() {
		return "[" + getTag() + "] " + getMessage();
	}

	@Override
	public int compareTo(
			Warning o) {
		return StringUtils.compare(message, o.message);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
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
		Warning other = (Warning) obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getTaggedMessage();
	}

}
