package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link SimpleConsoleInput} and {@link PasswordConsoleInput}, which need no console: settings, the override
 * hooks, the headless safe beep and clipboard access, and the fallback input path without console.
 */
@SuppressWarnings("static-method")
public class ConsoleInputBehaviorTest {
	@Test
	public void testSimpleConsoleInputSetters() {
		final SimpleConsoleInput input = new SimpleConsoleInput();
		input.setSilent(true);
		input.setAllowedCharacters("\t");
		input.setHideCharacter('#');
		input.setPrompt("Name: ");
		input.setPresetContent("preset");
		final List<String> values = Arrays.asList("a", "b");
		input.setAutoCompletionStrings(values);

		assertTrue(input.isSilent());
		assertEquals("\t", input.getAllowedCharacters());
		assertEquals(Character.valueOf('#'), input.getHideCharacter());
		assertEquals("Name: ", input.getPrompt());
		assertEquals("preset", input.getPresetContent());
		assertSame(values, input.getAutoCompletionStrings());
		assertSame(values, input.getAutoCompletionStrings("x", ConsoleUtilities.KeyCode_PageDown));

		input.setHideCharacter(null);
		input.setAutoCompletionStrings(null);
		assertNull(input.getHideCharacter());
		assertNull(input.getAutoCompletionStrings("x", ConsoleUtilities.KeyCode_Tab));
	}

	@Test
	public void testSimpleConsoleInputHooks() {
		final SimpleConsoleInput input = new SimpleConsoleInput();
		assertEquals("a/b\\c", input.normalizeInsertedText("a/b\\c"));
		assertTrue(input.matchesAutoCompletionPrefix("value", "val"));
		assertTrue(input.matchesAutoCompletionPrefix("value", ""));
		assertFalse(input.matchesAutoCompletionPrefix("value", "VAL"));
		assertTrue(input.isSameAutoCompletionValue("value", "value"));
		assertFalse(input.isSameAutoCompletionValue("value", "Value"));
	}

	@Test
	public void testPasswordConsoleInputSetters() {
		final PasswordConsoleInput input = new PasswordConsoleInput();
		input.setSilent(true);
		input.setAllowedCharacters("\t");
		input.setPrompt(null);
		input.setPresetContent("secret");

		assertTrue(input.isSilent());
		assertEquals("\t", input.getAllowedCharacters());
		assertEquals("", input.getPrompt());
		assertEquals("secret", input.getPresetContent());
	}

	@Test
	public void testBeepAndClipboardNeverFailHeadless() throws Exception {
		assumeTrue(GraphicsEnvironment.isHeadless(), "Only headless the clipboard is certainly not available");
		for (final Class<?> inputClass : new Class<?>[] { SimpleConsoleInput.class, PasswordConsoleInput.class }) {
			// Headless the AWT toolkit has no clipboard and cannot beep, which must not end the input
			ConsoleTestHelper.invokeStatic(inputClass, "beep", new Class<?>[0]);
			assertNull(ConsoleTestHelper.invokeStatic(inputClass, "readClipboardTextOrNull", new Class<?>[0]));
		}
	}

	@Test
	public void testSimpleConsoleInputWithoutConsole() {
		assumeTrue(ConsoleTestHelper.isWithoutConsole(), "Needs an environment without console, where the input fails immediately");
		final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new SimpleConsoleInput().withPrompt("> ").readInput());
		assertTrue(exception.getMessage().startsWith("No console available"), exception.getMessage());
		assertThrows(IllegalStateException.class, () -> new SimpleConsoleInput().withHideCharacter('*').readInput());
		assertThrows(IllegalStateException.class, () -> new FilepathConsoleInput().readInput());
	}

	@Test
	public void testPasswordConsoleInputWithoutConsole() {
		assumeTrue(ConsoleTestHelper.isWithoutConsole(), "Needs an environment without console, where the input fails immediately");
		final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new PasswordConsoleInput().withPrompt("Password: ").readInput());
		assertTrue(exception.getMessage().startsWith("No console available"), exception.getMessage());
	}
}
