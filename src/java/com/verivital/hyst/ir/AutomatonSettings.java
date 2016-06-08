package com.verivital.hyst.ir;

import java.util.Collection;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExConfigValues;

/**
 * Settings extracted from the model.
 *
 * automaton is guaranteed nonnull plotVariableNames is guaranteed nonnull, and of size 2, although
 * the elements may be null (if the model contains no variables, for example)
 * 
 * @author Stanley Bak (stanleybak@gmail.com)
 *
 */
public class AutomatonSettings
{
	public Configuration config;

	// values imported from the SpaceEx .cfg file (nonnnull)
	public SpaceExConfigValues spaceExConfig = new SpaceExConfigValues();

	// elements may be null if variables don't exist
	// if automaton contains a variable, then both elements are nonnull
	public String[] plotVariableNames = new String[2];

	@Override
	public String toString()
	{
		return "[AutomatonSettings plotVariableNames = " + plotVariableNames[0] + ", "
				+ plotVariableNames[1] + "]";
	}

	public AutomatonSettings(Configuration c)
	{
		config = c;
	}

	public void validate()
	{
		if (!Configuration.DO_VALIDATION)
			return;

		if (config == null)
			throw new AutomatonValidationException("config was null");

		if (plotVariableNames == null)
			throw new AutomatonValidationException("plotVariableNames was null");

		if (spaceExConfig == null)
			throw new AutomatonValidationException("spaceExConfig was null");

		if (plotVariableNames.length != 2)
			throw new AutomatonValidationException("plotVariableNames was not of length 2");

		for (int i = 0; i < plotVariableNames.length; ++i)
		{
			if (plotVariableNames[i] != null
					&& !config.root.getAllVariables().contains(plotVariableNames[i]))
				throw new AutomatonValidationException(
						"plot variable not in automaton: " + plotVariableNames[i]);
		}

		Collection<String> vars = config.root.getAllVariables();
		if (vars.size() > 0 && (plotVariableNames[0] == null || plotVariableNames[1] == null))
			throw new AutomatonValidationException("plotVariableNames elements were null, "
					+ "but automaton contains continuous variables.: " + vars);
	}

	public AutomatonSettings copy(Configuration newParent)
	{
		AutomatonSettings rv = new AutomatonSettings(newParent);
		rv.spaceExConfig = spaceExConfig.copy();

		if (plotVariableNames != null)
		{
			rv.plotVariableNames = new String[plotVariableNames.length];

			for (int i = 0; i < plotVariableNames.length; ++i)
				rv.plotVariableNames[i] = plotVariableNames[i];
		}

		return rv;
	}
}
