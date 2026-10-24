package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.soderer.console.ConsoleUtilities.TextColor;

/**
 * Tests of the output of {@link ConsoleMenu}. The menu input itself needs a console, so the menus are only shown up
 * to the input, where reading fails immediately without console.
 */
@SuppressWarnings("static-method")
public class ConsoleMenuOutputTest {
	@Test
	public void testPrintMessagesShowsAndClearsAllLists() throws Exception {
		final ConsoleMenu menu = new ConsoleMenu("My Application");
		menu.getErrors().add("Connection failed");
		menu.getWarnings().add("Disk almost full");
		menu.getMessages().add("Saved");

		final String output = ConsoleTestHelper.captureSystemOut(menu::printMessages);

		assertTrue(output.contains(ConsoleUtilities.getAnsiColoredText("Error: Connection failed", TextColor.Light_red)), output);
		assertTrue(output.contains(ConsoleUtilities.getAnsiColoredText("Warning: Disk almost full", TextColor.Yellow)), output);
		assertTrue(output.contains(ConsoleUtilities.getAnsiColoredText("Saved", TextColor.Green)), output);
		assertTrue(output.indexOf("Error:") < output.indexOf("Warning:") && output.indexOf("Warning:") < output.indexOf("Saved"), output);
		assertTrue(menu.getErrors().isEmpty());
		assertTrue(menu.getWarnings().isEmpty());
		assertTrue(menu.getMessages().isEmpty());

		// Nothing left to print
		assertTrue(ConsoleTestHelper.captureSystemOut(menu::printMessages).isEmpty());
	}

	@Test
	public void testShowMainMenuUpToTheInput() throws Exception {
		assumeTrue(ConsoleTestHelper.isWithoutConsole(), "Needs an environment without console, where the input fails immediately");
		final ConsoleMenu mainMenu = new ConsoleMenu("My Application");
		final ConsoleMenu settingsMenu = new ConsoleMenu(mainMenu, "Settings");
		for (int i = 2; i <= 10; i++) {
			@SuppressWarnings("unused")
			final
			ConsoleMenu consoleMenu = new ConsoleMenu(mainMenu, "Menu " + i);
		}
		mainMenu.getMessages().add("Welcome");

		final String output = ConsoleTestHelper.captureSystemOut(() -> assertThrows(IllegalStateException.class, mainMenu::show));

		assertTrue(output.contains("* My Application *"), output);
		assertTrue(output.contains("Main menu"), output);
		assertTrue(output.contains("Welcome"), output);
		assertTrue(output.contains("Please choose (Blank => Quit):"), output);
		assertTrue(output.contains("  1) Settings"), output);
		assertTrue(output.contains(" 10) Menu 10"), output);
		assertTrue(output.contains("  0) Quit"), output);
		assertTrue(mainMenu.getMessages().isEmpty());

		final String subMenuOutput = ConsoleTestHelper.captureSystemOut(() -> assertThrows(IllegalStateException.class, settingsMenu::show));
		assertTrue(subMenuOutput.contains("Please choose (Blank => Back to Main menu):"), subMenuOutput);
		assertTrue(subMenuOutput.contains("  0) Back to Main menu"), subMenuOutput);
	}

	@Test
	public void testAskForSelectionUpToTheInput() throws Exception {
		assumeTrue(ConsoleTestHelper.isWithoutConsole(), "Needs an environment without console, where the input fails immediately");
		final List<String> values = Arrays.asList("red", "green", "blue");

		final String output = ConsoleTestHelper.captureSystemOut(() -> assertThrows(IllegalStateException.class, () -> ConsoleMenu.askForSelection("Choose a color", values)));

		assertTrue(output.contains("Choose a color (Blank => Cancel)"), output);
		assertTrue(output.contains("  1) red"), output);
		assertTrue(output.contains("  3) blue"), output);
		assertFalse(output.contains("  4)"), output);
	}
}
