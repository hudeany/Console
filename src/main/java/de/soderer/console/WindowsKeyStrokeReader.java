package de.soderer.console;

import java.io.IOException;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Reads single keys from a Windows console without echo via the native functions of msvcrt and kernel32 (JNA).
 * Ctrl+C is read as key instead of ending the application. The original console mode is restored on exit.
 */
public class WindowsKeyStrokeReader {
	private WindowsKeyStrokeReader() {
		// Only static methods
	}

	private static volatile boolean initDone;
	private static volatile boolean stdinIsConsole;
	private static volatile boolean consoleModeAltered;

	private static volatile Msvcrt msvcrt;
	private static volatile Kernel32 kernel32;
	private static volatile Pointer consoleHandle;
	private static volatile int originalConsoleMode;

	/**
	 * Read a character from the console without echo
	 *
	 * @param wait true to wait for the next key, false to return immediately if no key is available
	 * @return key code (special keys as 0xE000 + scan code), -2 if no key is available and wait is false, -1 at the end of redirected input
	 * @throws Exception if the native functions cannot be used
	 */
	public static int readKey(final boolean wait) throws Exception {
		init();
		if (stdinIsConsole) {
			consoleModeAltered = true;
			setConsoleMode(consoleHandle, originalConsoleMode & ~Kernel32Definitions.ENABLE_PROCESSED_INPUT);
			if (!wait && msvcrt._kbhit() == 0) {
				return -2;
			}
			return getwch();
		} else {
			int c = msvcrt.getwchar();
			if (c == 0xFFFF) {
				c = -1;
			}
			return c;
		}
	}

	private static synchronized void init() {
		if (initDone) {
			return;
		} else {
			msvcrt = Native.load("msvcrt", Msvcrt.class);
			kernel32 = Native.load("kernel32", Kernel32.class);

			try {
				consoleHandle = getStdInputHandle();
				originalConsoleMode = getConsoleMode(consoleHandle);
				stdinIsConsole = true;
			} catch (@SuppressWarnings("unused") final IOException e) {
				stdinIsConsole = false;
			}

			if (stdinIsConsole) {
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						try {
							if (initDone && stdinIsConsole && consoleModeAltered) {
								setConsoleMode(consoleHandle, originalConsoleMode);
								consoleModeAltered = false;
							}
						} catch (@SuppressWarnings("unused") final Exception e) {
							// do nothing
						}
					}
				});
			}

			initDone = true;
		}
	}

	private static int getwch() {
		int c = msvcrt._getwch();
		if (c == 0 || c == 0xE0) {
			c = msvcrt._getwch();
			if (c >= 0 && c <= 0x18FF) {
				return 0xE000 + c;
			} else {
				return 0xFFFE;
			}
		} else if (c < 0 || c > 0xFFFF) {
			return 0xFFFE;
		} else {
			return c;
		}
	}

	private static Pointer getStdInputHandle() throws IOException {
		final Pointer handle = kernel32.GetStdHandle(Kernel32Definitions.STD_INPUT_HANDLE);
		if (Pointer.nativeValue(handle) == 0 || Pointer.nativeValue(handle) == Kernel32Definitions.INVALID_HANDLE_VALUE) {
			throw new IOException("GetStdHandle(STD_INPUT_HANDLE) failed");
		} else {
			return handle;
		}
	}

	private static int getConsoleMode(final Pointer handle) throws IOException {
		final IntByReference mode = new IntByReference();
		if (kernel32.GetConsoleMode(handle, mode) == 0) {
			throw new IOException("GetConsoleMode failed");
		} else {
			return mode.getValue();
		}
	}

	private static void setConsoleMode(final Pointer handle, final int mode) throws IOException {
		if (kernel32.SetConsoleMode(handle, mode) == 0) {
			throw new IOException("SetConsoleMode failed");
		}
	}

	private interface Msvcrt extends Library {
		int _kbhit();
		int _getwch();
		int getwchar();
	}

	private static class Kernel32Definitions {
		static final int STD_INPUT_HANDLE = -10;
		static final long INVALID_HANDLE_VALUE = (Native.POINTER_SIZE == 8) ? -1 : 0xFFFFFFFFL;
		static final int ENABLE_PROCESSED_INPUT = 0x0001;
	}

	private interface Kernel32 extends Library {
		int GetConsoleMode(Pointer hConsoleHandle, IntByReference lpMode);
		int SetConsoleMode(Pointer hConsoleHandle, int dwMode);
		Pointer GetStdHandle(int nStdHandle);
	}
}
