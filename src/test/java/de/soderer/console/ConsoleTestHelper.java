package de.soderer.console;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * Helpers for the console tests: capturing the standard output and access to private members.
 */
public final class ConsoleTestHelper {
	private ConsoleTestHelper() {
		// Utility class with static methods only
	}

	/**
	 * Code which may throw an exception
	 */
	@FunctionalInterface
	public interface ThrowingRunnable {
		void run() throws Exception;
	}

	/**
	 * Runs code with captured System.out and returns the output. Exceptions of the code are passed on.
	 */
	public static String captureSystemOut(final ThrowingRunnable action) throws Exception {
		final PrintStream originalOut = System.out;
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (PrintStream captureStream = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
			System.setOut(captureStream);
			action.run();
		} finally {
			System.setOut(originalOut);
		}
		return buffer.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Runs code with captured System.out, ignoring an exception of the code, and returns the output.
	 */
	public static String captureSystemOutIgnoringException(final ThrowingRunnable action) throws Exception {
		return captureSystemOut(() -> {
			try {
				action.run();
			} catch (@SuppressWarnings("unused") final Exception e) {
				// The output up to the exception is of interest
			}
		});
	}

	/**
	 * Calls a private static method. Exceptions thrown by the method itself are passed through unchanged.
	 */
	public static Object invokeStatic(final Class<?> classType, final String methodName, final Class<?>[] parameterTypes, final Object... arguments) throws Exception {
		final Method method = classType.getDeclaredMethod(methodName, parameterTypes);
		method.setAccessible(true);
		try {
			return method.invoke(null, arguments);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			} else if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			} else {
				throw e;
			}
		}
	}

	/**
	 * Reads a private static field.
	 */
	public static Object getStaticField(final Class<?> classType, final String fieldName) throws Exception {
		final Field field = classType.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(null);
	}

	/**
	 * Calls the private constructor of a utility class.
	 */
	public static void invokePrivateConstructor(final Class<?> classType) throws Exception {
		final Constructor<?> constructor = classType.getDeclaredConstructor();
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	/**
	 * Whether reading input via System.console() fails immediately, because there is no console. Only then the
	 * fallback input paths can be tested without waiting for input forever (since Java 22 System.console() also
	 * returns a console for redirected streams).
	 */
	public static boolean isWithoutConsole() {
		return System.console() == null && ConsoleUtilities.getConsoleType() != ConsoleType.ANSI;
	}
}
