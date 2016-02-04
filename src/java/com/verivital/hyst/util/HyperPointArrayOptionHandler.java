package com.verivital.hyst.util;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import com.verivital.hyst.geometry.HyperPoint;

/**
 * An {@link OptionHandler} for greedily mapping a list of tokens into a collection of {@link HyperPoint}s
 * (such as {@code HyperPoint[]}, {@code List<HyperPoint>}, etc.).
 *
 * This {@code OptionHandler} scans for parameter which begins with <tt>-</tt>. If found, it will stop.</p>
 * 
 * All the HyperPoints must be the same dimensionality. Each Hyperpoint is a 
 * comma-separated (no spaces) list of numbers, surrounded by parenthesis. 
 * for example: (-3,4,15)
 * 
 * The parenthesis are necessary because -3 would be interpretted as an argument on it's own (since it
 * starts with a dash).
 *
 * @author Stanley Bak
 */
public class HyperPointArrayOptionHandler extends OptionHandler<HyperPoint> 
{
	public HyperPointArrayOptionHandler(CmdLineParser parser, OptionDef option, Setter<HyperPoint> setter) 
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
	@SuppressWarnings("deprecation") // we don't have proper locale support in Hyst
	@Override
	public int parseArguments(Parameters params) throws CmdLineException 
	{
		int counter = 0;
		int numDims = -1;
				
		for (; counter < params.size(); counter++) 
		{
			String param = params.getParameter(counter);

            if(param.startsWith("-"))
				break;

            for (String pointStr : param.split(" ")) 
            {
            	if (!pointStr.startsWith("(") || !pointStr.endsWith(")"))
            	{
            		// deprecated: we don't have proper locale support in Hyst
            		throw new CmdLineException(owner, "Argument should start and end with parenthesis: '" 
            				+ pointStr + "'");
            	}
            	
            	// trim parenthesis
            	pointStr = pointStr.substring(1, pointStr.length() - 1);
            	
            	String[] dims = pointStr.split(",");
            	
            	if (numDims == -1)
            		numDims = dims.length;
            	else if (numDims != dims.length)
            	{
            		// deprecated: we don't have proper locale support in Hyst
            		throw new CmdLineException(owner, "Argument has wrong number of dimensions (expected " + 
            				numDims + "): '" + pointStr + "'");
            	}
            	
            	HyperPoint hp = new HyperPoint(dims.length);
            	
            	for (int i = 0; i < dims.length; ++i)
            	{
            		try
                	{
                		hp.dims[i] = Double.parseDouble(dims[i]);
                	}
                	catch (NumberFormatException e)
                	{
                		// deprecated: we don't have proper locale support in Hyst
                		throw new CmdLineException(owner, "Error parsing argument as number: '" + dims[i] + "'");
                	}
            	}
            	
            	setter.addValue(hp);
            }
		}

        return counter;
	}
}
