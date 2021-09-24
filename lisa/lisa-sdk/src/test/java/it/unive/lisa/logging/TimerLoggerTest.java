package it.unive.lisa.logging;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class TimerLoggerTest {
	private static final Logger logger = LogManager.getLogger(TimerLoggerTest.class);

	@Test
	public void testSupplier() {
		Integer result = TimerLogger.execSupplier(logger, Level.INFO, "Test supplier logging",
				this::supplier);
		assertEquals(5, result.intValue());
		result = TimerLogger.execSupplier(logger, Level.INFO, TimeFormat.MILLIS, "Test supplier logging",
				this::supplier);
		assertEquals(5, result.intValue());
	}

	private int supplier() {
		logAction();
		return 5;
	}

	private void logAction() {
		TimerLogger.execAction(logger, Level.INFO, TimeFormat.MILLIS, "Test action logging", this::action);
		TimerLogger.execAction(logger, Level.INFO, "Test action logging", this::action);
	}

	private void action() {
		System.out.println("Going to sleep...");
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Waking up!");
	}
}
