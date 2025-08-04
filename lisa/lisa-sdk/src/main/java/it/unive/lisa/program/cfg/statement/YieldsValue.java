package it.unive.lisa.program.cfg.statement;

public interface YieldsValue {

    Expression yieldedValue();

    Statement withValue(Expression value);
}
