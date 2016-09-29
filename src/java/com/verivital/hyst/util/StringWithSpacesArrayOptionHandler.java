package com.verivital.hyst.util;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/**
 * <p>
 * An {@link OptionHandler} for greedily mapping a list of tokens into a collection of
 * {@link String}s (such as {@code String[]}, {@code List<String>} , etc.).
 * </p>
 *
 * This one allows spaces within the arguments.
 *
 * This {@code OptionHandler} scans for parameter which begins with <tt>-</tt>. If found, it will
 * stop.
 * </p>
 *
 * @author PlainText,LuVar
 */
public class StringWithSpacesArrayOptionHandler extends OptionHandler<String>
{

	public StringWithSpacesArrayOptionHandler(CmdLineParser parser, OptionDef option,
			Setter<String> setter)
	{
		super(parser, option, setter);
	}

	/**
	 * Returns {@code "STRING[]"}.
	 *
	 * @return return "STRING[]";
	 */
	@Override
	public String getDefaultMetaVariable()
	{
		return Messages.DEFAULT_META_STRING_ARRAY_OPTION_HANDLER.format();
	}

	/**
	 * Tries to parse {@code String[]} argument from {@link Parameters}.
	 */
	@Override
	public int parseArguments(Parameters params) throws CmdLineException
	{
		int counter = 0;
		for (; counter < params.size(); counter++)
		{
			String param = params.getParameter(counter);

			boolean nextAlpha = false;

			if (param.length() > 1 && Character.isAlphabetic(param.charAt(1)))
				nextAlpha = true;

			if (param.startsWith("-") && nextAlpha)
				break;

			setter.addValue(param);
		}

		return counter;
	}

}