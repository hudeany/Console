package de.soderer.console;

/**
 * Requests the cursor position from a Windows console in its own thread, because the native read blocks forever
 * if the console does not answer (e.g. in an IDE console).
 */
public class WindowsConsoleWidthDetection implements Runnable {
	private final StringBuilder response;

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

			// The caller waits 1 second for this thread, so the answer is waited for slightly shorter
			ConsoleUtilities.readWindowsAnsiPositionResponse(response, 900);
		} catch (@SuppressWarnings("unused") final Throwable t) {
			// Most probably an eclipse console (or, e.g., a native JNA library failure) - catching Throwable
			// (not just Exception) avoids an unhandled Error surfacing via this thread's default
			// UncaughtExceptionHandler. The incomplete response then makes the caller's parsing fail.
		}
	}
}
