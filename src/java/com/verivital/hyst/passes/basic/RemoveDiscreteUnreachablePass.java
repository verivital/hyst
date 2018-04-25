package com.verivital.hyst.passes.basic;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.Preconditions;
import com.verivital.hyst.util.PreconditionsFlag;

/**
 * This pass performs discrete reachability and eliminates unreachable locations and transitions
 * 
 * @author Stanley Bak (Jan 2015)
 *
 */
public class RemoveDiscreteUnreachablePass extends TransformationPass
{
	public RemoveDiscreteUnreachablePass()
	{
		// skip all checks
		preconditions = new Preconditions(true);

		// except require that it's flat
		preconditions.skip[PreconditionsFlag.CONVERT_TO_FLAT_AUTOMATON.ordinal()] = false;
	}

	@Override
	protected void runPass()
	{
		BaseComponent ha = (BaseComponent) config.root;

		Set<String> reachable = constructReacahbleStates(ha);

		// remove unreachable modes
		for (Iterator<Entry<String, AutomatonMode>> i = ha.modes.entrySet().iterator(); i
				.hasNext();)
		{
			Entry<String, AutomatonMode> e = i.next();

			if (!reachable.contains(e.getKey()))
				i.remove();
		}

		// remove unreachable transitions
		for (Iterator<AutomatonTransition> i = ha.transitions.iterator(); i.hasNext();)
		{
			AutomatonTransition t = i.next();

			if (!(reachable.contains(t.from.name) && reachable.contains(t.to.name)))
				i.remove();
		}

		// remove unreachable initial states
		for (Iterator<Entry<String, Expression>> i = config.init.entrySet().iterator(); i
				.hasNext();)
		{
			Entry<String, Expression> e = i.next();

			if (!reachable.contains(e.getKey()))
				i.remove();
		}

		// remove unreachable final states
		for (Iterator<Entry<String, Expression>> i = config.forbidden.entrySet().iterator(); i
				.hasNext();)
		{
			Entry<String, Expression> e = i.next();

			if (!reachable.contains(e.getKey()))
				i.remove();
		}
	}

	private Set<String> constructReacahbleStates(BaseComponent ha)
	{
		HashSet<String> reachable = new HashSet<String>();
		HashSet<String> next = new HashSet<String>();

		for (String s : config.init.keySet())
		{
			reachable.add(s);

			for (String successor : successorsOf(s, ha))
			{
				if (!reachable.contains(successor))
					next.add(successor);
			}
		}

		while (!next.isEmpty())
		{
			String s = next.iterator().next();

			reachable.add(s);

			for (String successor : successorsOf(s, ha))
			{
				if (!reachable.contains(successor))
					next.add(successor);
			}

			next.remove(s);
		}

		return reachable;
	}

	/**
	 * Get the successor states of s in ha
	 * 
	 * @param s
	 * @param ha
	 * @return the set of sucessors
	 */
	private Set<String> successorsOf(String s, BaseComponent ha)
	{
		HashSet<String> rv = new HashSet<String>();

		for (AutomatonTransition t : ha.transitions)
		{
			if (t.from.name.equals(s))
				rv.add(t.to.name);
		}

		return rv;
	}

	@Override
	public String getCommandLineFlag()
	{
		return "remove_unreachable";
	}

	@Override
	public String getName()
	{
		return "Remove Discrete Unreachable States Pass";
	}
}
