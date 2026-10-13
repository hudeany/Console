package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests of the configuration of the console inputs. Reading input needs a real console and is not tested here.
 */
@SuppressWarnings("static-method")
public class ConsoleInputSettingsTest {
	@Test
	public void testSimpleConsoleInputDefaults() {
		final SimpleConsoleInput input = new SimpleConsoleInput();
		assertFalse(input.isSilent());
		assertNull(input.getHideCharacter());
		assertNull(input.getAllowedCharacters());
		assertEquals("", input.getPrompt());
		assertEquals("", input.getPresetContent());
		assertNull(input.getAutoCompletionStrings());
	}

	@Test
	public void testSimpleConsoleInputFluentSetters() {
		final SimpleConsoleInput input = new SimpleConsoleInput();
		final List<String> values = Arrays.asList("yes", "no");

		assertSame(input, input.withPrompt("> "));
		assertSame(input, input.withPresetContent("preset"));
		assertSame(input, input.withHideCharacter('*'));
		assertSame(input, input.withAutoCompletionStrings(values));

		assertEquals("> ", input.getPrompt());
		assertEquals("preset", input.getPresetContent());
		assertEquals(Character.valueOf('*'), input.getHideCharacter());
		assertSame(values, input.getAutoCompletionStrings());
	}

	@Test
	public void testSimpleConsoleInputNullValuesBecomeEmpty() {
		final SimpleConsoleInput input = new SimpleConsoleInput().withPrompt(null).withPresetContent(null);
		assertEquals("", input.getPrompt());
		assertEquals("", input.getPresetContent());
	}

	@Test
	public void testSimpleConsoleInputAddAutoCompletionString() {
		final SimpleConsoleInput input = new SimpleConsoleInput().addAutoCompletionString("first").addAutoCompletionString("second");
		assertEquals(Arrays.asList("first", "second"), input.getAutoCompletionStrings());
		assertEquals(Arrays.asList("first", "second"), input.getAutoCompletionStrings("f", ConsoleUtilities.KeyCode_Tab));
	}

	@Test
	public void testPasswordConsoleInputDefaults() {
		final PasswordConsoleInput input = new PasswordConsoleInput();
		assertEquals('*', input.getHideCharacter());
		assertFalse(input.isSilent());
		assertNull(input.getAllowedCharacters());
		assertEquals("", input.getPrompt());
		assertEquals("", input.getPresetContent());
	}

	@Test
	public void testPasswordConsoleInputSettings() {
		final PasswordConsoleInput input = new PasswordConsoleInput('#');
		assertSame(input, input.withPrompt("Password: "));
		assertSame(input, input.withPresetContent(null));

		assertEquals('#', input.getHideCharacter());
		assertEquals("Password: ", input.getPrompt());
		assertEquals("", input.getPresetContent());
	}
}
