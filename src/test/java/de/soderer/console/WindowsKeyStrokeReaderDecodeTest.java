package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import de.soderer.console.WindowsKeyStrokeReader.ControlKeyState;
import de.soderer.console.WindowsKeyStrokeReader.VirtualKey;
import de.soderer.console.utilities.SystemUtilities;

/**
 * Additional tests of the decoding of Windows console key events. No native calls are needed.
 */
@SuppressWarnings("static-method")
public class WindowsKeyStrokeReaderDecodeTest {
	private static final int NO_MODIFIERS = 0;
	private static final int RIGHT_CTRL = ControlKeyState.RIGHT_CTRL_PRESSED;

	private static int keyDown(final int virtualKeyCode, final int scanCode, final int controlKeyState) {
		return WindowsKeyStrokeReader.decodeKeyEvent(true, virtualKeyCode, scanCode, (char) 0, controlKeyState);
	}

	@Test
	public void testCtrlVariantsOfPageAndEditKeys() {
		assertEquals(0xE000 + 0x84, keyDown(VirtualKey.PRIOR, 0x49, RIGHT_CTRL));
		assertEquals(0xE000 + 0x76, keyDown(VirtualKey.NEXT, 0x51, RIGHT_CTRL));
		assertEquals(0xE000 + 0x92, keyDown(VirtualKey.INSERT, 0x52, RIGHT_CTRL));
		assertEquals(0xE000 + 0x93, keyDown(VirtualKey.DELETE, 0x53, RIGHT_CTRL));
	}

	@Test
	public void testF11AndF12KeepTheCodesOfGetwch() {
		assertEquals(ConsoleUtilities.KeyCode_F11, keyDown(VirtualKey.F11, 0x57, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_F12, keyDown(VirtualKey.F12, 0x58, NO_MODIFIERS));
	}

	@Test
	public void testAllModifierAndLockKeysAreNoKey() {
		final int[] virtualKeys = { VirtualKey.PAUSE, VirtualKey.LWIN, VirtualKey.RWIN, VirtualKey.APPS, VirtualKey.NUMLOCK, VirtualKey.SCROLL,
				VirtualKey.LSHIFT, VirtualKey.RSHIFT, VirtualKey.LCONTROL, VirtualKey.RCONTROL, VirtualKey.LMENU, VirtualKey.RMENU };
		for (final int virtualKey : virtualKeys) {
			assertEquals(WindowsKeyStrokeReader.NO_KEY, keyDown(virtualKey, 0x10, NO_MODIFIERS), "Virtual key " + virtualKey);
		}
	}

	@Test
	public void testDeadKeysAndKeysWithoutScanCodeAreNoKey() {
		assertEquals(WindowsKeyStrokeReader.NO_KEY, keyDown(VirtualKey.OEM_FIRST, 0x1B, NO_MODIFIERS));
		assertEquals(WindowsKeyStrokeReader.NO_KEY, keyDown(VirtualKey.OEM_LAST, 0x56, NO_MODIFIERS));
		assertEquals(WindowsKeyStrokeReader.NO_KEY, keyDown(0x70, 0, NO_MODIFIERS));
		// Just outside of the OEM range: a special key with its scan code
		assertEquals(0xE000 + 0x22, keyDown(VirtualKey.OEM_LAST + 1, 0x22, NO_MODIFIERS));
	}

	@Test
	public void testTabWithoutShiftAndCharacterWithShift() {
		assertEquals(ConsoleUtilities.KeyCode_Tab, WindowsKeyStrokeReader.decodeKeyEvent(true, 0x09, 0x0F, '\t', NO_MODIFIERS));
		assertEquals('A', WindowsKeyStrokeReader.decodeKeyEvent(true, 0x41, 0x1E, 'A', ControlKeyState.SHIFT_PRESSED));
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		ConsoleTestHelper.invokePrivateConstructor(WindowsKeyStrokeReader.class);
	}

	@Test
	public void testNativeFunctionsAreNotAvailableOutsideOfWindows() throws Exception {
		assumeTrue(!SystemUtilities.isWindowsSystem(), "The native Windows functions are only missing on other systems");
		// Loading the Windows libraries fails, which must not end in an unexpected exception type
		try {
			WindowsKeyStrokeReader.isConsoleAttached();
		} catch (@SuppressWarnings("unused") final Throwable expected) {
			// UnsatisfiedLinkError or similar is expected here
		}
		// The terminal size detection thread catches all errors of the native functions
		final StringBuilder response = new StringBuilder();
		final String output = ConsoleTestHelper.captureSystemOut(() -> new WindowsConsoleWidthDetection(response).run());
		assertEquals("\033[6n", output);
		assertEquals("", response.toString());
	}
}
