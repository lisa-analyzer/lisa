package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.CodeLocation;

/**
 * Strategy for deciding the {@link CodeLocation} of a statement's clone during
 * {@link Statement#clone(CloneLocator)}. Called once per node in the cloned
 * subtree; the caller decides how each clone's location is derived from the
 * original.
 *
 * @author <a href="mailto:giacomo12596@gmail.com">Giacomo Zanatta</a>
 */
@FunctionalInterface
public interface CloneLocator {

	/**
	 * Yields the {@link CodeLocation} that the clone of {@code original} should
	 * carry.
	 *
	 * @param original the statement being cloned
	 *
	 * @return the location for its clone
	 */
	CodeLocation locationFor(
			Statement original);
}
