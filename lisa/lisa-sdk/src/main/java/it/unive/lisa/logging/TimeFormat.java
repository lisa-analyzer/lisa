package it.unive.lisa.logging;

import java.util.concurrent.TimeUnit;

/**
 * Time formatting utility.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum TimeFormat {

	/**
	 * Formats the given elapsed nanos as nanoseconds.
	 */
	NANOS {

		@Override
		public String format(
				long nanos) {
			return String.valueOf(nanos) + UNIT_NANOS;
		}

	},

	/**
	 * Formats the given elapsed nanos as milliseconds.
	 */
	MILLIS {

		@Override
		public String format(
				long nanos) {
			return String.valueOf(TimeUnit.NANOSECONDS.toMillis(nanos)) + UNIT_MILLIS;
		}

	},

	/**
	 * Formats the given elapsed nanos as seconds.
	 */
	SECONDS {

		@Override
		public String format(
				long nanos) {
			return String.valueOf(TimeUnit.NANOSECONDS.toSeconds(nanos)) + UNIT_SECONDS;
		}

	},

	/**
	 * Formats the given elapsed nanos as minutes.
	 */
	MINUTES {

		@Override
		public String format(
				long nanos) {
			return String.valueOf(TimeUnit.NANOSECONDS.toMinutes(nanos)) + UNIT_MINUTES;
		}

	},

	/**
	 * Formats the given elapsed nanos as hours.
	 */
	HOURS {

		@Override
		public String format(
				long nanos) {
			return String.valueOf(TimeUnit.NANOSECONDS.toHours(nanos)) + UNIT_HOURS;
		}

	},

	/**
	 * Formats the given elapsed nanos as seconds and milliseconds.
	 */
	SECONDS_AND_MILLIS {

		@Override
		public String format(
				long nanos) {
			long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos);
			if (seconds == 0)
				return MILLIS.format(nanos);
			else
				return SECONDS.format(nanos) + " " + MILLIS.format(nanos - TimeUnit.SECONDS.toNanos(seconds));
		}

	},

	/**
	 * Formats the given elapsed nanos as minutes and seconds.
	 */
	MINUTES_AND_SECONDS {

		@Override
		public String format(
				long nanos) {
			long minutes = TimeUnit.NANOSECONDS.toMinutes(nanos);
			if (minutes == 0)
				return SECONDS.format(nanos);
			else
				return MINUTES.format(nanos) + " " + SECONDS.format(nanos - TimeUnit.MINUTES.toNanos(minutes));
		}

	},

	/**
	 * Formats the given elapsed nanos as milliseconds and nanoseconds.
	 */
	UP_TO_MILLIS {

		@Override
		public String format(
				long nanos) {
			long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
			if (millis == 0)
				return NANOS.format(nanos);
			else
				return MILLIS.format(nanos) + " " + NANOS.format(nanos - TimeUnit.MILLISECONDS.toNanos(millis));
		}

	},

	/**
	 * Formats the given elapsed nanos as seconds, milliseconds and nanoseconds.
	 */
	UP_TO_SECONDS {

		@Override
		public String format(
				long nanos) {
			long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos);
			long diff = nanos - TimeUnit.SECONDS.toNanos(seconds);
			long millis = TimeUnit.NANOSECONDS.toMillis(diff);
			if (seconds == 0)
				if (millis == 0)
					return NANOS.format(nanos);
				else
					return UP_TO_MILLIS.format(nanos);
			else
				return SECONDS.format(nanos) + " " + UP_TO_MILLIS.format(diff);
		}

	},

	/**
	 * Formats the given elapsed nanos as minutes, seconds, milliseconds and
	 * nanoseconds.
	 */
	UP_TO_MINUTES {

		@Override
		public String format(
				long nanos) {
			long minutes = TimeUnit.NANOSECONDS.toMinutes(nanos);
			long diff = nanos - TimeUnit.MINUTES.toNanos(minutes);
			long seconds = TimeUnit.NANOSECONDS.toSeconds(diff);
			long millis = TimeUnit.NANOSECONDS.toMillis(diff - TimeUnit.SECONDS.toNanos(seconds));
			if (minutes == 0)
				if (seconds == 0)
					if (millis == 0)
						return NANOS.format(nanos);
					else
						return UP_TO_MILLIS.format(nanos);
				else
					return UP_TO_SECONDS.format(nanos);
			else
				return MINUTES.format(nanos) + " " + UP_TO_SECONDS.format(diff);
		}

	},

	/**
	 * Formats the given elapsed nanos as hours, minutes, seconds, milliseconds
	 * and nanoseconds.
	 */
	UP_TO_HOURS {

		@Override
		public String format(
				long nanos) {
			long hours = TimeUnit.NANOSECONDS.toHours(nanos);
			long diff = nanos - TimeUnit.HOURS.toNanos(hours);
			long minutes = TimeUnit.NANOSECONDS.toMinutes(diff);
			long diff1 = diff - TimeUnit.MINUTES.toNanos(minutes);
			long seconds = TimeUnit.NANOSECONDS.toSeconds(diff1);
			long millis = TimeUnit.NANOSECONDS.toMillis(diff1 - TimeUnit.SECONDS.toNanos(seconds));
			if (hours == 0)
				if (minutes == 0)
					if (seconds == 0)
						if (millis == 0)
							return NANOS.format(nanos);
						else
							return UP_TO_MILLIS.format(nanos);
					else
						return UP_TO_SECONDS.format(nanos);
				else
					return UP_TO_MINUTES.format(nanos);
			else
				return HOURS.format(nanos) + " " + UP_TO_MINUTES.format(diff);
		}

	};

	private static final String UNIT_HOURS = "h";

	private static final String UNIT_MINUTES = "m";

	private static final String UNIT_SECONDS = "s";

	private static final String UNIT_MILLIS = "ms";

	private static final String UNIT_NANOS = "ns";

	/**
	 * Formats the given elapsed nanos into a string with time units.
	 * 
	 * @param nanos the elapsed time, in nanoseconds
	 * 
	 * @return the formatted string
	 */
	public String format(
			long nanos) {
		return String.valueOf(nanos);
	}

}
