package com.verivital.hyst.passes.complex.hybridize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.python.PythonUtil;
import com.verivital.hyst.util.AutomatonUtil;

public class AffineOptimize
{
	public static class OptimizationParams
	{
		// set these two as input (original dynamics, bounds)
		public LinkedHashMap<String, ExpressionInterval> original = new LinkedHashMap<String, ExpressionInterval>(); 
		public HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		
		// this is set as output
		public LinkedHashMap<String, ExpressionInterval> result = new LinkedHashMap<String, ExpressionInterval>();

		// this is used internally
		private LinkedHashMap<String, Expression> linearized = new LinkedHashMap<String, Expression>();
	}
	
	/**
	 * Create affine dynamics which encompass the original dynamics in some rectangle
	 * This function is called on several dynamics and several rectangles
	 * 
	 * @param params [in/out] the list of OptimizationParams to optimize. Result is stored here
	 */
	public static void createAffineDynamics(List<OptimizationParams> params)
	{
		if (params.size() == 0)
			throw new AutomatonExportException("createAffineDynamics was called with params list of length 0");
		
		ArrayList <Expression> expList = new ArrayList <Expression>();
		ArrayList<HashMap<String, Interval>> boundsList = new ArrayList<HashMap<String, Interval>>();
		
		createOptimizationParams(params, expList, boundsList);
		
		List<Interval> result = PythonUtil.scipyOptimize(expList, boundsList);
		
		createOptimizationResult(params, result);
	}

	/**
	 * Populate the result data structures after the optimization was performed
	 * @param params the param list where the results will be stored
	 * @param result the optimization result intervals
	 */
	private static void createOptimizationResult(List<OptimizationParams> params, List<Interval> result)
	{
		int curParam = 0;
		int curVar = 0;
		
		ArrayList <String> orderedVariables = new ArrayList <String>(); // same ordering as the flow hashmap		
		orderedVariables.addAll(params.get(0).original.keySet());
		int NUM_VARS = orderedVariables.size();
		
		for (Interval inter : result)
		{
			String var = orderedVariables.get(curVar);
			OptimizationParams op = params.get(curParam);
			Expression linearized = op.linearized.get(var);
			
			// make the min interval 0, so that we get something like: x + y + 4 + [0, 0.1]
			double val = inter.min;
			linearized = new Operation(Operator.ADD, linearized, new Constant(val));
			linearized = SimplifyExpressionsPass.simplifyExpression(linearized);
			
			inter.min -= val;
			inter.max -= val;
			
			// if the interval is zero, don't include it as an interval
			double TOL = 0; //1e-9;
			
			if (Math.abs(inter.max) <= TOL)
				op.result.put(var, new ExpressionInterval(linearized));
			else
			
				op.result.put(var, new ExpressionInterval(linearized, inter));
			
			// increment
			if (++curVar == NUM_VARS)
			{
				curVar = 0;
				++curParam;
			}
		}
	}

	/**
	 * Create the parameters for optimization of affine dynamics
	 * @param params the passed-in parameters
	 * @param expList [out] the expressions to optimize
	 * @param boundsList [out] the bounds where to optimize
	 */
	private static void createOptimizationParams(
			List<OptimizationParams> params, ArrayList<Expression> expList,
			ArrayList<HashMap<String, Interval>> boundsList)
	{
		ArrayList <String> orderedVariables = new ArrayList <String>(); // same ordering as the flow hashmap		
		orderedVariables.addAll(params.get(0).original.keySet());
		int NUM_VARS = orderedVariables.size();
		
		for (OptimizationParams op : params)
		{
			LinkedHashMap<String, ExpressionInterval> original = op.original;
			HashMap<String, Interval> bounds = op.bounds;
			
			double[][] JAC = AutomatonUtil.estimateJacobian(original, bounds);
	
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
				
				op.linearized.put(derVarName, linearized);
				
				// the function to be optimized is the difference between the linear approximation and the real function
				Expression optimizeFunc = new Operation(Operator.SUBTRACT, derivativeExp, linearized);
				expList.add(optimizeFunc);
				boundsList.add(bounds);
			}
		}
	}
}
