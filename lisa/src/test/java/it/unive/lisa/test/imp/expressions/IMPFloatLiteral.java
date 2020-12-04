package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Literal;
import it.unive.lisa.test.imp.types.FloatType;

public class IMPFloatLiteral extends Literal {

	public IMPFloatLiteral(CFG cfg, String sourceFile, int line, int col, float value) {
		super(cfg, sourceFile, line, col, value, FloatType.INSTANCE);
	}
}
