package io.mosip.biosdk.client.config;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.appender.RollingFileAppender;
import io.mosip.kernel.logger.logback.factory.Logfactory;

/**
 * Utility class for configuring a rolling file appender for logging in the
 * BioSDK client application.
 *
 * This class provides a pre-configured rolling file appender that can be used
 * throughout the BioSDK client to centralize logging behavior. It utilizes the
 * Logback logging framework for creating and managing the appender.
 *
 * @since 1.0.0
 */
public final class LoggerConfig {
	/**
	 * Instantiates a new pre-reg logger.
	 */
	private LoggerConfig() {
	}

	/**
	 * The pre-configured rolling file appender for the BioSDK client logs.
	 *
	 * This static variable holds the single instance of the rolling file appender
	 * used throughout the application. It's configured with properties like
	 * appender name, log file location, rolling pattern, and size limits.
	 */
	private static RollingFileAppender mosipRollingFileAppender;

	static {
		mosipRollingFileAppender = new RollingFileAppender();
		mosipRollingFileAppender.setAppend(true);
		mosipRollingFileAppender.setAppenderName("fileappender");
		mosipRollingFileAppender.setFileName("./logs/biosdk-client.log");
		mosipRollingFileAppender.setFileNamePattern("./logs/biosdk-client-%d{yyyy-MM-dd}-%i.log");
		mosipRollingFileAppender.setImmediateFlush(true);
		mosipRollingFileAppender.setMaxFileSize("50mb");
		mosipRollingFileAppender.setPrudent(false);
	}

	/**
	 * Retrieves a logger instance configured with the pre-defined rolling file
	 * appender.
	 *
	 * This method takes a Class object as input and returns a Logger object. The
	 * Logger instance is associated with the provided class, allowing for
	 * class-specific logging behavior within the application.
	 *
	 * @param clazz The Class object representing the class for which the logger is
	 *              requested.
	 * @return A Logger object configured with the pre-defined rolling file
	 *         appender.
	 */
	public static Logger logConfig(Class<?> clazz) {
		return Logfactory.getDefaultRollingFileLogger(mosipRollingFileAppender, clazz);
	}
}