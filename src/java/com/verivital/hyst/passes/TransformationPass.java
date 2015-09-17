package com.verivital.hyst.passes;

import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.util.Preconditions;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

/**
 * A transformation pass will modify a hybrid automaton (Configuration object). It may have preconditions set,
 * for example, only working on flat automata. Define the pass behavior by defining the abstract method runPass().
 * The user can use the command line or GUI to manually select transformation passes. To do this, override the 
 * name / help / command-line flag methods, and add the pass to the list in Hyst.java. Otherwise, the pass will
 * only be accessible in code.
 */
public abstract class TransformationPass
{
	protected Preconditions preconditions = new Preconditions(false); // run all checks / conversions by default
	protected Configuration config = null; // set before runPass is called
	
	/**
	 * Run the pass on the given configuration, modifying it in place. This first checks preconditions, then runs
	 * the pass, then afterwards validates the configuration to make sure IR-assumptions remain valid.
	 * @param c the configuration to run the pass on
	 * @param params the pass parameters
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
			throw new AutomatonExportException("The preconditions for pass " + name +
					" were not met by the Hybrid Automaton model.\n" + e, e);
		}
		
		// convert and validate
		runVanillaPass(c, params);
	}
	
	protected void checkPreconditons(Configuration c, String name)
	{
		preconditions.check(c, name);
	}

	/**
	 * Run the pass on the given configuration, modifying it in place. This does NOT check preconditions, but does validate
	 * the model afterwards.
	 * 
	 * @param c the configuration to run the pass on
	 * @param params the pass parameters
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
			throw new AutomatonExportException("Hybrid Automaton IR structure was corrupted after running pass " + 
							this.getClass().getName(), e);
		}
	}
	
	/**
	 * Get the flag used to run this pass on the command line. If null (the default), this pass cannnot
	 * be used as a command-line pass
	 * @return null or a command-line flag used to run this pass
	 */
	public String getCommandLineFlag()
	{
		return null;
	}
	
	/**
	 * Get the name of this pass. Can be null if the pass is not a command-line pass.
	 * @return the name text
	 */
	public String getName()
	{
		return null;
	}
	
	/**
	 * Get the help text for the parameter (if any). Can be null if parameter is ignored
	 * @return the parameter help
	 */
	public String getParamHelp()
	{
		return null;
	}
	
	/**
	 * Get the longer version of the help text for this pass. Can be null.
	 * @return the help text, or null if not specified
	 */
	public String getLongHelp()
	{
		return null;
	}
	
	/**
	 * Run the pass on the given configuration (stored in the global config object), modifying it in place
	 * @param params the parameter string given to the pass
	 */
	protected abstract void runPass(String params);
}
