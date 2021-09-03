package it.unive.lisa.logging;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class IterationLoggerTest {

	private static final Logger logger = LogManager.getLogger(IterationLoggerTest.class);

	@Test
	public void testIterations() {
		Integer[] array = generateArray();
		int expected = 0;
		for (int i : array)
			expected += i;

		int sum = 0;
		for (Integer i : IterationLogger.iterate(logger, array, "Iteration test - array", "integers"))
			sum += i;
		assertEquals(expected, sum);

		sum = 0;
		for (Integer i : IterationLogger.iterate(logger, Level.OFF, array, "Iteration test - array", "integers"))
			sum += i;
		assertEquals(expected, sum);

		sum = 0;
		List<Integer> list = List.of(array);
		for (Integer i : IterationLogger.iterate(logger, list, "Iteration test - collection", "integers"))
			sum += i;
		assertEquals(expected, sum);

		sum = 0;
		for (Integer i : IterationLogger.iterate(logger, Level.OFF, list, "Iteration test - collection", "integers"))
			sum += i;
		assertEquals(expected, sum);

		sum = 0;
		for (Integer i : IterationLogger.iterate(logger, (Iterable<Integer>) list, "Iteration test - iterable",
				"integers"))
			sum += i;
		assertEquals(expected, sum);

		sum = 0;
		for (Integer i : IterationLogger.iterate(logger, Level.OFF, (Iterable<Integer>) list,
				"Iteration test - iterable", "integers"))
			sum += i;
		assertEquals(expected, sum);

		sum = 0;
		for (Integer i : IterationLogger.iterate(logger, list.stream(), "Iteration test - stream", "integers"))
			sum += i;
		assertEquals(expected, sum);

		sum = 0;
		for (Integer i : IterationLogger.iterate(logger, Level.OFF, list.stream(), "Iteration test - stream",
				"integers"))
			sum += i;
		assertEquals(expected, sum);
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
