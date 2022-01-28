package it.unive.lisa.checks.warnings;

import it.unive.lisa.program.cfg.ImplementedCFG;
import org.apache.commons.lang3.StringUtils;

/**
 * A warning reported by LiSA on one of the CFGs under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGWarning extends WarningWithLocation {

	/**
	 * The cfg where this warning was reported on
	 */
	private final ImplementedCFG cfg;

	/**
	 * Builds the warning.
	 * 
	 * @param cfg     the cfg where this warning was reported on
	 * @param message the message of this warning
	 */
	public CFGWarning(ImplementedCFG cfg, String message) {
		super(cfg.getDescriptor().getLocation(), message);
		this.cfg = cfg;
	}

	/**
	 * Yields the cfg where this warning was reported on.
	 * 
	 * @return the cfg
	 */
	public final ImplementedCFG getCFG() {
		return cfg;
	}

	@Override
	public int compareTo(Warning o) {
		int cmp;
		if ((cmp = super.compareTo(o)) != 0)
			return cmp;

		if (!(o instanceof CFGWarning))
			return getClass().getName().compareTo(o.getClass().getName());

		CFGWarning other = (CFGWarning) o;
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CFGWarning other = (CFGWarning) obj;
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
		return getLocationWithBrackets() + " on '" + cfg.getDescriptor().getFullSignatureWithParNames() + "': "
				+ getTaggedMessage();
	}
}
