package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.program.cfg.statement.Call;

abstract public class ContextSensitiveToken {

    abstract public ContextSensitiveToken empty();

    abstract public ContextSensitiveToken pushCall(Call c);
}
