/**
 * 
 */
package com.verivital.hyst.util;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.base.ExpressionInterval;



/**
 * classify components 
 * 
 * 
 *
 */
public class Classification 
{
    
    // TODO: use recursive
    public enum Dynamics{
        LINEAR, HYBRID, DISCRETE, NONLINEAR
    }
    public static Dynamics classifyFlow(ExpressionInterval ei){
        
        Dynamics rv = null;
        Expression e = ei.getExpression();
        
	if (e instanceof Operation)
		{
			Operation o = (Operation)e;
			Expression l = o.getLeft();
                        Expression r = o.getRight();
                        
                        if (l instanceof Variable && r instanceof Constant)
                        {
                               rv = Dynamics.LINEAR;
                        }
                        
			else if (l instanceof Variable && r instanceof Variable)
			{
                            if ( o.op == Operator.ADD | o.op == Operator.SUBTRACT)
                            {        
                                rv = Dynamics.LINEAR;                              
                            }
                            else 
                                rv = Dynamics.NONLINEAR;

			}		
		}

        return rv;
    }
            
}
