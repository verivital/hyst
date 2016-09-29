package com.verivital.hyst.passes.basic;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
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
 * This pass substitutes constants in expressions for their values (assumes all constants are not
 * intervals).
 *
 */
public class SubstituteConstantsPass extends TransformationPass
{
	public SubstituteConstantsPass()
	{
		preconditions = new Preconditions(true); // no preconditions

		// except that constants can't be intervals
		preconditions.skip[PreconditionsFlag.CONVERT_INTERVAL_CONST_TO_VAR.ordinal()] = false;
	}

	@Override
	protected void runPass()
	{
		final Map<String, Interval> rootMapping = getConstMapping(config.root);
		// modify init and forbidden

		ExpressionModifier.modifyInitForbidden(config, new ExpressionModifier()
		{
			@Override
			public Expression modifyExpression(Expression e)
			{
				return substituteConstantsIntoExpression(rootMapping, e);
			}
		});

		removeRedundantConstaints(config.init);

		if (config.forbidden != null)
			removeRedundantConstaints(config.forbidden);

		runRec(config.root);
	}

	private void removeRedundantConstaints(LinkedHashMap<String, Expression> init)
	{
		// remove constraints like 5 == 5->
		for (Entry<String, Expression> e : init.entrySet())
		{
			Expression simpler = SimplifyExpressionsPass.simplifyExpression(e.getValue());
			e.setValue(simpler);
		}
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

			ha.constants.clear();
		}
		else
		{
			NetworkComponent nc = (NetworkComponent) c;

			for (ComponentInstance ci : nc.children.values())
			{
				runRec(ci.child);
			}

			nc.constants.clear();

			for (ComponentInstance ci : nc.children.values())
			{
				ci.constMapping.clear();
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
	public static Expression substituteConstantsIntoExpression(Map<String, Interval> constants,
			Expression exp)
	{
		HashMap<String, Expression> subMap = new HashMap<String, Expression>();

		for (Entry<String, Interval> e : constants.entrySet())
		{
			try
			{
				subMap.put(e.getKey(), new Constant(e.getValue().asConstant()));
			}
			catch (AutomatonExportException ex)
			{
				throw new AutomatonExportException("Error creating constant from variable '"
						+ e.getKey() + "' = " + e.getValue());
			}
		}

		ValueSubstituter vs = new ValueSubstituter(subMap);

		return vs.substitute(exp);
	}

	@Override
	public String getCommandLineFlag()
	{
		return "sub_constants";
	}

	@Override
	public String getName()
	{
		return "Substitute Named Constants for Values Pass";
	}
}
