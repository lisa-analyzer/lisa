package it.unive.lisa.outputs.messages;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import java.util.List;
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
		this(descriptor, message, false, null);
	}

	/**
	 * Builds the message.
	 * 
	 * @param descriptor        the descriptor where this message was reported
	 *                              on
	 * @param message           the message of this message
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 */
	public CFGDescriptorMessage(
			CodeMemberDescriptor descriptor,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		super(descriptor.getLocation(), message, definite, reproducingInputs);
		this.descriptor = descriptor;
	}

	/**
	 * Yields the descriptor where this message was reported on.
	 *
	 * @return the descriptor
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
	public String getCFGSignature() {
		return descriptor.getFullSignatureWithParNames();
	}

	@Override
	public String getTag() {
		return "DESCRIPTOR";
	}

	@Override
	public String toString() {
		return getLocationWithBrackets()
				+ " on '"
				+ getCFGSignature()
				+ "': "
				+ getTaggedMessage();
	}

}
