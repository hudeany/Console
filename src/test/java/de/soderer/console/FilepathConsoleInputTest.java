package de.soderer.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class FilepathConsoleInputTest {
	private Path tempDirectory;
	private String originalUserHome;

	@BeforeEach
	public void setUp() throws IOException {
		originalUserHome = System.getProperty("user.home");
		tempDirectory = Files.createTempDirectory("FilepathConsoleInputTest").toRealPath();
		Files.createFile(tempDirectory.resolve("alpha.txt"));
		Files.createDirectory(tempDirectory.resolve("beta"));
		Files.createFile(tempDirectory.resolve("beta").resolve("inner.txt"));
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

	@Test
	public void testPresetContentNormalizesSeparators() {
		final FilepathConsoleInput input = new FilepathConsoleInput();
		input.setPresetContent("dir/sub\\file.txt");
		assertEquals("dir" + File.separator + "sub" + File.separator + "file.txt", input.getPresetContent());

		input.setPresetContent(null);
		assertEquals("", input.getPresetContent());
	}

	@Test
	public void testTabOnUnknownFileOffersSiblings() {
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings(path("al"), ConsoleUtilities.KeyCode_Tab);
		assertEquals(sorted(path("alpha.txt"), path("beta") + File.separator), sorted(result));
	}

	@Test
	public void testTabOnDirectoryStepsIntoFirstChild() {
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings(path("beta"), ConsoleUtilities.KeyCode_Tab);
		assertEquals(Collections.singletonList(path("beta") + File.separator + "inner.txt"), result);
	}

	@Test
	public void testTabOnExistingFileOffersNothing() {
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings(path("alpha.txt"), ConsoleUtilities.KeyCode_Tab);
		assertTrue(result.isEmpty(), result.toString());
	}

	@Test
	public void testPageDownOnExistingFileOffersSiblings() {
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings(path("alpha.txt"), ConsoleUtilities.KeyCode_PageDown);
		assertEquals(sorted(path("alpha.txt"), path("beta") + File.separator), sorted(result));
	}

	@Test
	public void testPageDownOnUnknownFileOffersNothing() {
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings(path("unknown"), ConsoleUtilities.KeyCode_PageDown);
		assertTrue(result.isEmpty(), result.toString());
	}

	@Test
	public void testSlashIsAcceptedAsSeparator() {
		final String contentWithSlashes = path("al").replace(File.separatorChar, '/');
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings(contentWithSlashes, ConsoleUtilities.KeyCode_Tab);
		assertEquals(sorted(path("alpha.txt"), path("beta") + File.separator), sorted(result));
	}

	@Test
	public void testContentWithoutSeparatorUsesConfiguredValues() {
		final FilepathConsoleInput input = new FilepathConsoleInput();
		assertTrue(input.getAutoCompletionStrings("abc", ConsoleUtilities.KeyCode_Tab).isEmpty());

		input.addAutoCompletionString("first");
		assertEquals(Collections.singletonList("first"), input.getAutoCompletionStrings("abc", ConsoleUtilities.KeyCode_Tab));
		assertEquals(Collections.singletonList("first"), input.getAutoCompletionStrings("abc", ConsoleUtilities.KeyCode_PageUp));
	}

	@Test
	public void testTildeIsKeptInResults() {
		System.setProperty("user.home", tempDirectory.toString());
		final List<String> result = new FilepathConsoleInput().getAutoCompletionStrings("~" + File.separator + "al", ConsoleUtilities.KeyCode_Tab);
		assertEquals(sorted("~" + File.separator + "alpha.txt", "~" + File.separator + "beta" + File.separator), sorted(result));
	}

	private String path(final String name) {
		return tempDirectory.resolve(name).toString();
	}

	private static List<String> sorted(final String... values) {
		return sorted(Arrays.asList(values));
	}

	private static List<String> sorted(final List<String> values) {
		final List<String> sortedValues = new ArrayList<>(values);
		Collections.sort(sortedValues);
		return sortedValues;
	}
}
