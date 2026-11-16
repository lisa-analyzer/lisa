package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class InstrumentedReceiverTest {

	@Test
	public void nameDistinguishesArrayFromObjectReceivers() {
		InstrumentedReceiver arr = new InstrumentedReceiver(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE);
		InstrumentedReceiver obj = new InstrumentedReceiver(Untyped.INSTANCE, false, SyntheticLocation.INSTANCE);
		assertEquals("$array@" + SyntheticLocation.INSTANCE, arr.getName());
		assertEquals("$rec@" + SyntheticLocation.INSTANCE, obj.getName());
	}

	@Test
	public void getNameStaticHelperMatchesTheInstanceName() {
		assertEquals(
				InstrumentedReceiver.getName(true, SyntheticLocation.INSTANCE),
				new InstrumentedReceiver(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE).getName());
	}

	@Test
	public void isInstrumentedReceiverIsTrueUnlikeAPlainVariable() {
		InstrumentedReceiver rec = new InstrumentedReceiver(Untyped.INSTANCE, false, SyntheticLocation.INSTANCE);
		assertTrue(rec.isInstrumentedReceiver());

		Variable plain = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertFalse(plain.isInstrumentedReceiver());
	}

}
