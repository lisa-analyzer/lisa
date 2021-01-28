package it.unive.lisa.checks.warnings;

import it.unive.lisa.program.cfg.CFGDescriptor;
import org.apache.commons.lang3.StringUtils;

/**
 * A warning reported by LiSA on the descriptor of one of the CFGs under
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class CFGDesccriptorWarning extends WarningWithLocation {

	/**
	 * The descriptor where this warning was reported on
	 */
	private final CFGDescriptor descriptor;

	/**
	 * Builds the warning.
	 * 
	 * @param descriptor the descriptor where this warning was reported on
	 * @param message    the message of this warning
	 */
	public CFGDesccriptorWarning(CFGDescriptor descriptor, String message) {
		super(descriptor.getSourceFile(), descriptor.getLine(), descriptor.getCol(), message);
		this.descriptor = descriptor;
	}

	/**
	 * Yields the cfg where this warning was reported on.
	 * 
	 * @return the column, or {@code -1}
	 */
	public final CFGDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public int compareTo(Warning o) {
		if (!(o instanceof CFGDesccriptorWarning))
			return super.compareTo(o);

		CFGDesccriptorWarning other = (CFGDesccriptorWarning) o;
		int cmp;

		if ((cmp = StringUtils.compare(descriptor.toString(), other.descriptor.toString())) != 0)
			return cmp;

		return super.compareTo(other);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
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
		CFGDesccriptorWarning other = (CFGDesccriptorWarning) obj;
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
		return getLocationWithBrackets() + " on '" + descriptor.getFullSignatureWithParNames() + "': "
				+ getTaggedMessage();
	}
}