package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.statement.literal.Literal;

public interface YieldsValue {

	Expression yieldedValue();

	default boolean isAtomic() {
		Expression value = yieldedValue();
		return value instanceof VariableRef || value instanceof Literal;
	}

	Statement withValue(
			Expression value);
}
