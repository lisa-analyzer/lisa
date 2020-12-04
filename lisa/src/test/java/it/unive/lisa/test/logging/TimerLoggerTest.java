package it.unive.lisa.test.logging;

import static org.junit.Assert.assertEquals;

import it.unive.lisa.logging.TimeFormat;
import it.unive.lisa.logging.TimerLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class TimerLoggerTest {
	private static final Logger logger = LogManager.getLogger(TimerLoggerTest.class);

	@Test
	public void testSupplier() {
		Integer result = TimerLogger.execSupplier(logger, Level.INFO, TimeFormat.MILLIS, "Test supplier logging",
				this::supplier);
		assertEquals(5, result.intValue());
	}

	@Test
	public void testFunction() {
		for (double d = 0; d < 3; d++) {
			Integer result = TimerLogger.execFunction(logger, Level.INFO, TimeFormat.MILLIS, "Test function logging",
					v -> function(v), d);
			assertEquals(5 + (int) d, result.intValue());
		}
	}

	private int function(double par) {
		logAction();
		return 5 + (int) par;
	}

	private int supplier() {
		logAction();
		return 5;
	}

	private void logAction() {
		TimerLogger.execAction(logger, Level.INFO, TimeFormat.MILLIS, "Test action logging", this::action);
	}

	private void action() {
		System.out.println("Going to sleep...");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Waking up!");
	}
}
