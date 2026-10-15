package de.soderer.console;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.soderer.console.utilities.Utilities;

/**
 * Console input for file paths with auto completion from the file system: Tab completes or steps into a directory,
 * Shift+Tab steps up to the parent directory, Page up and Page down scroll through the siblings of the current path.
 * "~" is supported for the user's home directory.
 */
public class FilepathConsoleInput extends SimpleConsoleInput {
	/**
	 * Creates an input with default settings.
	 */
	public FilepathConsoleInput() {
		// Default settings
	}

	@Override
	public void setPresetContent(final String presetContent) {
		this.presetContent = (presetContent == null ? "" : presetContent.replace("/", File.separator).replace("\\", File.separator));
	}

	@Override
	protected List<String> getAutoCompletionStrings(String checkContent, final int keyCode) {
		boolean useTilde = false;
		// "~" only stands for the home directory at the start of the path (e.g. not in Windows short names like "PROGRA~1")
		if (checkContent.startsWith("~")) {
			checkContent = Utilities.replaceUsersHome(checkContent);
			useTilde = true;
		}
		checkContent = checkContent.replace("/", File.separator).replace("\\", File.separator);
		final List<String> returnList = new ArrayList<>();
		if (keyCode == ConsoleUtilities.KeyCode_ReverseTab) {
			// Shift+Tab for stepping up to the parent directory (counterpart of Tab stepping down into a directory)
			if (Utilities.isNotBlank(checkContent) && checkContent.contains(File.separator)) {
				String path = checkContent;
				// A trailing separator marks a directory, whose parent is wanted, not the directory itself
				while (path.length() > 1 && path.endsWith(File.separator)) {
					path = path.substring(0, path.length() - 1);
				}
				final File parentDirectory = new File(path).getParentFile();
				if (parentDirectory != null) {
					final String parentPath = parentDirectory.toString();
					returnList.add(parentPath.endsWith(File.separator) ? parentPath : parentPath + File.separator);
				}
			} else if (autoCompletionStrings != null) {
				returnList.addAll(autoCompletionStrings);
			}
		} else if (keyCode == ConsoleUtilities.KeyCode_Tab) {
			// Tab for auto completing or downstepping to child file
			if (Utilities.isNotBlank(checkContent) && checkContent.contains(File.separator)) {
				if (!new File(checkContent).exists()) {
					final File basedir = new File(checkContent).getParentFile();
					if (basedir != null) {
						final File[] fileList = listFilesSorted(basedir);
						if (fileList != null) {
							for (final File file : fileList) {
								if (file.isDirectory()) {
									returnList.add(file.toString() + File.separator);
								} else {
									returnList.add(file.toString());
								}
							}
						}
					}
				} else if (new File(checkContent).isDirectory()) {
					final File[] fileList = listFilesSorted(new File(checkContent));
					if (fileList != null) {
						if (fileList.length > 0) {
							if (fileList[0].isDirectory()) {
								returnList.add(fileList[0].toString() + File.separator);
							} else {
								returnList.add(fileList[0].toString());
							}
						}
					}
				}
			} else if (autoCompletionStrings != null) {
				returnList.addAll(autoCompletionStrings);
			}
		} else {
			// Page up and Page down for scrolling through file siblings
			if (Utilities.isNotBlank(checkContent) && checkContent.contains(File.separator)) {
				if (new File(checkContent).exists()) {
					final File basedir = new File(checkContent).getParentFile();
					if (basedir != null) {
						final File[] fileList = listFilesSorted(basedir);
						if (fileList != null) {
							for (final File file : fileList) {
								if (file.isDirectory()) {
									returnList.add(file.toString() + File.separator);
								} else {
									returnList.add(file.toString());
								}
							}
						}
					}
				}
			} else if (autoCompletionStrings != null) {
				returnList.addAll(autoCompletionStrings);
			}
		}

		if (useTilde) {
			return returnList.stream().map(item -> Utilities.replaceUsersHomeByTilde(item)).collect(Collectors.toList());
		} else {
			return returnList;
		}
	}

	/**
	 * Converts '/' typed or pasted on Windows into the Windows path separator (backslash), so the content matches the
	 * auto completion values, which always use the system's path separator. On other systems a backslash is a valid
	 * file name character and nothing is converted.
	 */
	@Override
	protected String normalizeInsertedText(final String text) {
		return isWindowsPathSyntax() ? text.replace('/', '\\') : text;
	}

	/**
	 * File names on Windows are case insensitive, so e.g. "c:/program files/com" also completes to
	 * "C:\Program Files\Common Files" on Tab.
	 */
	@Override
	protected boolean matchesAutoCompletionPrefix(final String autoCompletionString, final String prefix) {
		if (isWindowsPathSyntax()) {
			return autoCompletionString.regionMatches(true, 0, prefix, 0, prefix.length());
		} else {
			return super.matchesAutoCompletionPrefix(autoCompletionString, prefix);
		}
	}

	/**
	 * File names on Windows are case insensitive, so "c:\program files" is found as "C:\Program Files" when stepping
	 * through the siblings with Page up/down.
	 */
	@Override
	protected boolean isSameAutoCompletionValue(final String autoCompletionString, final String content) {
		if (isWindowsPathSyntax()) {
			return autoCompletionString.equalsIgnoreCase(content);
		} else {
			return super.isSameAutoCompletionValue(autoCompletionString, content);
		}
	}

	/**
	 * Package-private and non-static only to allow tests of the Windows behavior on other systems.
	 */
	@SuppressWarnings("static-method")
	boolean isWindowsPathSyntax() {
		return File.separatorChar == '\\';
	}

	/**
	 * Lists the files of a directory sorted by name, because File.listFiles() returns them in no guaranteed order.
	 * Otherwise Tab and Page up/down would step through the files in an arbitrary, file system dependent order.
	 */
	private static File[] listFilesSorted(final File directory) {
		final File[] fileList = directory.listFiles();
		if (fileList != null) {
			Arrays.sort(fileList);
		}
		return fileList;
	}
}
