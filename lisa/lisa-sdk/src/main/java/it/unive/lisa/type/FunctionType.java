package it.unive.lisa.type;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;

public interface FunctionType extends Type {

    CodeMember getCodeMember();

}
