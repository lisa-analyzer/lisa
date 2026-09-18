package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.Variable;
import org.junit.jupiter.api.Test;

public class InstrumentedReceiverRefTest {

	@Test
	public void getVariableOverridesTheDefaultToBuildAnInstrumentedReceiver() {
		CFG cfg = newCfg();
		InstrumentedReceiverRef arrayReceiver = new InstrumentedReceiverRef(cfg, LOC, true);
		Variable v = arrayReceiver.getVariable();
		assertEquals(InstrumentedReceiver.class, v.getClass());
		assertEquals(InstrumentedReceiver.getName(true, LOC), arrayReceiver.getName());
	}

	@Test
	public void arrayAndObjectReceiversAtTheSameLocationHaveDifferentNames() {
		CFG cfg = newCfg();
		InstrumentedReceiverRef array = new InstrumentedReceiverRef(cfg, LOC, true);
		InstrumentedReceiverRef object = new InstrumentedReceiverRef(cfg, LOC, false);
		assertNotEquals(array.getName(), object.getName());
	}

}
