package it.unive.lisa.outputs.messages;

import it.unive.lisa.program.cfg.CFG;
import org.apache.commons.lang3.StringUtils;

/**
 * A message reported by LiSA on one of the CFGs under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGMessage
		extends
		MessageWithLocation {

	/**
	 * The cfg where this message was reported on
	 */
	private final CFG cfg;

	/**
	 * Builds the message.
	 * 
	 * @param cfg     the cfg where this message was reported on
	 * @param message the message of this message
	 */
	public CFGMessage(
			CFG cfg,
			String message) {
		super(cfg.getDescriptor().getLocation(), message);
		this.cfg = cfg;
	}

	/**
	 * Yields the cfg where this message was reported on.
	 * 
	 * @return the cfg
	 */
	public final CFG getCFG() {
		return cfg;
	}

	@Override
	public int compareTo(
			Message o) {
		int cmp;
		if ((cmp = super.compareTo(o)) != 0)
			return cmp;

		if (!(o instanceof CFGMessage))
			return getClass().getName().compareTo(o.getClass().getName());

		CFGMessage other = (CFGMessage) o;
		if ((cmp = StringUtils.compare(cfg.getDescriptor().toString(), other.cfg.getDescriptor().toString())) != 0)
			return cmp;

		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((cfg == null) ? 0 : cfg.hashCode());
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
		CFGMessage other = (CFGMessage) obj;
		if (cfg == null) {
			if (other.cfg != null)
				return false;
		} else if (!cfg.equals(other.cfg))
			return false;
		return true;
	}

	@Override
	public String getTag() {
		return "CFG";
	}

	@Override
	public String toString() {
		return getLocationWithBrackets()
				+ " on '"
				+ cfg.getDescriptor().getFullSignatureWithParNames()
				+ "': "
				+ getTaggedMessage();
	}

}
