package it.unive.lisa.program.cfg.transform;

import it.unive.lisa.program.cfg.CFG;

/**
 * A rewrite over a single {@link CFG}, applied before the fixpoint.
 * Implementations mutate the CFG in place and are registered in the order they
 * should run through
 * {@link it.unive.lisa.conf.LiSAConfiguration#cfgTransformations}.
 *
 * @author <a href="mailto:giacomo12596@gmail.com">Giacomo Zanatta</a>
 */
public interface CFGTransformation {

	/**
	 * Applies this transformation to the given CFG in place. Implementations
	 * must preserve the concrete semantics of the CFG and leave it in a state
	 * that passes {@link CFG#validate()}.
	 *
	 * @param cfg the CFG to transform, mutated in place
	 */
	void transform(
			CFG cfg);
}
