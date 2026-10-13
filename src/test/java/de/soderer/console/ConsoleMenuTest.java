package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class ConsoleMenuTest {
	@Test
	public void testMainMenu() {
		final ConsoleMenu mainMenu = new ConsoleMenu("My Application");
		assertEquals("My Application", mainMenu.getMainTitle());
		assertEquals("Main menu", mainMenu.getTitle());
		assertNull(mainMenu.getParentMenu());
		assertTrue(mainMenu.getMessages().isEmpty());
		assertTrue(mainMenu.getWarnings().isEmpty());
		assertTrue(mainMenu.getErrors().isEmpty());
	}

	@Test
	public void testSubMenuInheritsMainTitle() throws Exception {
		final ConsoleMenu mainMenu = new ConsoleMenu("My Application");
		final ConsoleMenu subMenu = new ConsoleMenu(mainMenu, "Settings");
		final ConsoleMenu subSubMenu = new ConsoleMenu(subMenu, "Network");

		assertEquals("Settings", subMenu.getTitle());
		assertSame(mainMenu, subMenu.getParentMenu());
		assertEquals("My Application", subMenu.getMainTitle());

		assertSame(subMenu, subSubMenu.getParentMenu());
		assertEquals("My Application", subSubMenu.getMainTitle());
	}

	@Test
	public void testSubMenuWithoutParent() {
		assertThrows(Exception.class, () -> new ConsoleMenu(null, "Orphan"));
	}

	@Test
	public void testMessageListsAreModifiable() {
		final ConsoleMenu menu = new ConsoleMenu("My Application");
		menu.getMessages().add("Saved");
		menu.getWarnings().add("Disk almost full");
		menu.getErrors().add("Connection failed");

		assertEquals("Saved", menu.getMessages().get(0));
		assertEquals("Disk almost full", menu.getWarnings().get(0));
		assertEquals("Connection failed", menu.getErrors().get(0));
	}
}
