package com.verivital.hyst.util;

import java.util.Map;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Variable;

public class ValueSubstituter
{
	private Map<String, Expression> valMap;
	
	public ValueSubstituter(Map<String, Expression> substitutionMap)
	{
		this.valMap = substitutionMap;
	}
	
	/**
	 * Do the Substitution return the new expression
	 * @param e the expression to substitute into
	 * @return the modified expression
	 */
	public Expression substitute(Expression e)
	{
		Expression rv = null;
		
		if (e != null)
			rv = substituteCopy(e.copy());
		
		return rv;
	}
	
	private Expression substituteCopy(Expression e)
	{
		Expression rv = e;
		
		if (e instanceof Variable)
		{
			Variable v = (Variable)e;
			Expression subIn = valMap.get(v.name);
			
			if (subIn != null)
				rv = subIn.copy();
		}
		else if (e instanceof Operation)
		{
			Operation o = (Operation)e;
			
			for (int i = 0; i < o.children.size(); ++i)
			{
				Expression child = o.children.get(i);
				
				o.children.set(i, substitute(child));
			}
		}
		
		return rv;
	}
};
