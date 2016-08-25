package com.verivital.hyst.generators;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;

/**
 * Creates a (flat) hybrid automaton from scratch, based on the passed-in
 * arguments. This works with Hypy's ModelArgsBuilder object to aid python
 * creation of hybrid automaton models
 * 
 * @author Stanley Bak (August 2016)
 *
 */
public class BuildGenerator extends ModelGenerator
{
	@Option(name = "-vars", required = true, usage = "list of variables", metaVar = "VAR1 VAR2 ...")
	private ArrayList<String> vars;

	@Option(name = "-time_bound", required = true, usage = "reachability time bound", metaVar = "TIME")
	private double timeBound;

	@Option(name = "-init", required = true, usage = "initial mode names and conditions", metaVar = "(MODENAME CONDITION)+")
	private List<String> initParams;

	@Option(name = "-error", usage = "error mode names and conditions", metaVar = "(MODENAME CONDITION)+")
	private List<String> errorParams = null;

	@Option(name = "-modes", required = true, usage = "modes to create", metaVar = "(MODENAME INVARIANT DER1 DER2 ...)+")
	private List<String> modeParams;

	@Option(name = "-transitions", required = true, usage = "transitions to create", metaVar = "(FROM TO GUARD [RESET1 RESET2 ...])*")
	private List<String> transitionParams;

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
			throw new AutomatonExportException(
					"Mode params was of wrong size (should be divisble by " + div + ")");

		for (int i = 0; i < modeParams.size(); i += div)
		{
			String name = modeParams.get(i);
			String inv = modeParams.get(i + 1);

			AutomatonMode am = ha.createMode(name);
			am.invariant = FormulaParser.parseInvariant(inv);

			for (int v = 0; v < vars.size(); ++v)
			{
				String der = modeParams.get(i + 2 + v);
				am.flowDynamics.put(vars.get(v), new ExpressionInterval(der));
			}
		}

		// add transitions
		div = 3 + vars.size();

		if (transitionParams.size() % (div) != 0)
			throw new AutomatonExportException(
					"Transition params was of wrong size (should be divisble by " + div + ")");

		for (int i = 0; i < transitionParams.size(); i += div)
		{
			String from = transitionParams.get(i);
			String to = transitionParams.get(i + 1);
			String guard = transitionParams.get(i + 2);

			AutomatonTransition at = ha.createTransition(ha.modes.get(from), ha.modes.get(to));
			at.guard = FormulaParser.parseGuard(guard);

			for (int v = 0; v < vars.size(); ++v)
			{
				String reset = modeParams.get(i + 3 + v);

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
			throw new AutomatonExportException("Expected -init param count to be multiple of 2");

		for (int i = 0; i < initParams.size(); i += 2)
		{
			String mode = initParams.get(i);
			String condition = initParams.get(i + 1);

			if (c.init.containsKey(mode))
				throw new AutomatonExportException("initial mode was listed twice: " + mode);

			c.init.put(mode, FormulaParser.parseInitialForbidden(condition));
		}

		if (errorParams.size() % 2 != 0)
			throw new AutomatonExportException("Expected -error param count to be multiple of 2");

		for (int i = 0; i < errorParams.size(); i += 2)
		{
			String mode = errorParams.get(i);
			String condition = errorParams.get(i + 1);

			if (c.forbidden.containsKey(mode))
				throw new AutomatonExportException("error mode was listed twice: " + mode);

			c.forbidden.put(mode, FormulaParser.parseInitialForbidden(condition));
		}

		return c;
	}
}
