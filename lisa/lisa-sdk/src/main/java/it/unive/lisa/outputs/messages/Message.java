package it.unive.lisa.outputs.messages;

import org.apache.commons.lang3.StringUtils;

/**
 * A message reported by LiSA on the program under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Message
		implements
		Comparable<Message> {

	/**
	 * The message of this message
	 */
	private final String message;

	/**
	 * Builds the message.
	 * 
	 * @param message the message of this message
	 */
	public Message(
			String message) {
		this.message = message;
	}

	/**
	 * Yields the message of this message.
	 * 
	 * @return the message of this message
	 */
	public final String getMessage() {
		return message;
	}

	/**
	 * Yields the tag of this message.
	 * 
	 * @return the tag of this message
	 */
	public String getTag() {
		return "GENERIC";
	}

	/**
	 * Yields the message of this message, preceeded by the tag under square
	 * brackets.
	 * 
	 * @return the tag and message of this message
	 */
	public final String getTaggedMessage() {
		return "[" + getTag() + "] " + getMessage();
	}

	@Override
	public int compareTo(
			Message o) {
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
		Message other = (Message) obj;
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
