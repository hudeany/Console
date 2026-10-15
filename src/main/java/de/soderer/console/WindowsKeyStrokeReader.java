package de.soderer.console;

import java.io.IOException;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Reads single keys from a Windows console without echo via the native console input functions of kernel32 (JNA).
 * Ctrl+C is read as key instead of ending the application. The original console mode is restored after each read
 * and on exit.
 * <p>
 * Keys are read as console input records (ReadConsoleInputW) instead of via _getwch(), because _getwch() returns
 * the same value 0xE0 for the character 'à' and for the prefix of special keys, so 'à' could not be entered.
 * Special keys keep the key codes known from _getwch(): 0xE000 + scan code.
 */
public class WindowsKeyStrokeReader {
	private WindowsKeyStrokeReader() {
		// Only static methods
	}

	/**
	 * Returned by {@link #decodeKeyEvent(boolean, int, int, char, int)} for input events that do not represent a key
	 * for the application (key releases, modifier keys alone, dead keys).
	 */
	static final int NO_KEY = Integer.MIN_VALUE;

	private static final Object READ_LOCK = new Object();

	private static volatile boolean initDone;
	private static volatile boolean stdinIsConsole;
	private static volatile boolean consoleModeAltered;

	private static volatile Msvcrt msvcrt;
	private static volatile Kernel32 kernel32;
	private static volatile Pointer consoleHandle;
	private static volatile int originalConsoleMode;

	/** Key code of a held key, which the console reported once with a repeat count above 1 */
	private static int pendingRepeatedKeyCode = NO_KEY;
	/** Number of repetitions of {@link #pendingRepeatedKeyCode} not returned yet */
	private static int pendingRepeatCount = 0;
	/** Native buffer for one INPUT_RECORD, only used under READ_LOCK */
	private static Memory inputRecordBuffer = null;

	/**
	 * Read a character from the console without echo
	 *
	 * @param wait true to wait for the next key, false to return immediately if no key is available
	 * @return key code (special keys as 0xE000 + scan code), -2 if no key is available and wait is false, -1 ({@link ConsoleUtilities#KeyCode_EndOfInput}) at the end of redirected input
	 * @throws Exception if the native functions cannot be used
	 */
	public static int readKey(final boolean wait) throws Exception {
		init();
		if (stdinIsConsole) {
			synchronized (READ_LOCK) {
				if (pendingRepeatCount > 0) {
					pendingRepeatCount--;
					return pendingRepeatedKeyCode;
				}

				consoleModeAltered = true;
				setConsoleMode(consoleHandle, originalConsoleMode & ~Kernel32Definitions.ENABLE_PROCESSED_INPUT);
				try {
					return readKeyFromConsoleInput(wait);
				} finally {
					// Restore the original mode right after reading, so Ctrl+C ends the application again outside of key reading
					try {
						setConsoleMode(consoleHandle, originalConsoleMode);
						consoleModeAltered = false;
					} catch (@SuppressWarnings("unused") final IOException e) {
						// Mode stays marked as altered, so the shutdown hook retries the reset
					}
				}
			}
		} else {
			int c = msvcrt.getwchar();
			if (c == 0xFFFF) {
				c = ConsoleUtilities.KeyCode_EndOfInput;
			}
			return c;
		}
	}

	/**
	 * Reads console input records until one of them is a key for the application. Other records (key releases,
	 * modifier keys, mouse, focus and window size events) are skipped.
	 */
	private static int readKeyFromConsoleInput(final boolean wait) throws IOException {
		// Only called under READ_LOCK, so one native buffer can be reused (Memory of JNA 5.6 is not Closeable)
		if (inputRecordBuffer == null) {
			inputRecordBuffer = new Memory(InputRecordLayout.SIZE);
		}
		final Memory inputRecord = inputRecordBuffer;
		final IntByReference numberOfEvents = new IntByReference();
		while (true) {
			if (!wait) {
				if (kernel32.GetNumberOfConsoleInputEvents(consoleHandle, numberOfEvents) == 0) {
					throw new IOException("GetNumberOfConsoleInputEvents failed");
				} else if (numberOfEvents.getValue() == 0) {
					return -2;
				}
			}

			// Blocks until at least one input record is available
			if (kernel32.ReadConsoleInputW(consoleHandle, inputRecord, 1, numberOfEvents) == 0) {
				throw new IOException("ReadConsoleInputW failed");
			} else if (numberOfEvents.getValue() < 1 || inputRecord.getShort(InputRecordLayout.EVENT_TYPE) != InputRecordLayout.KEY_EVENT) {
				continue;
			}

			final int keyCode = decodeKeyEvent(
					inputRecord.getInt(InputRecordLayout.KEY_DOWN) != 0,
					inputRecord.getShort(InputRecordLayout.VIRTUAL_KEY_CODE) & 0xFFFF,
					inputRecord.getShort(InputRecordLayout.VIRTUAL_SCAN_CODE) & 0xFFFF,
					(char) inputRecord.getShort(InputRecordLayout.UNICODE_CHAR),
					inputRecord.getInt(InputRecordLayout.CONTROL_KEY_STATE));
			if (keyCode != NO_KEY) {
				final int repeatCount = inputRecord.getShort(InputRecordLayout.REPEAT_COUNT) & 0xFFFF;
				if (repeatCount > 1) {
					pendingRepeatedKeyCode = keyCode;
					pendingRepeatCount = repeatCount - 1;
				}
				return keyCode;
			}
		}
	}

	/**
	 * Converts a console key event into a key code. Package-private and free of native calls, so it can be tested
	 * on all systems.
	 *
	 * @param keyDown true for a key press, false for a key release
	 * @param virtualKeyCode Windows virtual key code (VK_*)
	 * @param virtualScanCode hardware scan code
	 * @param unicodeChar translated character, 0 if the key has no character
	 * @param controlKeyState state of the modifier keys (*_PRESSED flags)
	 * @return the character for keys with a character, Ctrl+letter as 1 to 26, special keys as 0xE000 + scan code
	 *         (the values of _getwch()), {@link #NO_KEY} for events without a key for the application
	 */
	static int decodeKeyEvent(final boolean keyDown, final int virtualKeyCode, final int virtualScanCode, final char unicodeChar, final int controlKeyState) {
		if (!keyDown) {
			return NO_KEY;
		}

		final boolean shiftPressed = (controlKeyState & ControlKeyState.SHIFT_PRESSED) != 0;
		final boolean ctrlPressed = (controlKeyState & (ControlKeyState.LEFT_CTRL_PRESSED | ControlKeyState.RIGHT_CTRL_PRESSED)) != 0;

		if (unicodeChar != 0) {
			if (unicodeChar == '\t' && shiftPressed) {
				return ConsoleUtilities.KeyCode_ReverseTab;
			} else {
				// Also AltGr characters like '@' (reported with Ctrl+Alt) and characters composed via dead keys like 'à'
				return unicodeChar;
			}
		}

		switch (virtualKeyCode) {
			case VirtualKey.HOME:
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlHome : ConsoleUtilities.KeyCode_Home;
			case VirtualKey.END:
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlEnd : ConsoleUtilities.KeyCode_End;
			case VirtualKey.PRIOR:
				return ctrlPressed ? 0xE000 + 0x84 : ConsoleUtilities.KeyCode_PageUp;
			case VirtualKey.NEXT:
				return ctrlPressed ? 0xE000 + 0x76 : ConsoleUtilities.KeyCode_PageDown;
			case VirtualKey.LEFT:
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlArrowLeft : ConsoleUtilities.KeyCode_ArrowLeft;
			case VirtualKey.RIGHT:
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlArrowRight : ConsoleUtilities.KeyCode_ArrowRight;
			case VirtualKey.UP:
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlArrowUp : ConsoleUtilities.KeyCode_ArrowUp;
			case VirtualKey.DOWN:
				return ctrlPressed ? ConsoleUtilities.KeyCode_CtrlArrowDown : ConsoleUtilities.KeyCode_ArrowDown;
			case VirtualKey.INSERT:
				return ctrlPressed ? 0xE000 + 0x92 : ConsoleUtilities.KeyCode_Insert;
			case VirtualKey.DELETE:
				return ctrlPressed ? 0xE000 + 0x93 : ConsoleUtilities.KeyCode_Delete;
			case VirtualKey.F11:
				// The hardware scan code of F11 is 0x57, but _getwch() reported 0x85
				return ConsoleUtilities.KeyCode_F11;
			case VirtualKey.F12:
				// The hardware scan code of F12 is 0x58, but _getwch() reported 0x86
				return ConsoleUtilities.KeyCode_F12;
			case VirtualKey.SHIFT:
			case VirtualKey.CONTROL:
			case VirtualKey.MENU:
			case VirtualKey.PAUSE:
			case VirtualKey.CAPITAL:
			case VirtualKey.LWIN:
			case VirtualKey.RWIN:
			case VirtualKey.APPS:
			case VirtualKey.NUMLOCK:
			case VirtualKey.SCROLL:
			case VirtualKey.LSHIFT:
			case VirtualKey.RSHIFT:
			case VirtualKey.LCONTROL:
			case VirtualKey.RCONTROL:
			case VirtualKey.LMENU:
			case VirtualKey.RMENU:
				// Modifier and lock keys alone are no key for the application (_getwch() did not report them either)
				return NO_KEY;
			default:
				if ((virtualKeyCode >= VirtualKey.OEM_FIRST && virtualKeyCode <= VirtualKey.OEM_LAST) || virtualScanCode == 0) {
					// Dead keys (e.g. '`' or '^' on a German keyboard) report no character; the composed character
					// (e.g. 'à') follows with the next key
					return NO_KEY;
				} else {
					// Other special keys like F1 to F12: same value as _getwch() returned
					return 0xE000 + virtualScanCode;
				}
		}
	}

	/**
	 * Checks whether stdin and stdout are both attached to a real Windows console (not redirected to a file or pipe,
	 * not an IDE console).
	 *
	 * @return true if stdin and stdout are a console
	 */
	public static boolean isConsoleAttached() {
		init();
		if (!stdinIsConsole) {
			return false;
		}
		final Pointer stdOutputHandle = kernel32.GetStdHandle(Kernel32Definitions.STD_OUTPUT_HANDLE);
		if (stdOutputHandle == null || Pointer.nativeValue(stdOutputHandle) == 0 || Pointer.nativeValue(stdOutputHandle) == Kernel32Definitions.INVALID_HANDLE_VALUE) {
			return false;
		}
		return kernel32.GetConsoleMode(stdOutputHandle, new IntByReference()) != 0;
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

	private static Pointer getStdInputHandle() throws IOException {
		final Pointer handle = kernel32.GetStdHandle(Kernel32Definitions.STD_INPUT_HANDLE);
		if (handle == null || Pointer.nativeValue(handle) == 0 || Pointer.nativeValue(handle) == Kernel32Definitions.INVALID_HANDLE_VALUE) {
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
		int getwchar();
	}

	private static class Kernel32Definitions {
		static final int STD_INPUT_HANDLE = -10;
		static final int STD_OUTPUT_HANDLE = -11;
		static final long INVALID_HANDLE_VALUE = (Native.POINTER_SIZE == 8) ? -1 : 0xFFFFFFFFL;
		static final int ENABLE_PROCESSED_INPUT = 0x0001;
	}

	/**
	 * Byte layout of the native INPUT_RECORD structure with a KEY_EVENT_RECORD (identical for 32 and 64 bit).
	 * Read directly from memory instead of a JNA Structure mapping of the union.
	 */
	private static class InputRecordLayout {
		static final int SIZE = 20;
		static final short KEY_EVENT = 0x0001;

		/** WORD EventType, followed by 2 bytes of padding */
		static final long EVENT_TYPE = 0;
		/** BOOL bKeyDown */
		static final long KEY_DOWN = 4;
		/** WORD wRepeatCount */
		static final long REPEAT_COUNT = 8;
		/** WORD wVirtualKeyCode */
		static final long VIRTUAL_KEY_CODE = 10;
		/** WORD wVirtualScanCode */
		static final long VIRTUAL_SCAN_CODE = 12;
		/** WCHAR UnicodeChar */
		static final long UNICODE_CHAR = 14;
		/** DWORD dwControlKeyState */
		static final long CONTROL_KEY_STATE = 16;
	}

	/**
	 * Flags of KEY_EVENT_RECORD.dwControlKeyState.
	 */
	static class ControlKeyState {
		static final int RIGHT_ALT_PRESSED = 0x0001;
		static final int LEFT_ALT_PRESSED = 0x0002;
		static final int RIGHT_CTRL_PRESSED = 0x0004;
		static final int LEFT_CTRL_PRESSED = 0x0008;
		static final int SHIFT_PRESSED = 0x0010;
	}

	/**
	 * Windows virtual key codes (VK_*) used for decoding.
	 */
	static class VirtualKey {
		static final int SHIFT = 0x10;
		static final int CONTROL = 0x11;
		static final int MENU = 0x12;
		static final int PAUSE = 0x13;
		static final int CAPITAL = 0x14;
		static final int PRIOR = 0x21;
		static final int NEXT = 0x22;
		static final int END = 0x23;
		static final int HOME = 0x24;
		static final int LEFT = 0x25;
		static final int UP = 0x26;
		static final int RIGHT = 0x27;
		static final int DOWN = 0x28;
		static final int INSERT = 0x2D;
		static final int DELETE = 0x2E;
		static final int LWIN = 0x5B;
		static final int RWIN = 0x5C;
		static final int APPS = 0x5D;
		static final int F11 = 0x7A;
		static final int F12 = 0x7B;
		static final int NUMLOCK = 0x90;
		static final int SCROLL = 0x91;
		static final int LSHIFT = 0xA0;
		static final int RSHIFT = 0xA1;
		static final int LCONTROL = 0xA2;
		static final int RCONTROL = 0xA3;
		static final int LMENU = 0xA4;
		static final int RMENU = 0xA5;
		/** First of the keyboard layout specific keys VK_OEM_1 to VK_OEM_102, which are the dead keys on many layouts */
		static final int OEM_FIRST = 0xBA;
		/** Last of the keyboard layout specific keys VK_OEM_1 to VK_OEM_102 */
		static final int OEM_LAST = 0xE2;
	}

	/**
	 * Kernel32 functions. StdCallLibrary for the correct calling convention on 32 bit Windows.
	 */
	private interface Kernel32 extends StdCallLibrary {
		int GetConsoleMode(Pointer hConsoleHandle, IntByReference lpMode);
		int SetConsoleMode(Pointer hConsoleHandle, int dwMode);
		Pointer GetStdHandle(int nStdHandle);
		int ReadConsoleInputW(Pointer hConsoleInput, Pointer lpBuffer, int nLength, IntByReference lpNumberOfEventsRead);
		int GetNumberOfConsoleInputEvents(Pointer hConsoleInput, IntByReference lpNumberOfEvents);
	}
}
