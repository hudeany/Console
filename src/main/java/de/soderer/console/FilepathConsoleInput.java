package de.soderer.console;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.soderer.console.utilities.Utilities;

/**
 * Console input for file paths with auto completion from the file system: Tab completes or steps into a directory,
 * Page up and Page down scroll through the siblings of the current path. "~" is supported for the user's home directory.
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
		if (checkContent.contains("~")) {
			checkContent = Utilities.replaceUsersHome(checkContent);
			useTilde = true;
		}
		checkContent = checkContent.replace("/", File.separator).replace("\\", File.separator);
		final List<String> returnList = new ArrayList<>();
		if (keyCode == ConsoleUtilities.KeyCode_Tab) {
			// Tab for auto completing or downstepping to child file
			if (Utilities.isNotBlank(checkContent) && checkContent.contains(File.separator)) {
				if (!new File(checkContent).exists()) {
					final File basedir = new File(checkContent).getParentFile();
					if (basedir != null) {
						final File[] fileList = basedir.listFiles();
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
					final File[] fileList = new File(checkContent).listFiles();
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
						final File[] fileList = basedir.listFiles();
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
}
