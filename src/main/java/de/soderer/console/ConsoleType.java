package de.soderer.console;

/**
 * Type of the console the application runs in, which decides how input is read.
 */
public enum ConsoleType {
	/**
	 * Terminal supporting ANSI escape sequences, single keys can be read.
	 */
	ANSI,

	/**
	 * Terminal without ANSI support, input is read line by line.
	 */
	NO_ANSI,

	/**
	 * Like Eclipse console
	 */
	TEST
}
