package com.verivital.hyst.passes.basic;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionModifier;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.Preconditions;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.ValueSubstituter;

/**
 * This pass substitutes constants in expressions for their values (assumes all
 * constants are not intervals).
 *
 */
public class SubstituteConstantsPass extends TransformationPass
{
	private ValueSubstituter vs = null;

	public SubstituteConstantsPass()
	{
		preconditions = new Preconditions(true); // no preconditions

		// except that constants can't be intervals
		preconditions.skip[PreconditionsFlag.CONVERT_INTERVAL_CONSTANTS.ordinal()] = false;
	}

	@Override
	protected void runPass()
	{
		runRec(config.root);
	}

	private void runRec(Component c)
	{
		if (c instanceof BaseComponent)
		{
			final BaseComponent ha = (BaseComponent) c;
			final Map<String, Interval> mapping = getConstMapping(ha);

			ExpressionModifier.modifyBaseComponent(ha, new ExpressionModifier()
			{
				@Override
				public Expression modifyExpression(Expression e)
				{
					return substituteConstantsIntoExpression(mapping, e);
				}
			});
		}
		else
		{
			NetworkComponent nc = (NetworkComponent) c;

			for (ComponentInstance ci : nc.children.values())
			{
				runRec(ci.child);
			}
		}
	}

	private static Map<String, Interval> getConstMapping(Component c)
	{
		Map<String, Interval> rv = new HashMap<String, Interval>();

		for (Entry<String, Interval> r : c.constants.entrySet())
		{
			String name = r.getKey();
			rv.put(name, c.getConstantValue(name));
		}

		return rv;
	}

	/**
	 * Substitute the constants into a single expression
	 * 
	 * @param ha
	 *            the automaton we're working with
	 * @param exp
	 *            the expression to substitute into
	 * @return the new expression with constants substituted in
	 */
	public Expression substituteConstantsIntoExpression(Map<String, Interval> constants,
			Expression exp)
	{
		if (vs == null)
		{
			HashMap<String, Expression> subMap = new HashMap<String, Expression>();

			for (Entry<String, Interval> e : constants.entrySet())
				subMap.put(e.getKey(), new Constant(e.getValue().asConstant()));

			vs = new ValueSubstituter(subMap);
		}

		return vs.substitute(exp);
	}

	@Override
	public String getCommandLineFlag()
	{
		return "-pass_sub_constants";
	}

	@Override
	public String getName()
	{
		return "Substitute Named Constants for Values Pass";
	}
}
