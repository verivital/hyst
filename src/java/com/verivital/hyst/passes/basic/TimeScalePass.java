package com.verivital.hyst.passes.basic;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.TransformationPass;

/**
 * A model transformation pass which re-scales time
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class TimeScalePass extends TransformationPass
{
	@Option(name = "-scale", required = true, usage = "multiplier for each derivative", metaVar = "NUM")
	private double scale;

	@Option(name = "-ignorevar", required = false, usage = "variable to ignore", metaVar = "VARNAME")
	private String ignoreVar;

	@Override
	public String getName()
	{
		return "Scale Time Pass";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "scale_time";
	}

	@Override
	protected void runPass()
	{
		BaseComponent ha = (BaseComponent) config.root;

		// multiply all derivatives by the scale, then divide the reachtime by
		// the scale
		if (scale <= 0)
			throw new AutomatonExportException("Rescale factor must be positive: " + scale);

		for (AutomatonMode am : ha.modes.values())
			am.flowDynamics = rescaleFlow(am.flowDynamics);

		config.settings.spaceExConfig.timeHorizon /= scale;
		config.settings.spaceExConfig.samplingTime /= scale;
	}

	/**
	 * Rescale the flow
	 * 
	 * @param scale
	 *            the scale the use
	 * @param flows
	 *            the mode.flowDynamics to rescale
	 * @param ignoreVar
	 *            the variables to not rescale (to ignore)
	 */
	private LinkedHashMap<String, ExpressionInterval> rescaleFlow(
			LinkedHashMap<String, ExpressionInterval> flows)
	{
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();

		for (Entry<String, ExpressionInterval> e : flows.entrySet())
		{
			String var = e.getKey();

			if (var.equals(ignoreVar))
				rv.put(var, e.getValue());
			else
			{
				ExpressionInterval ei = e.getValue().copy();
				Operation o = new Operation(ei.getExpression(), Operator.MULTIPLY,
						new Constant(scale));
				ei.setExpression(o);

				rv.put(var, ei);
			}
		}

		return rv;
	}
}
