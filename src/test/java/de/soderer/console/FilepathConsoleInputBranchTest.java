package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Additional tests of the auto completion of {@link FilepathConsoleInput}.
 */
@SuppressWarnings("static-method")
public class FilepathConsoleInputBranchTest {
	private Path tempDirectory;
	private String originalUserHome;

	@BeforeEach
	public void setUp() throws IOException {
		originalUserHome = System.getProperty("user.home");
		tempDirectory = Files.createTempDirectory("FilepathConsoleInputBranchTest").toRealPath();
		// Sorted by name the first entry is a directory
		Files.createDirectory(tempDirectory.resolve("a_directory"));
		Files.createFile(tempDirectory.resolve("a_directory").resolve("child.txt"));
		Files.createFile(tempDirectory.resolve("b_file.txt"));
		Files.createDirectory(tempDirectory.resolve("empty"));
	}

	@AfterEach
	public void tearDown() throws IOException {
		System.setProperty("user.home", originalUserHome);
		try (Stream<Path> paths = Files.walk(tempDirectory)) {
			for (final Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
				Files.deleteIfExists(path);
			}
		}
	}

	private String path(final String name) {
		return tempDirectory.resolve(name).toString();
	}

	@Test
	public void testTabOnDirectoryWhoseFirstChildIsDirectory() {
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings(tempDirectory.toString(), ConsoleUtilities.KeyCode_Tab);
		assertEquals(Collections.singletonList(path("a_directory") + File.separator), result);
	}

	@Test
	public void testTabOnEmptyDirectoryOffersNothing() {
		assertTrue(new FilepathConsoleInput().getAutoCompletionStrings(path("empty"), ConsoleUtilities.KeyCode_Tab).isEmpty());
	}

	@Test
	public void testTabInMissingDirectoryOffersNothing() {
		final String missingPath = path("missing") + File.separator + "file";
		assertTrue(new FilepathConsoleInput().getAutoCompletionStrings(missingPath, ConsoleUtilities.KeyCode_Tab).isEmpty());
	}

	@Test
	public void testTabWithoutConfiguredValues() {
		assertTrue(new FilepathConsoleInput().getAutoCompletionStrings("", ConsoleUtilities.KeyCode_Tab).isEmpty());
		assertTrue(new FilepathConsoleInput().getAutoCompletionStrings("   ", ConsoleUtilities.KeyCode_Tab).isEmpty());
	}

	@Test
	public void testPageUpOnExistingDirectoryOffersSiblingsWithSeparator() {
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings(path("b_file.txt"), ConsoleUtilities.KeyCode_PageUp);
		assertEquals(Arrays.asList(path("a_directory") + File.separator, path("b_file.txt"), path("empty") + File.separator), result);
	}

	@Test
	public void testPageDownWithoutSeparator() {
		final FilepathConsoleInput input = new FilepathConsoleInput();
		assertTrue(input.getAutoCompletionStrings("abc", ConsoleUtilities.KeyCode_PageDown).isEmpty());
		input.addAutoCompletionString("value");
		assertEquals(Collections.singletonList("value"), input.getAutoCompletionStrings("abc", ConsoleUtilities.KeyCode_PageDown));
	}

	@Test
	public void testReverseTabWithoutSeparator() {
		final FilepathConsoleInput input = new FilepathConsoleInput();
		assertTrue(input.getAutoCompletionStrings("abc", ConsoleUtilities.KeyCode_ReverseTab).isEmpty());
		input.addAutoCompletionString("value");
		assertEquals(Collections.singletonList("value"), input.getAutoCompletionStrings("abc", ConsoleUtilities.KeyCode_ReverseTab));
	}

	@Test
	public void testReverseTabWithSeveralTrailingSeparators() {
		final String directoryWithSeparators = path("a_directory") + File.separator + File.separator;
		assertEquals(Collections.singletonList(tempDirectory.toString() + File.separator), new FilepathConsoleInput().getAutoCompletionStrings(directoryWithSeparators, ConsoleUtilities.KeyCode_ReverseTab));
	}

	@Test
	public void testReverseTabWithTilde() {
		System.setProperty("user.home", tempDirectory.toString());
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings("~" + File.separator + "a_directory" + File.separator + "child.txt", ConsoleUtilities.KeyCode_ReverseTab);
		assertEquals(Collections.singletonList("~" + File.separator + "a_directory" + File.separator), result);
	}

	@Test
	public void testTildeInsideNameIsNoHomeDirectory() {
		// A "~" which is not at the start of the path is a normal character (e.g. Windows short names like "PROGRA~1")
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings(path("b~"), ConsoleUtilities.KeyCode_Tab);
		assertEquals(Arrays.asList(path("a_directory") + File.separator, path("b_file.txt"), path("empty") + File.separator), result);
	}
}
