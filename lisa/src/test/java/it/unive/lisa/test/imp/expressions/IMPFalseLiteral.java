package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Literal;
import it.unive.lisa.test.imp.types.BoolType;

public class IMPFalseLiteral extends Literal {

	public IMPFalseLiteral(CFG cfg, String sourceFile, int line, int col) {
		super(cfg, sourceFile, line, col, false, BoolType.INSTANCE);
	}

}
