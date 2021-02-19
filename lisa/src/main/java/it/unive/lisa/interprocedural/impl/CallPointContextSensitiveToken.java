package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.program.cfg.statement.Call;

import java.util.Objects;

public class CallPointContextSensitiveToken extends ContextSensitiveToken {
    private final Call callPoint;

    private CallPointContextSensitiveToken(Call callPoint) {
        this.callPoint = callPoint;
    }

    @Override
    public ContextSensitiveToken empty() {
        return new CallPointContextSensitiveToken(null);
    }

    @Override
    public ContextSensitiveToken pushCall(Call c) {
        return new CallPointContextSensitiveToken(c);
    }

    private static CallPointContextSensitiveToken singleton = new CallPointContextSensitiveToken(null);
    public static CallPointContextSensitiveToken getSingleton() {
        return singleton;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallPointContextSensitiveToken that = (CallPointContextSensitiveToken) o;
        return Objects.equals(callPoint, that.callPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callPoint);
    }

}
