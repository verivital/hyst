package com.verivital.hyst.util;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.Interval;

/**
 * An {@link OptionHandler} for greedily mapping a list of tokens into a collection of
 * {@link HyperPoint}s (such as {@code HyperPoint[]}, {@code List
 * <HyperPoint>}, etc.).
 *
 * This {@code OptionHandler} scans for parameter which begins with <tt>-</tt>. If found, it will
 * stop.
 * </p>
 * 
 * All the HyperRectangles must be the same dimensionality. Each HyperRectangles is a has two
 * numbers for each dimension (comma-separated), with dimensions separated by semicolons (no
 * spaces), surrounded by parentheses, like: (-1,1;-2,-1;0.2,0.4)
 * 
 * The parenthesis are necessary because -1 would be interpreted as an argument on it's own (since
 * it starts with a dash).
 *
 * @author Stanley Bak
 */
public class HyperRectangleArrayOptionHandler extends OptionHandler<HyperRectangle>
{
	public HyperRectangleArrayOptionHandler(CmdLineParser parser, OptionDef option,
			Setter<HyperRectangle> setter)
	{
		super(parser, option, setter);
	}

	@Override
	public String getDefaultMetaVariable()
	{
		return Messages.DEFAULT_META_STRING_ARRAY_OPTION_HANDLER.format();
	}

	/**
	 * Tries to parse {@code HyperPoint[]} argument from {@link Parameters}.
	 */
	@SuppressWarnings("deprecation") // we don't have proper locale support in
										// Hyst
	@Override
	public int parseArguments(Parameters params) throws CmdLineException
	{
		int counter = 0;
		int numDims = -1;

		for (; counter < params.size(); counter++)
		{
			String param = params.getParameter(counter);

			if (param.startsWith("-"))
				break;

			for (String partStr : param.split(" "))
			{
				if (!partStr.startsWith("(") || !partStr.endsWith(")"))
				{
					// deprecated: we don't have proper locale support in Hyst
					throw new CmdLineException(owner,
							"Argument should start and end with parenthesis: '" + partStr + "'");
				}

				// trim parenthesis
				partStr = partStr.substring(1, partStr.length() - 1);

				String[] dims = partStr.split(";");

				if (numDims == -1)
					numDims = dims.length;
				else if (numDims != dims.length)
				{
					// deprecated: we don't have proper locale support in Hyst
					throw new CmdLineException(owner,
							"Argument has wrong number of dimensions (expected " + numDims + "): '"
									+ partStr + "'");
				}

				HyperRectangle hr = new HyperRectangle(dims.length);

				for (int i = 0; i < dims.length; ++i)
				{
					try
					{
						String[] minmax = dims[i].split(",");

						if (minmax.length != 2)
						{
							// deprecated: we don't have proper locale support
							// in Hyst
							throw new CmdLineException(owner,
									"Dimension in HyperRectangle expected two parts: '" + dims[i]
											+ "' in '" + partStr + "'");
						}

						double min = Double.parseDouble(minmax[0]);
						double max = Double.parseDouble(minmax[1]);

						if (max < min)
						{
							// deprecated: we don't have proper locale support
							// in Hyst
							throw new CmdLineException(owner,
									"min > max in dimension in HyperRectangle: '" + dims[i]
											+ "' in '" + partStr + "'");
						}

						hr.dims[i] = new Interval(min, max);
					}
					catch (NumberFormatException e)
					{
						// deprecated: we don't have proper locale support in
						// Hyst
						throw new CmdLineException(owner,
								"Error parsing argument as number: '" + dims[i] + "'");
					}
				}

				setter.addValue(hr);
			}
		}

		return counter;
	}
}
