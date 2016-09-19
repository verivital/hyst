package com.verivital.hyst.generators;

import java.io.ByteArrayOutputStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.CmdLineRuntimeException;

/**
 * A ModelGenerator is an alternative input method which generates a Configuration rather than
 * loading one from files. It can take parameters, for example, if the configuration generation can
 * be take inputs like the number of dimensions.
 * 
 * The parameters are automatically parsed using args4j.
 * 
 * See IntegralChainGenerator as a simple example which implements the abstract methods.
 * 
 * @author Stanley Bak (May 2016)
 *
 */
public abstract class ModelGenerator
{
	private CmdLineParser parser = new CmdLineParser(this);

	public ModelGenerator()
	{
		String flag = getCommandLineFlag();

		if (flag.startsWith("-"))
			throw new RuntimeException(
					"model generator's command-line flag shouldn't start with a hyphen: " + flag);
	}

	/**
	 * Get the longer version of the help text for this pass.
	 * 
	 * @return the help text, or null if not specified
	 */
	public String getLongHelp()
	{
		return null;
	}

	public String getParamHelp()
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		parser.printUsage(out);

		return out.toString();
	}

	public Configuration generate(String params)
	{
		if (params == null)
			params = "";

		String[] args = AutomatonUtil.extractArgs(params);

		try
		{
			parser.parseArgument(args);
		}
		catch (CmdLineException e)
		{
			String message = "Error Parsing Arguments for " + getName() + ",\n Message: "
					+ e.getMessage() + "\nArguments: '" + params + "'\n" + getParamHelp();

			throw new CmdLineRuntimeException(message, e);
		}

		Configuration c = generateModel();

		try
		{
			c.validate();
		}
		catch (AutomatonValidationException e)
		{
			throw new AutomatonExportException("Hybrid Automaton IR structure was "
					+ "invalid after generating model using " + this.getClass().getName()
					+ " with params '" + params + "': " + e.toString(), e);
		}

		return c;
	}

	/**
	 * Get the flag used to run this generator on the command line.
	 * 
	 * @return a command-line flag used to run this generator
	 */
	public abstract String getCommandLineFlag();

	/**
	 * Get the name of this model generator.
	 * 
	 * @return the name text
	 */
	public abstract String getName();

	/**
	 * Run the pass on the given configuration (stored in the global config object), modifying it in
	 * place. The command-line args are parsed before this is called
	 */
	protected abstract Configuration generateModel();
}
