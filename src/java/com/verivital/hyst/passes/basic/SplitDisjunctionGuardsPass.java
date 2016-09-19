package com.verivital.hyst.passes.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;

/**
 * This pass splits guards with disjunctions into multiple transitions.
 * 
 * For example, A -- (x == 1 || x == 2) --> B would be split into two transitions from A to B, one
 * with (x==1) and one with (x == 2)
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class SplitDisjunctionGuardsPass extends TransformationPass
{
	@Override
	public String getCommandLineFlag()
	{
		return "split_disjunctions";
	}

	@Override
	public String getName()
	{
		return "Split Guards with Disjunctions";
	}

	@Override
	protected void runPass()
	{
		split(config.root);
	}

	public static void split(Component root)
	{
		splitRecursive(root);
	}

	private static void splitRecursive(Component c)
	{
		if (c instanceof BaseComponent)
		{
			// base case
			BaseComponent ha = (BaseComponent) c;
			List<AutomatonTransition> originalTransitions = new ArrayList<AutomatonTransition>(
					ha.transitions);

			for (AutomatonTransition t : originalTransitions)
			{
				Collection<Expression> conditions = splitExpression(t.guard);

				if (conditions.size() > 1)
				{
					Hyst.log("Splitting disjunctive guard '" + t.guard.toDefaultString()
							+ "' in automaton " + ha.instanceName);

					// remove the old one
					ha.transitions.remove(t);

					// add the new ones
					for (Expression subCondition : conditions)
					{
						AutomatonTransition newT = t.copy(ha);
						newT.guard = subCondition;
					}
				}
			}
		}
		else
		{
			// recursive case
			NetworkComponent nc = (NetworkComponent) c;

			for (ComponentInstance ci : nc.children.values())
				splitRecursive(ci.child);
		}
	}

	/**
	 * split a disjunctive expression into several sub expressions
	 * 
	 * @param reset
	 * @return
	 */
	private static Collection<Expression> splitExpression(Expression e)
	{
		Collection<Expression> rv = new LinkedList<Expression>();

		// currently just split top-level disjunctions
		if (e instanceof Operation && ((Operation) e).op == Operator.OR)
		{
			Operation o = (Operation) e;

			rv.addAll(splitExpression(o.getLeft()));
			rv.addAll(splitExpression(o.getRight()));
		}
		else // not a disjunction
			rv.add(e);

		return rv;
	}
}
