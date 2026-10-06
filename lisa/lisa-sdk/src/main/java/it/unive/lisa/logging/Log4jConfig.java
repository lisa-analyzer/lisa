package it.unive.lisa.logging;

import it.unive.lisa.LiSA;
import it.unive.lisa.conf.LiSAConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.AppenderRefComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Utility class to check and initialize Log4j logging configuration. This class
 * provides methods to verify if Log4j is configured and to set up a default
 * logging configuration if it is not. The automatic checking and setup is
 * performed when the {@link LiSA} class or the {@link LiSAConfiguration} class
 * are first accessed, which is a reasonable time to ensure that logging is
 * properly configured before the analysis starts.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Log4jConfig {

	private static final Logger LOG = LogManager.getLogger(Log4jConfig.class);

	/**
	 * Checks if Log4j is configured. This amounts to checking if the
	 * configuration the default one provided by Log4j, or if a custom
	 * configuration has been set up.
	 *
	 * @return whether Log4j is configured with a custom configuration or not
	 */
	public static boolean isLog4jConfigured() {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration config = context.getConfiguration();
		return !config.getClass().getSimpleName().equals("DefaultConfiguration");
	}

	/**
	 * Initializes Log4j logging with a default configuration. This sets up a
	 * console appender that outputs logs to the system's standard output, with
	 * a specific pattern for log messages. It also sets the logging levels for
	 * various dependencies, ensuring that verbose logs are omitted from the
	 * analysis logs.
	 */
	public static void initializeLogging() {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

		builder.setStatusLevel(Level.WARN);
		builder.setConfigurationName("LiSADefaultConfig");

		// colors and self-overwriting progress bars only make sense when
		// attached to a real terminal: when the output is redirected or
		// captured (e.g. by Gradle or a test runner), System.console() is
		// null, and we fall back to a plain, capture-friendly format instead
		boolean interactive = System.console() != null;
		String disableAnsi = interactive ? "false" : "true";
		String pattern = "%m %ex";
		String level = "%equals{%level: }{INFO: }{}";
		if (interactive)
			pattern = "\u001B[2K\r%highlight{" + level + "}" + "%highlight{" + pattern
					+ "}{INFO=bright_white, DEBUG=bright_white, TRACE=bright_white}";
		else
			pattern = level + pattern;

		// Normal console appender: everything except PROGRESS-marked logs
		LayoutComponentBuilder plainLayout = builder.newLayout("PatternLayout");
		plainLayout.addAttribute("pattern", pattern + "%n");
		plainLayout.addAttribute("disableAnsi", disableAnsi);
		FilterComponentBuilder denyProg = builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.ACCEPT);
		denyProg.addAttribute("marker", "PROGRESS");
		AppenderComponentBuilder console = builder.newAppender("ConsoleNormal", "CONSOLE");
		console.addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
		console.add(plainLayout);
		console.addComponent(denyProg);
		builder.add(console);
		AppenderRefComponentBuilder normalRef = builder.newAppenderRef("ConsoleNormal");

		// Progress console appender: overwrites the same line. Only
		// added when running interactively, since without ANSI cursor
		// control the repeated updates would print one line per update
		// instead of overwriting it, flooding captured output
		LayoutComponentBuilder progLayout = builder.newLayout("PatternLayout");
		progLayout.addAttribute("pattern", pattern);
		progLayout.addAttribute("disableAnsi", disableAnsi);
		FilterComponentBuilder allowProg = builder.newFilter("MarkerFilter", Filter.Result.ACCEPT, Filter.Result.DENY);
		allowProg.addAttribute("marker", "PROGRESS");
		AppenderComponentBuilder progress = builder.newAppender("ConsoleProgress", "CONSOLE");
		progress.addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
		progress.addComponent(allowProg);
		progress.add(progLayout);
		builder.add(progress);
		AppenderRefComponentBuilder progRef = builder.newAppenderRef("ConsoleProgress");

		// Set level for specific loggers
		LoggerComponentBuilder lisaLogger = builder.newLogger("it.unive.lisa", Level.DEBUG);
		lisaLogger.addAttribute("additivity", false);
		lisaLogger.add(normalRef);
		if (interactive)
			lisaLogger.add(progRef);

		LoggerComponentBuilder reflLogger = builder.newLogger("org.reflections", Level.ERROR);
		reflLogger.add(normalRef);
		reflLogger.addAttribute("additivity", false);
		LoggerComponentBuilder thymLogger = builder.newLogger("org.thymeleaf", Level.WARN);
		thymLogger.add(normalRef);
		thymLogger.addAttribute("additivity", false);

		RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.INFO);
		rootLogger.add(builder.newAppenderRef("ConsoleNormal"));

		builder.add(rootLogger);
		builder.add(reflLogger);
		builder.add(thymLogger);
		if (interactive)
			builder.add(lisaLogger);

		context.start(builder.build());

		LOG.warn("No Log4j configuration found, using default configuration");
	}

}
