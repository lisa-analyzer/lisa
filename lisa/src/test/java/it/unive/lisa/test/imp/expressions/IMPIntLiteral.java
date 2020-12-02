package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Literal;
import it.unive.lisa.test.imp.types.BoolType;
import it.unive.lisa.test.imp.types.FloatType;
import it.unive.lisa.test.imp.types.IntType;
import it.unive.lisa.test.imp.types.StringType;

public class IMPIntLiteral extends Literal {

	public IMPIntLiteral(CFG cfg, String sourceFile, int line, int col, int value) {
		super(cfg, sourceFile, line, col, value, IntType.INSTANCE);
	}
}
