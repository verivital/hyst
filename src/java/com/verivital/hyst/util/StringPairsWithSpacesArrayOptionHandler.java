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
 * This one allows spaces within the arguments. It expects pair of arguments (groups of two), and at
 * least one group.
 *
 * This {@code OptionHandler} scans for parameter which begins with <tt>-</tt>. If found, it will
 * stop.
 * </p>
 *
 * @author PlainText,LuVar
 */
public class StringPairsWithSpacesArrayOptionHandler extends OptionHandler<String>
{

	public StringPairsWithSpacesArrayOptionHandler(CmdLineParser parser, OptionDef option,
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
	@SuppressWarnings("deprecation")
	@Override
	public int parseArguments(Parameters params) throws CmdLineException
	{
		int counter = 0;
		for (; counter < params.size(); counter += 2)
		{
			String param1 = params.getParameter(counter);

			if (param1.startsWith("-"))
			{
				if (counter == 0)
				{
					// deprecated: we don't have proper locale support in Hyst
					throw new CmdLineException(owner,
							"No arguments found after a flag which requires arguments: "
									+ option.toString());
				}

				break;
			}

			if (counter + 1 == params.size())
			{
				// deprecated: we don't have proper locale support in Hyst
				throw new CmdLineException(owner,
						"Excepted pairs of arguments for option '" + option.toString() + "'.");
			}

			String param2 = params.getParameter(counter + 1);

			setter.addValue(param1);
			setter.addValue(param2);
		}

		return counter;
	}

}