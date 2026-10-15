package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.soderer.console.WindowsKeyStrokeReader.ControlKeyState;
import de.soderer.console.WindowsKeyStrokeReader.VirtualKey;

/**
 * Tests the decoding of Windows console key events, which needs no native calls and so runs on all systems.
 */
@SuppressWarnings("static-method")
public class WindowsKeyStrokeReaderTest {
	private static final int NO_MODIFIERS = 0;
	private static final int LEFT_CTRL = ControlKeyState.LEFT_CTRL_PRESSED;
	private static final int SHIFT = ControlKeyState.SHIFT_PRESSED;
	private static final int ALT_GR = ControlKeyState.RIGHT_ALT_PRESSED | ControlKeyState.LEFT_CTRL_PRESSED;

	private static int keyDown(final int virtualKeyCode, final int scanCode, final char unicodeChar, final int controlKeyState) {
		return WindowsKeyStrokeReader.decodeKeyEvent(true, virtualKeyCode, scanCode, unicodeChar, controlKeyState);
	}

	@Test
	public void testCharacterAGraveIsNotMistakenForSpecialKeyPrefix() {
		// 'à' (0xE0) was swallowed by _getwch(), because 0xE0 is also its prefix for special keys
		assertEquals('à', keyDown(0x41, 0x1E, 'à', NO_MODIFIERS));
	}

	@Test
	public void testPlainAndAltGrCharacters() {
		assertEquals('a', keyDown(0x41, 0x1E, 'a', NO_MODIFIERS));
		assertEquals('@', keyDown(0x51, 0x10, '@', ALT_GR));
		assertEquals('€', keyDown(0x45, 0x12, '€', ALT_GR));
	}

	@Test
	public void testControlCharacters() {
		assertEquals(ConsoleUtilities.KeyCode_CtrlC, keyDown(0x43, 0x2E, (char) 3, LEFT_CTRL));
		assertEquals(ConsoleUtilities.KeyCode_CtrlV, keyDown(0x56, 0x2F, (char) 22, LEFT_CTRL));
		assertEquals(ConsoleUtilities.KeyCode_Enter, keyDown(0x0D, 0x1C, '\r', NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_Backspace, keyDown(0x08, 0x0E, '\b', NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_Escape, keyDown(0x1B, 0x01, (char) 27, NO_MODIFIERS));
	}

	@Test
	public void testTabAndReverseTab() {
		assertEquals(ConsoleUtilities.KeyCode_Tab, keyDown(0x09, 0x0F, '\t', NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_ReverseTab, keyDown(0x09, 0x0F, '\t', SHIFT));
	}

	@Test
	public void testSpecialKeysKeepTheKeyCodesOfGetwch() {
		assertEquals(ConsoleUtilities.KeyCode_Home, keyDown(VirtualKey.HOME, 0x47, (char) 0, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_End, keyDown(VirtualKey.END, 0x4F, (char) 0, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_PageUp, keyDown(VirtualKey.PRIOR, 0x49, (char) 0, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_PageDown, keyDown(VirtualKey.NEXT, 0x51, (char) 0, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_ArrowUp, keyDown(VirtualKey.UP, 0x48, (char) 0, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_ArrowDown, keyDown(VirtualKey.DOWN, 0x50, (char) 0, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_ArrowLeft, keyDown(VirtualKey.LEFT, 0x4B, (char) 0, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_ArrowRight, keyDown(VirtualKey.RIGHT, 0x4D, (char) 0, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_Insert, keyDown(VirtualKey.INSERT, 0x52, (char) 0, NO_MODIFIERS));
		assertEquals(ConsoleUtilities.KeyCode_Delete, keyDown(VirtualKey.DELETE, 0x53, (char) 0, NO_MODIFIERS));
		// Shift does not change navigation keys
		assertEquals(ConsoleUtilities.KeyCode_ArrowUp, keyDown(VirtualKey.UP, 0x48, (char) 0, SHIFT));
	}

	@Test
	public void testCtrlSpecialKeys() {
		assertEquals(ConsoleUtilities.KeyCode_CtrlHome, keyDown(VirtualKey.HOME, 0x47, (char) 0, LEFT_CTRL));
		assertEquals(ConsoleUtilities.KeyCode_CtrlEnd, keyDown(VirtualKey.END, 0x4F, (char) 0, LEFT_CTRL));
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowUp, keyDown(VirtualKey.UP, 0x48, (char) 0, LEFT_CTRL));
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowDown, keyDown(VirtualKey.DOWN, 0x50, (char) 0, LEFT_CTRL));
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowLeft, keyDown(VirtualKey.LEFT, 0x4B, (char) 0, LEFT_CTRL));
		assertEquals(ConsoleUtilities.KeyCode_CtrlArrowRight, keyDown(VirtualKey.RIGHT, 0x4D, (char) 0, LEFT_CTRL));
	}

	@Test
	public void testFunctionKeysUseScanCodeLikeGetwch() {
		// F1 has scan code 0x3B
		assertEquals(0xE000 + 0x3B, keyDown(0x70, 0x3B, (char) 0, NO_MODIFIERS));
	}

	@Test
	public void testEventsWithoutKeyForTheApplication() {
		// Key release
		assertEquals(WindowsKeyStrokeReader.NO_KEY, WindowsKeyStrokeReader.decodeKeyEvent(false, 0x41, 0x1E, 'a', NO_MODIFIERS));
		// Modifier keys alone
		assertEquals(WindowsKeyStrokeReader.NO_KEY, keyDown(VirtualKey.SHIFT, 0x2A, (char) 0, SHIFT));
		assertEquals(WindowsKeyStrokeReader.NO_KEY, keyDown(VirtualKey.CONTROL, 0x1D, (char) 0, LEFT_CTRL));
		assertEquals(WindowsKeyStrokeReader.NO_KEY, keyDown(VirtualKey.MENU, 0x38, (char) 0, ControlKeyState.LEFT_ALT_PRESSED));
		assertEquals(WindowsKeyStrokeReader.NO_KEY, keyDown(VirtualKey.CAPITAL, 0x3A, (char) 0, NO_MODIFIERS));
		// Dead key '`' (VK_OEM_6 on a German keyboard) before 'a' to compose 'à'
		assertEquals(WindowsKeyStrokeReader.NO_KEY, keyDown(0xDD, 0x0D, (char) 0, SHIFT));
	}
}
