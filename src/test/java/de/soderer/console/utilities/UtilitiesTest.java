package de.soderer.console.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class UtilitiesTest {
	private static final String TEST_HOME = File.separator + "home" + File.separator + "testuser";

	private String originalUserHome;

	@BeforeEach
	public void setUp() {
		originalUserHome = System.getProperty("user.home");
		System.setProperty("user.home", TEST_HOME);
	}

	@AfterEach
	public void tearDown() {
		System.setProperty("user.home", originalUserHome);
	}

	@Test
	public void testIsBlankString() {
		assertTrue(Utilities.isBlank((String) null));
		assertTrue(Utilities.isBlank(""));
		assertTrue(Utilities.isBlank("   "));
		assertTrue(Utilities.isBlank(" \t\r\n "));
		assertFalse(Utilities.isBlank("a"));
		assertFalse(Utilities.isBlank("  a  "));
	}

	@Test
	public void testIsNotBlankString() {
		assertFalse(Utilities.isNotBlank(null));
		assertFalse(Utilities.isNotBlank(" \t"));
		assertTrue(Utilities.isNotBlank(" x "));
	}

	@Test
	public void testIsBlankCharArray() {
		assertTrue(Utilities.isBlank((char[]) null));
		assertTrue(Utilities.isBlank(new char[0]));
		assertTrue(Utilities.isBlank(" \t\n".toCharArray()));
		assertFalse(Utilities.isBlank(" secret ".toCharArray()));
	}

	@Test
	public void testClearCharArray() {
		final char[] password = "secret".toCharArray();
		Utilities.clear(password);
		assertArrayEquals(new char[6], password);
	}

	@Test
	public void testClearCharArrayNull() {
		// Must not throw
		Utilities.clear((char[]) null);
	}

	@Test
	public void testRepeatChar() {
		assertEquals("*****", Utilities.repeat('*', 5));
		assertEquals("", Utilities.repeat('*', 0));
		assertEquals("", Utilities.repeat('*', -3));
	}

	@Test
	public void testRepeatString() {
		assertEquals("ababab", Utilities.repeat("ab", 3));
		assertEquals("", Utilities.repeat("ab", 0));
		assertEquals("", Utilities.repeat("", 5));
		assertNull(Utilities.repeat(null, 5));
	}

	@Test
	public void testRepeatStringWithSeparator() {
		assertEquals("a, a, a", Utilities.repeat("a", 3, ", "));
		assertEquals("a", Utilities.repeat("a", 1, ", "));
		assertEquals("aaa", Utilities.repeat("a", 3, null));
	}

	@Test
	public void testLeftPad() {
		assertEquals(" 5%", Utilities.leftPad("5%", 3));
		assertEquals("100%", Utilities.leftPad("100%", 3));
		assertEquals(" 7", Utilities.leftPad("7", 2));
		// Invalid format width is ignored
		assertEquals("abc", Utilities.leftPad("abc", 0));
	}

	@Test
	public void testReplaceUsersHome() {
		assertNull(Utilities.replaceUsersHome(null));
		assertEquals(TEST_HOME, Utilities.replaceUsersHome("~"));
		assertEquals(TEST_HOME + File.separator + "data", Utilities.replaceUsersHome("~" + File.separator + "data"));
		assertEquals(TEST_HOME + "/data", Utilities.replaceUsersHome("~/data"));
		assertEquals(TEST_HOME + "/data", Utilities.replaceUsersHome("$HOME/data"));
		assertEquals(TEST_HOME + "/data", Utilities.replaceUsersHome("${HOME}/data"));
	}

	@Test
	public void testReplaceUsersHomeKeepsTildeInsidePath() {
		// "~" is only replaced at the start of a path
		assertEquals("/tmp/backup~", Utilities.replaceUsersHome("/tmp/backup~"));
		assertEquals("~other", Utilities.replaceUsersHome("~other"));
	}

	@Test
	public void testReplaceUsersHomeByTilde() {
		assertNull(Utilities.replaceUsersHomeByTilde(null));
		assertEquals("~", Utilities.replaceUsersHomeByTilde(TEST_HOME));
		assertEquals("~" + File.separator + "data", Utilities.replaceUsersHomeByTilde(TEST_HOME + File.separator + "data"));
		assertEquals(File.separator + "tmp", Utilities.replaceUsersHomeByTilde(File.separator + "tmp"));
	}

	@Test
	public void testReplaceUsersHomeByTildeRequiresCompletePathElement() {
		// A sibling directory with the home path as prefix must not be shortened to "~2"
		assertEquals(TEST_HOME + "2", Utilities.replaceUsersHomeByTilde(TEST_HOME + "2"));
	}

	@Test
	public void testReplaceUsersHomeRoundTrip() {
		final String path = "~" + File.separator + "projects" + File.separator + "console";
		assertEquals(path, Utilities.replaceUsersHomeByTilde(Utilities.replaceUsersHome(path)));
	}

	@Test
	public void testConvertByteArrayToInt() throws Exception {
		assertEquals(0, Utilities.convertByteArrayToInt(new byte[0]));
		assertEquals(1, Utilities.convertByteArrayToInt(new byte[] { 1 }));
		assertEquals(255, Utilities.convertByteArrayToInt(new byte[] { (byte) 0xFF }));
		assertEquals(0x1234, Utilities.convertByteArrayToInt(new byte[] { 0x12, 0x34 }));
		assertEquals(0x123456, Utilities.convertByteArrayToInt(new byte[] { 0x12, 0x34, 0x56 }));
		assertEquals(0x12345678, Utilities.convertByteArrayToInt(new byte[] { 0x12, 0x34, 0x56, 0x78 }));
		assertEquals(-1, Utilities.convertByteArrayToInt(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));
	}

	@Test
	public void testConvertByteArrayToIntTooLong() {
		assertThrows(Exception.class, () -> Utilities.convertByteArrayToInt(new byte[5]));
	}

	@Test
	public void testHumanReadableNumberBelowUnit() {
		assertEquals("0", Utilities.getHumanReadableNumber(0L, "", true, 5, true, Locale.ENGLISH));
		assertEquals("0 B", Utilities.getHumanReadableNumber(0L, "B", true, 5, true, Locale.ENGLISH));
		assertEquals("999 B", Utilities.getHumanReadableNumber(999L, "B", true, 5, true, Locale.ENGLISH));
		assertEquals("1023 B", Utilities.getHumanReadableNumber(1023, "B", false, 5, true, Locale.ENGLISH));
	}

	@Test
	public void testHumanReadableNumberSiUnits() {
		assertEquals("1.0000 kB", Utilities.getHumanReadableNumber(1000L, "B", true, 5, true, Locale.ENGLISH));
		assertEquals("1.0 kB", Utilities.getHumanReadableNumber(1000L, "B", true, 5, false, Locale.ENGLISH));
		assertEquals("1.0000 M", Utilities.getHumanReadableNumber(1_000_000L, "", true, 5, true, Locale.ENGLISH));
	}

	@Test
	public void testHumanReadableNumberBinaryUnits() {
		assertEquals("1.0000 KiB", Utilities.getHumanReadableNumber(1024L, "B", false, 5, true, Locale.ENGLISH));
		assertEquals("1.5 KiB", Utilities.getHumanReadableNumber(1536L, "B", false, 5, false, Locale.ENGLISH));
	}

	@Test
	public void testHumanReadableNumberRoundingCarriesIntoNextUnit() {
		// 999.999 kB rounds to 1000.0 kB and must be shown as 1 MB instead
		assertEquals("1.0000 MB", Utilities.getHumanReadableNumber(999_999L, "B", true, 5, true, Locale.ENGLISH));
		assertEquals("1.0 MB", Utilities.getHumanReadableNumber(999_960L, "B", true, 3, false, Locale.ENGLISH));
	}

	@Test
	public void testHumanReadableNumberLargestUnit() {
		assertEquals("9.2234 EB", Utilities.getHumanReadableNumber(Long.MAX_VALUE, "B", true, 5, true, Locale.ENGLISH));
	}

	@Test
	public void testHumanReadableNumberDecimalValue() {
		assertEquals("12.500", Utilities.getHumanReadableNumber(12.5, "", true, 5, true, Locale.ENGLISH));
		assertEquals("12.5", Utilities.getHumanReadableNumber(12.5, "", true, 5, false, Locale.ENGLISH));
	}

	@Test
	public void testHumanReadableNumberLocale() {
		assertEquals("1,5000 KiB", Utilities.getHumanReadableNumber(1536L, "B", false, 5, true, Locale.GERMAN));
	}
}
