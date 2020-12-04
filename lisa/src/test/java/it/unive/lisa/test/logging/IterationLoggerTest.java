package it.unive.lisa.test.logging;

import it.unive.lisa.logging.Counter;
import it.unive.lisa.logging.IterationLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class IterationLoggerTest {

	private static final Logger logger = LogManager.getLogger(IterationLoggerTest.class);

	@Test
	public void testIterations() {
		Integer[] array = generateArray();

		int sum = 0;
		for (Integer i : IterationLogger.iterate(logger, array, "Iteration test", "integers"))
			sum += i;

		logger.info("SUM THROUGH ITERATION: " + sum);
	}

	@Test
	public void testManual() {
		Integer[] array = generateArray();

		int sum = 0;
		Counter counter = new Counter(logger, Level.INFO, "Manual test", "integers", array.length, 0.1);
		counter.on();
		for (Integer i : array) {
			sum += i;
			counter.count();
		}
		counter.off();

		logger.info("MANUAL SUM: " + sum);
	}

	private Integer[] generateArray() {
		Integer[] array = new Integer[(int) (Math.random() * 1000)];
		for (int i = 0; i < array.length; i++)
			array[i] = (int) (Math.random() * 20);
		return array;
	}
}
