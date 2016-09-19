package com.verivital.hyst.passes;

import java.io.ByteArrayOutputStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.CmdLineRuntimeException;
import com.verivital.hyst.util.Preconditions;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

/**
 * A transformation pass is a behavior which modifies a Configuration (Hybrid Automaton) object.
 * Transformation passes can be called from the command line and accept params. When your runPass()
 * method gets called, the pass will have done automatic command-line parsing using the args4j
 * library. To use parameters, all you need to do is define the annotations for member variables.
 * 
 * See TimeScalePass for a simple example of these annotations.
 * 
 * @author Stanley Bak
 *
 */
public abstract class TransformationPass
{
	protected Preconditions preconditions = new Preconditions(false); // run all
																		// checks
																		// /
																		// conversions
																		// by
																		// default
	protected Configuration config = null; // is assigned before runPass is
											// called
	private CmdLineParser parser = new CmdLineParser(this);

	public TransformationPass()
	{
		String flag = getCommandLineFlag();

		if (flag.startsWith("-"))
			throw new RuntimeException(
					"transformation pass command-line flag shouldn't start with a hyphen: " + flag);
	}

	/**
	 * Run the pass on the given configuration, modifying it in place. This first checks
	 * preconditions, then runs the pass, then afterwards validates the configuration to make sure
	 * IR-assumptions remain valid.
	 * 
	 * @param c
	 *            the configuration to run the pass on
	 * @param params
	 *            the pass parameters
	 */
	public void runTransformationPass(Configuration c, String params)
	{
		// check preconditions
		String name = this.getClass().getName();

		try
		{
			checkPreconditons(c, name);
		}
		catch (PreconditionsFailedException e)
		{
			throw new AutomatonExportException("The preconditions for pass " + name
					+ " were not met by the Hybrid Automaton model.\n" + e, e);
		}

		// convert and validate
		runVanillaPass(c, params);
	}

	/**
	 * Check the preconditions, throwing a PreconditionsException or converting the automaton if
	 * they're not met
	 * 
	 * @param c
	 *            the Configuration object to check / convert
	 * @param name
	 *            the name of the pass, for error reporting
	 * 
	 * @throws PreconditionsFailedException
	 *             if the automaton does not match the assumptions and cannot be converted
	 */
	protected void checkPreconditons(Configuration c, String name)
	{
		preconditions.check(c, name);
	}

	/**
	 * Run the pass on the given configuration, modifying it in place. This does NOT check
	 * preconditions, but does validate the model afterwards.
	 * 
	 * @param c
	 *            the configuration to run the pass on
	 * @param params
	 *            the pass parameters
	 */
	public void runVanillaPass(Configuration c, String params)
	{
		config = c;
		runPass(params);

		// validate modified configuration
		try
		{
			c.validate();
		}
		catch (AutomatonValidationException e)
		{
			throw new AutomatonExportException(
					"Hybrid Automaton IR structure was corrupted after running pass "
							+ this.getClass().getName(),
					e);
		}
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

	private void runPass(String params)
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
			String message = "Error Parsing " + getName() + ",\n Message: " + e.getMessage()
					+ "\nArguments: '" + params + "'\n" + getParamHelp();

			throw new CmdLineRuntimeException(message, e);
		}

		runPass();
	}

	/**
	 * Get the flag used to run this pass on the command line. If null (the default), this pass
	 * cannnot be used as a command-line pass
	 * 
	 * @return null or a command-line flag used to run this pass
	 */
	public abstract String getCommandLineFlag();

	/**
	 * Get the name of this pass.
	 * 
	 * @return the name text
	 */
	public abstract String getName();

	/**
	 * Run the pass on the given configuration (stored in the global config object), modifying it in
	 * place. The command-line args are parsed before this is called
	 */
	protected abstract void runPass();
}
