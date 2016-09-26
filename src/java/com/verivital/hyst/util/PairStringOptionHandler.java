package com.verivital.hyst.util;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/**
 * An {@link OptionHandler} which parses Strings which are expected as multiples of two. Stops when
 * the first non-multiple of two is encountered (because it starts with a hyphen '-').
 *
 * This {@code OptionHandler} scans for parameter which begins with <tt>-(NON_NUMBER)</tt>. If
 * found, it will stop.
 * </p>
 *
 * @author Stanley Bak
 */
public class PairStringOptionHandler extends OptionHandler<String[]>
{
	public PairStringOptionHandler(CmdLineParser parser, OptionDef option, Setter<String[]> setter)
	{
		super(parser, option, setter);
	}

	@Override
	public String getDefaultMetaVariable()
	{
		return Messages.DEFAULT_META_STRING_ARRAY_OPTION_HANDLER.format();
	}

	/**
	 * Tries to parse {@code Double[]} argument from {@link Parameters}.
	 */
	@SuppressWarnings("deprecation") // we don't have proper locale support in
										// Hyst
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
						"Excepted pairs of arguments. Argument missing after option '"
								+ option.toString() + "'.");
			}

			String param2 = params.getParameter(counter + 1);

			setter.addValue(new String[] { param1, param2 });
		}

		return counter;
	}
}
