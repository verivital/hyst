package com.verivital.hyst.passes.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

/**
 * This pass removes modes and associated transitions from the automata which have invariants that
 * are unsatisfiable.
 * 
 * @author Stanley Bak (Feb 2015)
 *
 */
public class RemoveSimpleUnsatInvariantsPass extends TransformationPass
{
	@Override
	protected void runPass()
	{
		BaseComponent ha = (BaseComponent) config.root;
		ArrayList<AutomatonMode> toRemove = new ArrayList<AutomatonMode>();

		for (AutomatonMode am : ha.modes.values())
		{
			if (isUnsat(am.invariant))
				toRemove.add(am);
		}

		removeModes(ha, toRemove);
	}

	/**
	 * Check if the given expression is unsatisfiable using simple range checks on all the
	 * variables.
	 * 
	 * @param e
	 *            the expression to check
	 * @return true if the expression is provably unsatisfiable
	 */
	private static boolean isUnsat(Expression e)
	{
		boolean rv = false;

		Collection<String> vars = AutomatonUtil.getVariablesInExpression(e);

		for (String v : vars)
		{
			try
			{
				RangeExtractor.getVariableRange(e, v);
			}
			catch (EmptyRangeException ex)
			{
				rv = true;
				break;
			}
			catch (ConstantMismatchException ex)
			{
				rv = true;
				break;
			}
			catch (UnsupportedConditionException ex)
			{
				// not provably unsatisfiable, do nothing
			}
		}

		return rv;
	}

	private void removeModes(BaseComponent ha, ArrayList<AutomatonMode> toRemove)
	{
		for (AutomatonMode am : toRemove)
		{
			config.init.remove(am.name);

			ha.modes.remove(am.name);
		}

		for (Iterator<AutomatonTransition> i = ha.transitions.iterator(); i
				.hasNext(); /* increment in loop */)
		{
			AutomatonTransition at = i.next();

			if (toRemove.contains(at.from) || toRemove.contains(at.to))
				i.remove();
		}
	}

	@Override
	public String getCommandLineFlag()
	{
		return "remove_unsat";
	}

	@Override
	public String getName()
	{
		return "Remove Unsatisfiable Modes Pass";
	}
}
