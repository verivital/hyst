package com.verivital.hyst.internalpasses;

import java.util.ArrayList;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.util.DynamicsUtil;

/**
 * Internal passes are similar to transformation passes, but instead are called programmatically.
 * They are like utility functions, but perform in-place modifications of a Configuration object. By
 * convention, call the static run() method to perform the transformation.
 * 
 * This pass converts a hybrid automaton to standard form. A standard form automaton is flat, and
 * has a single initial state named _init, which is urgent. It has a single forbidden state named
 * _error, with no condition on the real variables.
 * 
 * @author Stanley Bak
 */
public class ConvertToStandardForm
{
	// package visibility. For external access use
	// ConvertToStandardForm.getInitMode() and getErrorMode()
	static String INIT_MODE_NAME = "_init";
	static String ERROR_MODE_NAME = "_error";

	public static void run(Configuration config)
	{
		convertInit(config);

		convertForbidden(config);

		config.validate();
	}

	public static void convertForbidden(Configuration config)
	{
		BaseComponent ha = (BaseComponent) config.root;

		if (config.forbidden.size() != 1 || !config.forbidden.containsKey(ERROR_MODE_NAME))
			doConvertForbidden(config, ha);
		else
		{
			// sanity checks
			if (!config.forbidden.get(ERROR_MODE_NAME).equals(Constant.TRUE))
				throw new AutomatonExportException("Malformed Existing Standard Form Automaton. "
						+ ERROR_MODE_NAME + "'s expression must be Constant.True");
		}
	}

	public static void convertInit(Configuration config)
	{
		BaseComponent ha = (BaseComponent) config.root;

		if (config.init.size() != 1 || !config.init.containsKey(INIT_MODE_NAME))
			doConvertInit(config, ha);
		else
		{
			// sanity checks
			AutomatonMode init = ha.modes.get(INIT_MODE_NAME);

			if (!config.init.get(INIT_MODE_NAME).equals(Constant.TRUE))
				throw new AutomatonExportException("Malformed Existing Standard Form Automaton. "
						+ INIT_MODE_NAME + "'s expression must be Constant.True");

			if (!init.urgent)
				throw new AutomatonExportException("Malformed Existing Standard Form Automaton. "
						+ INIT_MODE_NAME + " must be urgent.");
		}
	}

	private static void doConvertInit(Configuration config, BaseComponent ha)
	{
		AutomatonMode init = ha.createMode(INIT_MODE_NAME);
		init.flowDynamics = null;
		init.urgent = true;
		init.invariant = Constant.TRUE;
		ArrayList<Expression> initExpressions = new ArrayList<Expression>();

		for (Entry<String, Expression> e : config.init.entrySet())
		{
			Expression initCondition = e.getValue();
			initExpressions.add(initCondition);
			AutomatonMode m = ha.modes.get(e.getKey());
			AutomatonTransition at = ha.createTransition(init, m);

			at.guard = initCondition;
		}

		config.init.clear();
		config.init.put(INIT_MODE_NAME, Constant.TRUE);
	}

	private static void doConvertForbidden(Configuration config, BaseComponent ha)
	{
		AutomatonMode error = ha.createMode(ERROR_MODE_NAME);
		error.invariant = Constant.TRUE;

		AutomatonMode someMode = ha.modes.values().iterator().next();

		for (String var : ha.variables)
		{
			if (someMode.flowDynamics.get(var) != null)
				error.flowDynamics.put(var, new ExpressionInterval(new Constant(0)));
			else
				error.flowDynamics.remove(var);
		}

		for (Entry<String, Expression> e : config.forbidden.entrySet())
		{
			Expression forbiddenCondition = e.getValue();
			ArrayList<Expression> conds = DynamicsUtil.splitDisjunction(forbiddenCondition);

			for (Expression cond : conds)
			{
				AutomatonMode m = ha.modes.get(e.getKey());
				AutomatonTransition at = ha.createTransition(m, error);

				at.guard = cond;
			}
		}

		config.forbidden.clear();
		config.forbidden.put(ERROR_MODE_NAME, Constant.TRUE);
	}

	public static AutomatonMode getInitMode(BaseComponent ha)
	{
		return ha.modes.get(INIT_MODE_NAME);
	}

	public static AutomatonMode getErrorMode(BaseComponent ha)
	{
		return ha.modes.get(ERROR_MODE_NAME);
	}
}
