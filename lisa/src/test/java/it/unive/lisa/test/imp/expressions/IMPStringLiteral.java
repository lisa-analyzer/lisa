package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Literal;
import it.unive.lisa.test.imp.types.StringType;

public class IMPStringLiteral extends Literal {

	public IMPStringLiteral(CFG cfg, String sourceFile, int line, int col, String value) {
		super(cfg, sourceFile, line, col, value, StringType.INSTANCE);
	}
}
