package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.program.cfg.statement.Call;

public class SingletonContextSensitiveToken extends ContextSensitiveToken {

    public static final SingletonContextSensitiveToken singleton = new SingletonContextSensitiveToken();

    private SingletonContextSensitiveToken() {}

    @Override
    public ContextSensitiveToken empty() {
        return this;
    }

    @Override
    public ContextSensitiveToken pushCall(Call c) {
        return this;
    }
}
