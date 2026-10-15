package de.soderer.console;

import java.util.ArrayList;
import java.util.List;

import de.soderer.console.ConsoleUtilities.TextColor;
import de.soderer.console.utilities.NumberUtilities;
import de.soderer.console.utilities.Utilities;

/**
 * Text menu for console applications, consisting of a main menu and nested sub menus.
 * Each menu shows its title, pending messages, warnings and errors, and the numbered list of its sub menus.
 */
public class ConsoleMenu {
	private final String mainTitle;
	private final String menuTitle;
	private final ConsoleMenu parentMenu;
	private final List<ConsoleMenu> subMenus = new ArrayList<>();

	private final List<String> messages = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();

	/**
	 * Creates the main menu.
	 *
	 * @param mainTitle title shown boxed at the top of every menu screen
	 */
	public ConsoleMenu(final String mainTitle) {
		this.mainTitle = mainTitle;
		menuTitle = "Main menu";
		parentMenu = null;
	}

	/**
	 * Creates a sub menu and adds it to its parent menu.
	 *
	 * @param parentMenu menu to add this sub menu to
	 * @param menuTitle title of this menu
	 * @throws Exception if the parent menu is missing
	 */
	public ConsoleMenu(final ConsoleMenu parentMenu, final String menuTitle) throws Exception {
		if (parentMenu == null) {
			throw new Exception("Invalid empty parent menu");
		}
		this.parentMenu = parentMenu;
		mainTitle = parentMenu.getMainTitle();
		this.menuTitle = menuTitle;
		// Registered at the parent only after all fields are set, so the parent never sees a half initialized menu.
		// The compiler warning "this-escape" remains for subclasses (their own fields are not set yet at this point),
		// which is harmless here, because the parent only uses the sub menu later in show().
		parentMenu.addSubMenu(this);
	}

	/**
	 * Returns the title shown boxed at the top of every menu screen.
	 *
	 * @return main title
	 */
	public String getMainTitle() {
		return mainTitle;
	}

	/**
	 * Returns the parent menu.
	 *
	 * @return parent menu, or null for the main menu
	 */
	public ConsoleMenu getParentMenu() {
		return parentMenu;
	}

	/**
	 * Returns the title of this menu.
	 *
	 * @return menu title
	 */
	public String getTitle() {
		return menuTitle;
	}

	/**
	 * Returns the messages shown (in green) the next time this menu is displayed. They are cleared after display.
	 *
	 * @return modifiable list of messages
	 */
	public List<String> getMessages() {
		return messages;
	}

	/**
	 * Returns the warnings shown (in yellow) the next time this menu is displayed. They are cleared after display.
	 *
	 * @return modifiable list of warnings
	 */
	public List<String> getWarnings() {
		return warnings;
	}

	/**
	 * Returns the errors shown (in red) the next time this menu is displayed. They are cleared after display.
	 *
	 * @return modifiable list of errors
	 */
	public List<String> getErrors() {
		return errors;
	}

	private ConsoleMenu addSubMenu(final ConsoleMenu subMenu) {
		subMenus.add(subMenu);
		return this;
	}

	/**
	 * Shows this menu and handles the user's choices until the user goes back. A sub menu can be chosen by its
	 * number or by its title (with auto completion, arrow up/down cycles through the titles). Entering "q", "quit"
	 * or "exit" ends the application immediately via System.exit(0).
	 * Sub classes override this method to implement the actual menu action.
	 *
	 * @return 0 when the user goes back, a negative value returned by a sub menu to leave all menus
	 * @throws Exception if the console input fails
	 */
	public int show() throws Exception {
		while (true) {
			ConsoleUtilities.clearScreen();

			ConsoleUtilities.printBoxed(mainTitle);
			System.out.println();

			System.out.println(menuTitle);
			System.out.println();
			printMessages();

			String defaultMenuTitle;
			if (parentMenu == null) {
				defaultMenuTitle = "Quit";
			} else {
				defaultMenuTitle = "Back to " + parentMenu.getTitle();
			}

			System.out.println("Please choose (Blank => " + defaultMenuTitle + "):");

			int subMenuIndex = 0;
			for (final ConsoleMenu subMenu : subMenus) {
				subMenuIndex++;
				System.out.println(" " + (subMenuIndex < 10 ? " " : "") + subMenuIndex + ") " + subMenu.getTitle());
			}

			System.out.println("  0) " + defaultMenuTitle);

			// Menu titles as auto completion values, so arrow up/down cycles through the texts
			final List<String> autoCompletionStrings = new ArrayList<>();
			autoCompletionStrings.add("");
			for (final ConsoleMenu subMenu : subMenus) {
				autoCompletionStrings.add(subMenu.getTitle());
			}
			autoCompletionStrings.add(defaultMenuTitle);

			String choice = new SimpleConsoleInput().withAutoCompletionStrings(autoCompletionStrings).withPrompt(" > ").readInput();
			choice = choice == null ? "" : choice.trim();

			if (Utilities.isBlank(choice)) {
				return 0;
			} else if ("q".equalsIgnoreCase(choice) || "quit".equalsIgnoreCase(choice) || "exit".equalsIgnoreCase(choice)) {
				ConsoleUtilities.clearScreen();
				System.exit(0);
			} else {
				// Accept a menu title (e.g. selected via arrow keys) as well as a menu number.
				// Titles are checked first, so a title consisting of digits also works.
				Integer choiceInt = null;
				if (defaultMenuTitle.equalsIgnoreCase(choice)) {
					choiceInt = 0;
				} else {
					for (int i = 0; i < subMenus.size(); i++) {
						if (subMenus.get(i).getTitle().equalsIgnoreCase(choice)) {
							choiceInt = i + 1;
							break;
						}
					}
				}

				// Only the parsing of the input is guarded here. Exceptions of the sub menus must not be
				// swallowed and reported as invalid input.
				if (choiceInt == null) {
					try {
						choiceInt = Integer.parseInt(choice);
					} catch (@SuppressWarnings("unused") final NumberFormatException e) {
						choiceInt = null;
					}
				}

				if (choiceInt == null) {
					errors.add("Invalid input: " + choice);
				} else if (choiceInt == 0) {
					return 0;
				} else if (1 <= choiceInt && choiceInt <= subMenus.size()) {
					final int returnValue = subMenus.get(choiceInt - 1).show();
					if (returnValue < 0) {
						return returnValue;
					}
				} else {
					errors.add("Invalid input: " + choice);
				}
			}
		}
	}

	/**
	 * Prints and clears the pending errors, warnings and messages.
	 */
	protected void printMessages() {
		if (errors.size() > 0) {
			for (final String error : errors) {
				System.out.println(ConsoleUtilities.getAnsiColoredText("Error: " + error, TextColor.Light_red));
				System.out.println();
			}
			errors.clear();
		}

		if (warnings.size() > 0) {
			for (final String warning : warnings) {
				System.out.println(ConsoleUtilities.getAnsiColoredText("Warning: " + warning, TextColor.Yellow));
			}
			System.out.println();
			warnings.clear();
		}

		if (messages.size() > 0) {
			for (final String message : messages) {
				System.out.println(ConsoleUtilities.getAnsiColoredText(message, TextColor.Green));
			}
			System.out.println();
			messages.clear();
		}
	}

	/**
	 * Asks the user to select one of the given values, by number or by entering the value (with auto completion).
	 *
	 * @param message question to show
	 * @param selectionValues values to select from
	 * @return selected value, or null if the user entered nothing
	 * @throws Exception if the input is no valid selection or the console input fails
	 */
	protected static String askForSelection(final String message, final List<String> selectionValues) throws Exception {
		System.out.println();
		System.out.println(message + " (Blank => Cancel)");
		int selectionIndex = 0;
		for (final String selectionValue : selectionValues) {
			selectionIndex++;
			System.out.println(" " + Utilities.leftPad(Integer.toString(selectionIndex), 2) + ") " + selectionValue);
		}
		String choice = new SimpleConsoleInput().withAutoCompletionStrings(selectionValues).withPrompt(" > ").readInput();
		choice = choice == null ? "" : choice.trim();
		if (Utilities.isBlank(choice)) {
			return null;
		} else if (selectionValues.contains(choice)) {
			// Checked first, so numeric selection values (e.g. "2024") can also be entered directly
			return choice;
		} else if (!NumberUtilities.isNumber(choice)) {
			throw new Exception("Invalid selection: " + choice);
		} else {
			final int choiseInt;
			try {
				choiseInt = Integer.parseInt(choice);
			} catch (final NumberFormatException e) {
				// e.g. "1.5" or "1e3"
				throw new Exception("Invalid selection: " + choice, e);
			}
			if (choiseInt < 1 || choiseInt > selectionValues.size()) {
				throw new Exception("Invalid selection: " + choice);
			} else {
				final String selectionItemString = selectionValues.get(choiseInt - 1);
				System.out.println(selectionItemString);
				return selectionItemString;
			}
		}
	}
}
