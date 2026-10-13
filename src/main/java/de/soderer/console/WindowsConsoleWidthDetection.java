package de.soderer.console;

/**
 * Requests the cursor position from a Windows console in its own thread, because the native read blocks forever
 * if the console does not answer (e.g. in an IDE console).
 */
public class WindowsConsoleWidthDetection implements Runnable {
	private final StringBuilder response;
	private ConsoleType consoleType = null;

	/**
	 * Returns the console type detected by the request.
	 *
	 * @return ANSI if the console answered, TEST if reading failed, null while the request is running
	 */
	public ConsoleType getConsoleType() {
		return consoleType;
	}

	/**
	 * Creates the detection.
	 *
	 * @param response buffer receiving the console's answer
	 */
	public WindowsConsoleWidthDetection(final StringBuilder response) {
		this.response = response;
	}

	@Override
	public void run() {
		try {
			// Request current cursor position
			System.out.print("\033[6n");

			int keyBuffer;
			while ((keyBuffer = WindowsKeyStrokeReader.readKey(false)) > 0) {
				response.append((char) keyBuffer);
			}
			consoleType = ConsoleType.ANSI;
		} catch (@SuppressWarnings("unused") final Throwable t) {
			// Most probably an eclipse console (or, e.g., a native JNA library failure) - catching Throwable
			// (not just Exception) avoids an unhandled Error surfacing via this thread's default
			// UncaughtExceptionHandler, and still falls back to TEST cleanly.
			consoleType = ConsoleType.TEST;
		}
	}
}
