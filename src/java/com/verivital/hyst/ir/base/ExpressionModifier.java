package com.verivital.hyst.ir.base;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.Configuration;

public abstract class ExpressionModifier
{
	abstract protected Expression modifyExpression(Expression ei);

	private static LinkedHashMap<String, ExpressionInterval> modifyMap(
			LinkedHashMap<String, ExpressionInterval> m, ExpressionModifier em)
	{
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();

		for (Entry<String, ExpressionInterval> e : m.entrySet())
		{
			ExpressionInterval ei = e.getValue();

			if (ei != null)
				rv.put(e.getKey(), new ExpressionInterval(em.modifyExpression(ei.getExpression()),
						ei.getInterval()));
			else
				rv.put(e.getKey(), null);
		}

		return rv;
	}

	/**
	 * Modify expressions in the initial and forbidden states in this configuration
	 * 
	 * @param c
	 *            the configuration
	 * @param eim
	 *            the modification to perform
	 */
	public static void modifyInitForbidden(Configuration c, ExpressionModifier em)
	{
		LinkedHashMap<String, Expression> newInit = new LinkedHashMap<String, Expression>();

		for (Entry<String, Expression> e : c.init.entrySet())
		{
			Expression exp = e.getValue();
			Expression newExp = em.modifyExpression(exp);
			newInit.put(e.getKey(), newExp);
		}

		c.init = newInit;

		LinkedHashMap<String, Expression> newForbidden = new LinkedHashMap<String, Expression>();

		for (Entry<String, Expression> e : c.forbidden.entrySet())
			newForbidden.put(e.getKey(), em.modifyExpression(e.getValue()));

		c.forbidden = newForbidden;
	}

	/**
	 * Enumerate over all the expressions in this base component and modify them with the passed-in
	 * object
	 * 
	 * @param ha
	 *            the automaton to enumerate over
	 * @param em
	 *            the function that gets called to modify each expression
	 */
	public static void modifyBaseComponent(BaseComponent bc, ExpressionModifier em)
	{
		for (Entry<String, AutomatonMode> e : bc.modes.entrySet())
		{
			AutomatonMode m = e.getValue();

			if (m.flowDynamics != null)
				m.flowDynamics = modifyMap(m.flowDynamics, em);

			m.invariant = em.modifyExpression(m.invariant);
		}

		for (AutomatonTransition t : bc.transitions)
		{
			t.guard = em.modifyExpression(t.guard);
			t.reset = modifyMap(t.reset, em);
		}
	}
}