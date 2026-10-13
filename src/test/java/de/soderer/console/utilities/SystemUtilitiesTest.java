package de.soderer.console.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class SystemUtilitiesTest {
	@Test
	public void testGetOsName() {
		assertEquals(System.getProperty("os.name"), SystemUtilities.getOsName());
	}

	@Test
	public void testIsWindowsSystem() {
		final boolean expected = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
		assertEquals(expected, SystemUtilities.isWindowsSystem());
	}
}
