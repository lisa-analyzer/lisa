package it.unive.lisa.program.cfg.statement.call;

import static it.unive.lisa.program.cfg.statement.call.CallFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.call.CallFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import org.junit.jupiter.api.Test;

public class CanRemoveReceiverTest {

	@Test
	public void truncateOfAnEmptyArrayReturnsItUnchanged() {
		Expression[] empty = new Expression[0];
		assertSame(empty, CanRemoveReceiver.truncate(empty));
	}

	@Test
	public void truncateOfASingleElementArrayReturnsAnEmptyArray() {
		CFG cfg = newCfg("c");
		Expression[] one = { new VariableRef(cfg, LOC, "x") };
		assertArrayEquals(new Expression[0], CanRemoveReceiver.truncate(one));
	}

	@Test
	public void truncateDropsExactlyTheFirstElementPreservingTheOrderOfTheRest() {
		CFG cfg = newCfg("c");
		VariableRef first = new VariableRef(cfg, LOC, "first");
		VariableRef second = new VariableRef(cfg, LOC, "second");
		VariableRef third = new VariableRef(cfg, LOC, "third");
		Expression[] truncated = CanRemoveReceiver.truncate(new Expression[] { first, second, third });
		assertEquals(2, truncated.length);
		assertSame(second, truncated[0]);
		assertSame(third, truncated[1]);
	}

}
