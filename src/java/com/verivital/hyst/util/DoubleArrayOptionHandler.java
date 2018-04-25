package com.verivital.hyst.util;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/**
 * An {@link OptionHandler} for greedily mapping a list of tokens into a collection of
 * {@link Doubles}s (such as {@code Double[]}, {@code List
 * <Double>}, etc.).
 *
 * This {@code OptionHandler} scans for parameter which begins with <tt>-(NON_NUMBER)</tt>. If
 * found, it will stop.
 * </p>
 *
 * @author Stanley Bak
 */
public class DoubleArrayOptionHandler extends OptionHandler<Double>
{
	public DoubleArrayOptionHandler(CmdLineParser parser, OptionDef option, Setter<Double> setter)
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

		for (; counter < params.size(); counter++)
		{
			String param = params.getParameter(counter);

			// negative numbers should be okay, but we should break if it's a
			// flag
			if (param.startsWith("-")
					&& (param.length() < 2 || !Character.isDigit(param.charAt(1))))
				break;

			try
			{
				setter.addValue(Double.parseDouble(param));
			}
			catch (NumberFormatException e)
			{
				// deprecated: we don't have proper locale support in Hyst
				throw new CmdLineException(owner,
						"Error parsing argument as number: '" + param + "'");
			}
		}

		return counter;
	}
}
