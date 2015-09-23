package com.verivital.hyst.passes.complex.hybridize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.python.PythonUtil;
import com.verivital.hyst.util.AutomatonUtil;

public class AffineOptimize
{
	public static int numOptimizations = 0;
	
	/**
	 * Create affine dynamics which encompass the original dynamics in some rectangle
	 * @param pb the PythonBridge to use
	 * @param original the original dynamics
	 * @param bounds the rectangle bounds
	 * @return the constructed affine flows
	 */
	public static LinkedHashMap<String, ExpressionInterval> createAffineDynamics( PythonBridge pb,
			LinkedHashMap<String, ExpressionInterval> original, HashMap<String, Interval> bounds)
	{
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();
		int NUM_VARS = original.size();
		double[][] JAC = AutomatonUtil.estimateJacobian(original, bounds);
		ArrayList <String> orderedVariables = new ArrayList <String>(); // same ordering as the flow hashmap		
		orderedVariables.addAll(original.keySet());

		for (int derVar = 0; derVar < NUM_VARS; ++derVar)
		{
			String derVarName = orderedVariables.get(derVar);
			Expression derivativeExp = original.get(derVarName).asExpression();
			
			// linear estimate is: JAC[derVar][0] * var0 + JAC[derVar][1] * var1 + ...
			Expression linearized = null;
			
			for (int partialVar = 0; partialVar < NUM_VARS; ++partialVar)
			{
				if (JAC[derVar][partialVar] == 0)
					continue;
				
				Operation term = new Operation(Operator.MULTIPLY, new Constant(JAC[derVar][partialVar]),
						new Variable(orderedVariables.get(partialVar)));
				
				if (linearized == null)
					linearized = term;
				else
					linearized = new Operation(Operator.ADD, linearized, term);
			}
			
			// if jacobian was zero for all directions
			if (linearized == null)
				linearized = new Constant(0);
			
			// the function to be optimized is the difference between the linear approximation and the real function
			Expression optimizeFunc = new Operation(Operator.SUBTRACT, derivativeExp, linearized);
			
			Interval inter = PythonUtil.scipyOptimize(pb, optimizeFunc, bounds);
			++numOptimizations;
			
			// make the min interval 0, so that we get something like: x + y + 4 + [0, 0.1]
			double val = inter.min;
			linearized = new Operation(Operator.ADD, linearized, new Constant(val));
			linearized = SimplifyExpressionsPass.simplifyExpression(linearized);
			
			inter.min -= val;
			inter.max -= val;
			
			// if the interval is zero, don't include it as an interval
			double TOL = 1e-9;
			
			if (Math.abs(inter.max) < TOL)
				rv.put(derVarName, new ExpressionInterval(linearized));
			else
				rv.put(derVarName, new ExpressionInterval(linearized, inter));
		}
		
		return rv;
	}
}
