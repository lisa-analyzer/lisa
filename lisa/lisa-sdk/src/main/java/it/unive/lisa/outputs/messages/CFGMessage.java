package it.unive.lisa.outputs.messages;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import java.util.List;
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
		this(cfg, message, false, null);
	}

	/**
	 * Builds the message.
	 * 
	 * @param cfg               the cfg where this message was reported on
	 * @param message           the message of this message
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 */
	public CFGMessage(
			CFG cfg,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		this(cfg, message, null, definite, reproducingInputs);
	}

	/**
	 * Builds the message.
	 *
	 * @param cfg               the cfg where this message was reported on
	 * @param message           the message of this message
	 * @param context           the context where this message was reported on
	 *                              (can be {@code null})
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 */
	public CFGMessage(
			CFG cfg,
			String message,
			ScopeId<?> context,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		super(cfg.getDescriptor().getLocation(), message, context, definite, reproducingInputs);
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
	public String getCFGSignature() {
		return cfg.getDescriptor().getFullSignatureWithParNames();
	}

	@Override
	public String getTag() {
		return "CFG";
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
