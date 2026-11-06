package it.unive.lisa.program.cfg.statement.literal;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;

/** A minimal concrete {@link Literal} wrapping an {@link Integer}. */
public class IntLiteral
		extends
		Literal<Integer> {

	public IntLiteral(
			CFG cfg,
			CodeLocation location,
			int value,
			Type staticType) {
		super(cfg, location, value, staticType);
	}

}
