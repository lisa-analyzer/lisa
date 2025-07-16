package it.unive.lisa.logging;

import it.unive.lisa.LiSA;
import it.unive.lisa.conf.LiSAConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
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

		AppenderComponentBuilder appenderBuilder = builder
				.newAppender("Console", "CONSOLE")
				.addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
		appenderBuilder.add(builder.newLayout("PatternLayout").addAttribute("pattern", "%d [%5level] %m %ex%n"));
		builder.add(appenderBuilder);
		builder.add(builder.newRootLogger(Level.DEBUG).add(builder.newAppenderRef("Console")));

		// Set level for specific loggers
		builder
				.add(
						builder
								.newLogger("it.unive.lisa", Level.DEBUG)
								.add(builder.newAppenderRef("Console"))
								.addAttribute("additivity", false));
		builder
				.add(
						builder
								.newLogger("org.reflections", Level.ERROR)
								.add(builder.newAppenderRef("Console"))
								.addAttribute("additivity", false));
		builder
				.add(
						builder
								.newLogger("org.thymeleaf", Level.WARN)
								.add(builder.newAppenderRef("Console"))
								.addAttribute("additivity", false));

		context.start(builder.build());

		LOG.warn("No Log4j configuration found, using default configuration");
	}

}
