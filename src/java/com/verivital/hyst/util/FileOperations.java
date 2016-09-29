package com.verivital.hyst.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import com.verivital.hyst.main.Hyst;

public class FileOperations
{
	/**
	 * Locate an executable by looking through all directories on first an environment variable, and
	 * second on PATH.
	 * 
	 * This never returns null.
	 * 
	 * @param programName
	 *            the executable name to look for.
	 * @param englishName
	 *            the name for error / success displays
	 * @param envVarName
	 *            the name of the environment variable to check before checking PATH
	 * @return the path of the found executable
	 * @throws FileNotFoundException
	 *             if the executable can not be found
	 */
	public static String locate(String programName, String englishName, String envVarName)
			throws FileNotFoundException
	{
		String rv = null;
		String[] tryExtensions = { "", ".exe" };

		// look for program name among PATH
		Map<String, String> env = System.getenv();

		for (String var : new String[] { envVarName, "PATH", "Path" })
		{
			String directories = env.get(var);

			if (directories == null)
			{
				Hyst.log("Environment variable was not defined (skipping): " + envVarName);
				continue;
			}

			String[] paths = directories.split(File.pathSeparator);

			for (String s : paths)
			{
				String dir = s;

				if (!dir.endsWith(File.separator))
					dir += File.separator;

				for (String suffix : tryExtensions)
				{
					String path = dir + programName + suffix;

					File f = new File(path);

					if (f.exists())
					{
						rv = path;
						break;
					}
				}

				if (rv != null) // already found
					break;
			}

			if (rv != null) // already found
				break;
		}

		if (rv == null)
		{
			String text = "The " + englishName
					+ " executable was not found in any of the directories in environment variable "
					+ envVarName + " or PATH. Please add " + programName
					+ " to your PATH environment variable.";

			throw new FileNotFoundException(text);
		}

		return rv;
	}
}
