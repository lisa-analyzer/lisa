package it.unive.lisa.outputs.messages;

import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import org.apache.commons.lang3.StringUtils;

/**
 * A message reported by LiSA on the descriptor of one of the CFGs under
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGDescriptorMessage
		extends
		MessageWithLocation {

	/**
	 * The descriptor where this message was reported on
	 */
	private final CodeMemberDescriptor descriptor;

	/**
	 * Builds the message.
	 * 
	 * @param descriptor the descriptor where this message was reported on
	 * @param message    the message of this message
	 */
	public CFGDescriptorMessage(
			CodeMemberDescriptor descriptor,
			String message) {
		super(descriptor.getLocation(), message);
		this.descriptor = descriptor;
	}

	/**
	 * Yields the cfg where this message was reported on.
	 * 
	 * @return the column, or {@code -1}
	 */
	public CodeMemberDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public int compareTo(
			Message o) {
		int cmp;
		if ((cmp = super.compareTo(o)) != 0)
			return cmp;

		if (!(o instanceof CFGDescriptorMessage))
			return getClass().getName().compareTo(o.getClass().getName());

		CFGDescriptorMessage other = (CFGDescriptorMessage) o;
		if ((cmp = StringUtils.compare(descriptor.toString(), other.descriptor.toString())) != 0)
			return cmp;

		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
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
		CFGDescriptorMessage other = (CFGDescriptorMessage) obj;
		if (descriptor == null) {
			if (other.descriptor != null)
				return false;
		} else if (!descriptor.equals(other.descriptor))
			return false;
		return true;
	}

	@Override
	public String getTag() {
		return "DESCRIPTOR";
	}

	@Override
	public String toString() {
		return getLocationWithBrackets()
				+ " on '"
				+ descriptor.getFullSignatureWithParNames()
				+ "': "
				+ getTaggedMessage();
	}

}
