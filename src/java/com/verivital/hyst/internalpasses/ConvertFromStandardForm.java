package com.verivital.hyst.internalpasses;

import java.util.ArrayList;
import java.util.List;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;

/**
 * Internal passes are similar to transformation passes, but instead are called programmatically.
 * They are like utility functions, but perform in-place modifications of a Configuration object. By
 * convention, call the static run() method to perform the transformation.
 * 
 * This pass converts a hybrid automaton from standard form back to normal. A standard form
 * automaton is flat, has a single initial state named _init, which is urgent. It has either a
 * single forbidden state named _error, with no condition on the real variables.
 * 
 * After running the conversion, _init and _error will be deleted, and the configuration's init and
 * forbidden variables will be populated.
 * 
 * @author Stanley Bak
 */
public class ConvertFromStandardForm
{
	// package visibility. For external access use
	// ConvertToStandardForm.getInitMode() and getErrorMode()
	static String INIT_MODE_NAME = ConvertToStandardForm.INIT_MODE_NAME;
	static String ERROR_MODE_NAME = ConvertToStandardForm.ERROR_MODE_NAME;

	public static void run(Configuration config)
	{
		convertInit(config);

		convertForbidden(config);
	}

	/**
	 * make all outgoing modes from _init into initial modes
	 * 
	 * @param config
	 * @param ha
	 */
	public static void convertInit(Configuration config)
	{
		BaseComponent ha = (BaseComponent) config.root;

		if (config.init.size() != 1 || !config.init.containsKey(INIT_MODE_NAME))
			throw new AutomatonExportException("Malformed Input Standard Form Automaton; expected"
					+ " single initial mode named " + INIT_MODE_NAME);

		// sanity checks
		AutomatonMode init = ha.modes.get(INIT_MODE_NAME);

		if (!config.init.get(INIT_MODE_NAME).equals(Constant.TRUE))
			throw new AutomatonExportException("Malformed Input Standard Form Automaton. "
					+ INIT_MODE_NAME + "'s expression must be Constant.True");

		if (!init.urgent)
			throw new AutomatonExportException("Malformed Input Standard Form Automaton. "
					+ INIT_MODE_NAME + " must be urgent.");

		List<AutomatonTransition> toRemove = new ArrayList<AutomatonTransition>();
		config.init.clear();

		for (AutomatonTransition at : ha.transitions)
		{
			if (at.from == init)
			{
				toRemove.add(at);

				if (at.reset.size() != 0)
					throw new AutomatonExportException("Malformed Input Standard Form Automaton. "
							+ "Transition from " + INIT_MODE_NAME + " contained a reset.");

				config.init.put(at.to.name, at.guard);
			}

			if (at.to == init)
				throw new AutomatonExportException("Malformed Input Standard Form Automaton. "
						+ "Transition into " + INIT_MODE_NAME + " not allowed.");
		}

		ha.transitions.removeAll(toRemove);
		ha.modes.remove(INIT_MODE_NAME);

		config.validate();
	}

	/**
	 * make all incoming modes to _error into error modes
	 * 
	 * @param config
	 * @param ha
	 */
	private static void convertForbidden(Configuration config)
	{
		if (config.forbidden.size() != 1 || !config.forbidden.containsKey(ERROR_MODE_NAME))
			throw new AutomatonExportException("Malformed Input Standard Form Automaton; expected"
					+ " single forbidden mode named " + ERROR_MODE_NAME);

		// sanity checks
		if (!config.forbidden.get(ERROR_MODE_NAME).equals(Constant.TRUE))
			throw new AutomatonExportException("Malformed Existing Standard Form Automaton. "
					+ ERROR_MODE_NAME + "'s expression must be Constant.True");

		// check if there is a transition from init -> error (in which case
		// conversion is not possible)
		if (config.init.containsKey(ERROR_MODE_NAME))
			throw new AutomatonExportException("Automaton contains has error as an initial"
					+ " state, which cannot be converted from standard form.");

		BaseComponent ha = (BaseComponent) config.root;
		AutomatonMode error = ha.modes.get(ERROR_MODE_NAME);
		List<AutomatonTransition> toRemove = new ArrayList<AutomatonTransition>();
		config.forbidden.clear();

		for (AutomatonTransition at : ha.transitions)
		{
			if (at.to == error)
			{
				toRemove.add(at);

				if (at.reset.size() != 0)
					throw new AutomatonExportException("Malformed Input Standard Form Automaton. "
							+ "Transition to " + ERROR_MODE_NAME + " contained a reset.");

				config.forbidden.put(at.from.name, at.guard);
			}

			if (at.from == error)
				throw new AutomatonExportException("Malformed Input Standard Form Automaton. "
						+ "Transition out of " + ERROR_MODE_NAME + " not allowed.");
		}

		ha.transitions.removeAll(toRemove);
		ha.modes.remove(ERROR_MODE_NAME);

		config.validate();
	}
}
