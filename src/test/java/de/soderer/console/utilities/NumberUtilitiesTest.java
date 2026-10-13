package de.soderer.console.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class NumberUtilitiesTest {
	@Test
	public void testValidNumbers() {
		for (final String value : new String[] { "0", "42", "-12", "+3", "3.5", "-3.5", "1.", ".5", "-.5", "1e10", "1E+10", "-12.5E-3" }) {
			assertTrue(NumberUtilities.isNumber(value), "Expected number: " + value);
		}
	}

	@Test
	public void testInvalidNumbers() {
		for (final String value : new String[] { null, "", " ", "abc", "1,5", "1.000.000", "1.2.3", "e5", ".", "-", "1e", " 1", "1 ", "0x1F" }) {
			assertFalse(NumberUtilities.isNumber(value), "Expected no number: " + value);
		}
	}
}
