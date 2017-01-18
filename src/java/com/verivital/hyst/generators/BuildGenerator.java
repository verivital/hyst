package com.verivital.hyst.generators;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.util.CmdLineRuntimeException;
import com.verivital.hyst.util.StringPairsWithSpacesArrayOptionHandler;
import com.verivital.hyst.util.StringWithSpacesArrayOptionHandler;

/**
 * Creates a (flat) hybrid automaton from scratch, based on the passed-in arguments. This works with
 * Hypy's ModelArgsBuilder object to aid python creation of hybrid automaton models
 * 
 * @author Stanley Bak (August 2016)
 *
 */
public class BuildGenerator extends ModelGenerator
{
	@Option(name = "-vars", required = true, usage = "list of variables", metaVar = "VAR1 VAR2 ...", handler = StringArrayOptionHandler.class)
	private ArrayList<String> vars;

	@Option(name = "-time_bound", required = true, usage = "reachability time bound", metaVar = "TIME")
	private double timeBound;

	@Option(name = "-init", required = true, handler = StringPairsWithSpacesArrayOptionHandler.class, usage = "initial mode names and conditions", metaVar = "(MODENAME CONDITION)+")
	private List<String> initParams;

	@Option(name = "-error", handler = StringPairsWithSpacesArrayOptionHandler.class, usage = "error mode names and conditions", metaVar = "(MODENAME CONDITION)+")
	private List<String> errorParams = new ArrayList<String>();

	@Option(name = "-modes", required = true, handler = StringWithSpacesArrayOptionHandler.class, usage = "modes to create", metaVar = "(MODENAME INVARIANT DER1 DER2 ...)+")
	private List<String> modeParams;

	@Option(name = "-transitions", handler = StringWithSpacesArrayOptionHandler.class, usage = "transitions to create", metaVar = "(FROM TO GUARD [RESET1 RESET2 ...])*")
	private List<String> transitionParams = new ArrayList<String>();

	@Override
	public String getCommandLineFlag()
	{
		return "build";
	}

	@Override
	public String getName()
	{
		return "Build Flat Automaton Generator";
	}

	@Override
	protected Configuration generateModel()
	{
		BaseComponent ha = new BaseComponent();

		ha.variables = vars;

		int div = 2 + vars.size();

		if (modeParams.size() % (div) != 0)
			throw new CmdLineRuntimeException(
					"Mode params was of wrong size. Should be divisible by " + div + "; got "
							+ modeParams.size() + " params: " + modeParams);

		for (int i = 0; i < modeParams.size(); i += div)
		{
			String name = modeParams.get(i);
			String inv = modeParams.get(i + 1);

			AutomatonMode am = ha.createMode(name);
			am.invariant = FormulaParser.parseInvariant(inv);

			for (int v = 0; v < vars.size(); ++v)
			{
				String der = modeParams.get(i + 2 + v).trim();

				if (der.equals("null"))
					am.flowDynamics.remove(vars.get(v));
				else
				{
					ExpressionInterval ei = derStringToExpInterval(der);

					am.flowDynamics.put(vars.get(v), ei);
				}
			}
		}

		// add transitions
		div = 3 + vars.size();

		if (transitionParams.size() % (div) != 0)
			throw new CmdLineRuntimeException(
					"Transition params was of wrong size. Should be divisible by " + div + "; got "
							+ transitionParams.size() + " params: " + transitionParams);

		for (int i = 0; i < transitionParams.size(); i += div)
		{
			String from = transitionParams.get(i);
			String to = transitionParams.get(i + 1);
			String guard = transitionParams.get(i + 2);

			AutomatonTransition at = ha.createTransition(ha.modes.get(from), ha.modes.get(to));
			at.guard = FormulaParser.parseGuard(guard);

			for (int v = 0; v < vars.size(); ++v)
			{
				String reset = transitionParams.get(i + 3 + v);

				if (!reset.equals("null"))
					at.reset.put(vars.get(v), new ExpressionInterval(reset));
			}
		}

		Configuration c = new Configuration(ha);
		c.settings.spaceExConfig.timeHorizon = timeBound;

		c.settings.plotVariableNames[0] = ha.variables.get(0);
		c.settings.plotVariableNames[1] = ha.variables.size() > 1 ? ha.variables.get(1)
				: ha.variables.get(0);

		if (initParams.size() % 2 != 0)
			throw new CmdLineRuntimeException("Expected -init param count to be multiple of 2");

		for (int i = 0; i < initParams.size(); i += 2)
		{
			String mode = initParams.get(i);
			String condition = initParams.get(i + 1);

			if (c.init.containsKey(mode))
				throw new CmdLineRuntimeException("initial mode was listed twice: " + mode);

			c.init.put(mode, FormulaParser.parseInitialForbidden(condition));
		}

		if (errorParams.size() % 2 != 0)
			throw new CmdLineRuntimeException("Expected -error param count to be multiple of 2");

		for (int i = 0; i < errorParams.size(); i += 2)
		{
			String mode = errorParams.get(i);
			String condition = errorParams.get(i + 1);

			if (c.forbidden.containsKey(mode))
				throw new CmdLineRuntimeException("error mode was listed twice: " + mode);

			c.forbidden.put(mode, FormulaParser.parseInitialForbidden(condition));
		}

		return c;
	}

	private static ExpressionInterval derStringToExpInterval(String der)
	{
		ExpressionInterval rv = null;

		if (der.endsWith("]"))
		{
			String noSpaces = der.replace(" ", "");

			// nondeterministic flow
			int start = noSpaces.indexOf('[');

			if (start == -1)
				throw new AutomatonExportException("Malforming nondeterministic flow: " + der);

			// the character before der must be a '+', or the first
			// character
			String expressionStr = "0";

			if (start != 0)
			{
				if (noSpaces.charAt(start - 1) != '+')
					throw new AutomatonExportException(
							"Malforming nondeterministic flow; non-plus operation; must end in '+ [min,max]': "
									+ der);

				expressionStr = noSpaces.substring(0, start - 1);
			}

			String rangeStr = noSpaces.substring(start + 1, noSpaces.length() - 1);
			String[] parts = rangeStr.split(",");

			if (parts.length != 2)
				throw new AutomatonExportException(
						"Malforming nondeterministic flow; interval without two parts; must end in '+ [min,max]': "
								+ der);

			Interval interval = new Interval(Double.parseDouble(parts[0]),
					Double.parseDouble(parts[1]));

			rv = new ExpressionInterval(expressionStr, interval);
		}
		else
			rv = new ExpressionInterval(der);

		return rv;
	}
}
