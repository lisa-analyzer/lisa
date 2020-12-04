package it.unive.lisa.test.logging;

import static it.unive.lisa.logging.TimeFormat.*;
import static org.junit.Assert.assertEquals;

import it.unive.lisa.logging.TimeFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.LongStream;
import org.junit.Test;

public class TimeFormatTest {

	private static final String UNIT_HOURS = "h";
	private static final String UNIT_MINUTES = "m";
	private static final String UNIT_SECONDS = "s";
	private static final String UNIT_MILLIS = "ms";
	private static final String UNIT_NANOS = "ns";

	@Test
	public void testNanos() {
		loopRunner(
				l -> TimeUnit.NANOSECONDS.toNanos(l) + UNIT_NANOS,
				l -> NANOS.format(l));
	}

	@Test
	public void testMillis() {
		loopRunner(
				l -> TimeUnit.NANOSECONDS.toMillis(l) + UNIT_MILLIS,
				l -> MILLIS.format(l));
	}

	@Test
	public void testSeconds() {
		loopRunner(
				l -> TimeUnit.NANOSECONDS.toSeconds(l) + UNIT_SECONDS,
				l -> SECONDS.format(l));
	}

	@Test
	public void testMinutes() {
		loopRunner(
				l -> TimeUnit.NANOSECONDS.toMinutes(l) + UNIT_MINUTES,
				l -> MINUTES.format(l));
	}

	@Test
	public void testHours() {
		loopRunner(
				l -> TimeUnit.NANOSECONDS.toHours(l) + UNIT_HOURS,
				l -> HOURS.format(l));
	}

	@Test
	public void testSecondsAndMillis() {
		loopRunner(
				l -> {
					long seconds = TimeUnit.NANOSECONDS.toSeconds(l);
					long millis = TimeUnit.NANOSECONDS.toMillis(l - TimeUnit.SECONDS.toNanos(seconds));
					String result = millis + UNIT_MILLIS;
					if (seconds != 0)
						result = seconds + UNIT_SECONDS + " " + result;
					return result;
				}, l -> TimeFormat.SECONDS_AND_MILLIS.format(l));
	}

	@Test
	public void testMinutesAndSeconds() {
		loopRunner(
				l -> {
					long minutes = TimeUnit.NANOSECONDS.toMinutes(l);
					long seconds = TimeUnit.NANOSECONDS.toSeconds(l - TimeUnit.MINUTES.toNanos(minutes));
					String result = seconds + UNIT_SECONDS;
					if (minutes != 0)
						result = minutes + UNIT_MINUTES + " " + result;
					return result;
				}, l -> TimeFormat.MINUTES_AND_SECONDS.format(l));
	}

	@Test
	public void testUpToMillis() {
		loopRunner(
				l -> {
					long millis = TimeUnit.NANOSECONDS.toMillis(l);
					long nanos = l - TimeUnit.MILLISECONDS.toNanos(millis);
					String result = nanos + UNIT_NANOS;
					if (millis != 0)
						result = millis + UNIT_MILLIS + " " + result;
					return result;
				}, l -> TimeFormat.UP_TO_MILLIS.format(l));
	}

	@Test
	public void testUpToSeconds() {
		loopRunner(
				l -> {
					long seconds = TimeUnit.NANOSECONDS.toSeconds(l);
					long millis = TimeUnit.NANOSECONDS.toMillis(l - TimeUnit.SECONDS.toNanos(seconds));
					long nanos = l - TimeUnit.SECONDS.toNanos(seconds) - TimeUnit.MILLISECONDS.toNanos(millis);
					String result = nanos + UNIT_NANOS;
					if (millis != 0)
						result = millis + UNIT_MILLIS + " " + result;
					if (seconds != 0)
						result = seconds + UNIT_SECONDS + " " + result;
					return result;
				}, l -> TimeFormat.UP_TO_SECONDS.format(l));
	}

	@Test
	public void testUpToMinutes() {
		loopRunner(
				l -> {
					long minutes = TimeUnit.NANOSECONDS.toMinutes(l);
					long seconds = TimeUnit.NANOSECONDS.toSeconds(l - TimeUnit.MINUTES.toNanos(minutes));
					long millis = TimeUnit.NANOSECONDS
							.toMillis(l - TimeUnit.MINUTES.toNanos(minutes) - TimeUnit.SECONDS.toNanos(seconds));
					long nanos = l - TimeUnit.MINUTES.toNanos(minutes) - TimeUnit.SECONDS.toNanos(seconds)
							- TimeUnit.MILLISECONDS.toNanos(millis);
					String result = nanos + UNIT_NANOS;
					if (millis != 0)
						result = millis + UNIT_MILLIS + " " + result;
					if (seconds != 0)
						result = seconds + UNIT_SECONDS + " " + result;
					if (minutes != 0)
						result = minutes + UNIT_MINUTES + " " + result;
					return result;
				}, l -> TimeFormat.UP_TO_MINUTES.format(l));
	}

	@Test
	public void testUpToHours() {
		loopRunner(
				l -> {
					long hours = TimeUnit.NANOSECONDS.toHours(l);
					long minutes = TimeUnit.NANOSECONDS.toMinutes(l - TimeUnit.HOURS.toNanos(hours));
					long seconds = TimeUnit.NANOSECONDS
							.toSeconds(l - TimeUnit.HOURS.toNanos(hours) - TimeUnit.MINUTES.toNanos(minutes));
					long millis = TimeUnit.NANOSECONDS.toMillis(l - TimeUnit.HOURS.toNanos(hours)
							- TimeUnit.MINUTES.toNanos(minutes) - TimeUnit.SECONDS.toNanos(seconds));
					long nanos = l - TimeUnit.HOURS.toNanos(hours) - TimeUnit.MINUTES.toNanos(minutes)
							- TimeUnit.SECONDS.toNanos(seconds) - TimeUnit.MILLISECONDS.toNanos(millis);
					String result = nanos + UNIT_NANOS;
					if (millis != 0)
						result = millis + UNIT_MILLIS + " " + result;
					if (seconds != 0)
						result = seconds + UNIT_SECONDS + " " + result;
					if (minutes != 0)
						result = minutes + UNIT_MINUTES + " " + result;
					if (hours != 0)
						result = hours + UNIT_HOURS + " " + result;
					return result;
				}, l -> TimeFormat.UP_TO_HOURS.format(l));
	}

	private void loopRunner(Function<Long, String> expectedGenerator, Function<Long, String> actualGenerator) {
		Random generator = new Random();
		LongStream
				.iterate(1 + generator.nextInt(9),
						l -> l = (1 + generator.nextInt(9)) * ((long) Math.pow(10, Math.log10(l))) + l)
				.limit((long) Math.log10(Long.MAX_VALUE))
				.forEach(random -> assertEquals("Conversion failed for long value " + random,
						expectedGenerator.apply(random),
						actualGenerator.apply(random)));
	}
}
