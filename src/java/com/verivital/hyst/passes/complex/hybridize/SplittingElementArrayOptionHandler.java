package com.verivital.hyst.passes.complex.hybridize;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMTRawPass.SpaceSplittingElement;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMTRawPass.SplittingElement;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMTRawPass.TimeSplittingElement;

/**
 * An args4j optionhanlder for splitting elements, which are either (time) or (pt;gradient)
 *
 * @author Stanley Bak
 */
public class SplittingElementArrayOptionHandler extends OptionHandler<SplittingElement>
{
	public SplittingElementArrayOptionHandler(CmdLineParser parser, OptionDef option,
			Setter<SplittingElement> setter)
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

				SplittingElement splittingElement = null;

				// trim parenthesis
				partStr = partStr.substring(1, partStr.length() - 1);

				try
				{
					if (!partStr.contains(";"))
					{
						// time
						splittingElement = new TimeSplittingElement(Double.parseDouble(partStr));
					}
					else
					{
						// space
						String[] dims = partStr.split(";");

						if (dims.length != 2)
							throw new CmdLineException(owner, "Expected two parts in space trigger "
									+ "(point,gradient): '" + partStr + "'");

						String[] ptParts = dims[0].split(",");
						String[] gradParts = dims[1].split(",");

						if (numDims == -1)
							numDims = ptParts.length;

						if (ptParts.length != numDims || gradParts.length != numDims)
							throw new CmdLineException(owner,
									"Mismatch in number of dimensions " + "(expected " + numDims
											+ ") in space trigger: '" + partStr + "'");

						SpaceSplittingElement ele = new SpaceSplittingElement();
						splittingElement = ele;
						ele.pt = new HyperPoint(numDims);
						ele.gradient = new double[numDims];

						for (int i = 0; i < numDims; ++i)
						{
							double ptX = Double.parseDouble(ptParts[i]);
							double gradX = Double.parseDouble(gradParts[i]);

							ele.pt.dims[i] = ptX;
							ele.gradient[i] = gradX;
						}
					}
				}
				catch (NumberFormatException e)
				{
					// deprecated: we don't have proper locale support in Hyst
					throw new CmdLineException(owner,
							"Error parsing argument as number: '" + partStr + "'");
				}

				setter.addValue(splittingElement);
			}
		}

		return counter;
	}
}
