package com.verivital.hyst.passes.basic;

import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.TransformationPass;


// Adds identity resets to resets which don't have them
public class AddIdentityResetPass extends TransformationPass
{
	@Override
	protected void runPass(String params)
	{
		BaseComponent ha = (BaseComponent)config.root;
		
		for (AutomatonTransition t : ha.transitions)
		{
			for (String v : ha.variables)
			{
				if (!t.reset.containsKey(v))
					t.reset.put(v, new ExpressionInterval(new Variable(v)));
			}
		}
	}
	
	@Override
	public String getCommandLineFlag()
	{
		return "-pass_identity";
	}
	
	@Override
	public String getName()
	{
		return "Add Identity Resets Pass";
	}
	
	public String getParamHelp()
	{
		return null;
	}
}
