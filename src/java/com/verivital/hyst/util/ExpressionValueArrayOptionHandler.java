package com.verivital.hyst.util;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;

/**
 * An {@link OptionHandler} for greedily mapping a list of tokens into a collection of
 * {@link Expression}s
 * 
 * This one uses FormulaParser.parseValue to get the expression.
 *
 * This {@code OptionHandler} scans for parameter which begins with <tt>-(NON_NUMBER)</tt>. If
 * found, it will stop. Use parenthesis around expressions that start with -var_name to resolve
 * parsing issues.
 * </p>
 *
 * @author Stanley Bak
 */
public class ExpressionValueArrayOptionHandler extends OptionHandler<Expression>
{
	public ExpressionValueArrayOptionHandler(CmdLineParser parser, OptionDef option,
			Setter<Expression> setter)
	{
		super(parser, option, setter);
	}

	@Override
	public String getDefaultMetaVariable()
	{
		return Messages.DEFAULT_META_STRING_ARRAY_OPTION_HANDLER.format();
	}

	/**
	 * Tries to parse {@code Expression[]} argument from {@link Parameters}.
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
			// use parenthesis around -varName expressions to resolve ambiguity
			if (param.startsWith("-")
					&& (param.length() < 2 || !Character.isDigit(param.charAt(1))))
				break;

			try
			{
				setter.addValue(FormulaParser.parseValue(param));
			}
			catch (AutomatonExportException e)
			{
				// deprecated: we don't have proper locale support in Hyst
				throw new CmdLineException(owner, "Error parsing argument as value expression: '"
						+ param + "': " + e.toString());
			}
		}

		return counter;
	}
}
