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
 * For example, A -- (x == 1 || x == 2) --> B would be split into two transitions
 * from A to B, one with (x==1) and one with (x == 2)
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class SplitDisjunctionGuardsPass extends TransformationPass
{
	private boolean print = false;
	public static final String PRINT_PARAM = "print";

	@Override
	protected void runPass(String params)
	{
		print = params.equals(PRINT_PARAM);
		
		splitRecrusive(config.root);
	}

	private void splitRecrusive(Component c)
	{
		if (c instanceof BaseComponent)
		{
			// base case
			BaseComponent ha = (BaseComponent)c;
			List <AutomatonTransition> originalTransitions = new ArrayList <AutomatonTransition>(ha.transitions);
			
			for (AutomatonTransition t : originalTransitions)
			{
				Collection <Expression> conditions = splitExpression(t.guard);
				
				if (conditions.size() > 1)
				{
					if (print)
						Hyst.log("Splitting disjunctive guard '" + t.guard.toDefaultString() + "' in automaton " + ha.instanceName);
					
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
			NetworkComponent nc = (NetworkComponent)c;
			
			for (ComponentInstance ci : nc.children.values())
				splitRecrusive(ci.child);
		}
	}

	/**
	 * split a disjunctive expression into several sub expressions
	 * @param reset
	 * @return
	 */
	private Collection<Expression> splitExpression(Expression e)
	{
		Collection<Expression> rv = new LinkedList <Expression>();
		
		// currently just split top-level disjunctions
		if (e instanceof Operation && ((Operation)e).op == Operator.OR)
		{
			Operation o = (Operation)e;
			
			rv.addAll(splitExpression(o.getLeft()));
			rv.addAll(splitExpression(o.getRight()));
		}
		else // not a disjunction
			rv.add(e);
		
		return rv;
	}

	@Override
	public String getCommandLineFlag()
	{
		return "-pass_split_disjunctions (" + PRINT_PARAM + ")";
	}
	
	/**
	 * Get the help text for the parameter (if any). Can be null if parameter is ignored
	 * @return the parameter help
	 */
	public String getParamHelp()
	{
		return "if the param '" + PRINT_PARAM + "' is given, any disjunctions that are split will be printed.";
	}
	
	@Override
	public String getName()
	{
		return "Split Guards with Disjunctions";
	}
}
