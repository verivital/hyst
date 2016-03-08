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
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.python.PythonUtil;
import com.verivital.hyst.util.AutomatonUtil;

public class AffineOptimize
{	
	public static class OptimizationParams
	{
		// set these two as input (original dynamics, bounds)
		public LinkedHashMap<String, ExpressionInterval> toOptimize = new LinkedHashMap<String, ExpressionInterval>(); 
		public HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		
		// this is set as output
		public LinkedHashMap<String, ExpressionInterval> result = new LinkedHashMap<String, ExpressionInterval>();
	}
	
	/**
	 * Perform an optimization in order to find out the differences in dynamics, for example,
	 * between a nonlinear derivative and its linear approximation.
	 * 
	 * @param optimizationType one of {"basinhopping", "interval", "intervalXXX" where XXX is a 
	 * real number describing the maximum overapproximation error
	 * @param params [in/out] the list of OptimizationParams to optimize. Result is stored here
	 */
	public static void optimizeDynamics(String optimizationType, 
			List<OptimizationParams> params)
	{
		if (params.size() == 0)
			throw new AutomatonExportException("createAffineDynamics was called with params list of length 0");
		
		PythonBridge.getInstance(PythonBridge.NO_TIMEOUT); // turn off timeout
		
		ArrayList <Expression> expList = new ArrayList <Expression>();
		ArrayList<HashMap<String, Interval>> boundsList = new ArrayList<HashMap<String, Interval>>();
		
		createOptimizationParams(params, expList, boundsList);
		
		Hyst.logDebug("Created optimization params; expList=" + expList); 
		
		List<Interval> optimizationResult;
		
		if (optimizationType.equals("basinhopping"))
			optimizationResult = PythonUtil.scipyOptimize(expList, boundsList);
		else if (optimizationType.equals("interval"))
			optimizationResult = PythonUtil.intervalOptimize(expList, boundsList);
		else if (optimizationType.startsWith("interval"))
		{
			String num = optimizationType.substring("interval".length());
			
			try
			{
				double accuracy = Double.parseDouble(num);
				
				if (accuracy <= 0)
					throw new AutomatonExportException("malformed interval optimization param: " + accuracy);
				
				optimizationResult = PythonUtil.intervalOptimizeBounded(expList, boundsList, accuracy);
			}
			catch (NumberFormatException e)
			{
				throw new AutomatonExportException("malformed interval optimization param", e);
			}
		}
		else
			throw new AutomatonExportException("Unsupported Optimization Method: " + optimizationType);
		
		// output stored in params.result
		createOptimizationResult(params, optimizationResult);
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
		orderedVariables.addAll(params.get(0).toOptimize.keySet());
		int NUM_VARS = orderedVariables.size();
		
		for (Interval inter : result)
		{
			String var = orderedVariables.get(curVar);
			OptimizationParams op = params.get(curParam);
			Expression linearized = op.linearized.get(var);
			
			// make the min interval 0, so that we get something like: x + y + 4 + [0, 0.1]
			double val = inter.min;
			linearized = new Operation(Operator.ADD, linearized, new Constant(val));
			linearized = PythonUtil.pythonSimplifyExpression(linearized);
			
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
	 * Create the parameters for optimization 
	 * @param params the passed-in parameters
	 * @param expList [out] the expressions to optimize
	 * @param boundsList [out] the bounds where to optimize
	 */
	private static void createOptimizationParams(
			List<OptimizationParams> params, ArrayList<Expression> expList,
			ArrayList<HashMap<String, Interval>> boundsList)
	{
		for (OptimizationParams op : params)
		{
			LinkedHashMap<String, ExpressionInterval> toOptimize = op.toOptimize;
			HashMap<String, Interval> bounds = op.bounds;
			
			for (ExpressionInterval ei : toOptimize.values())
			{
				expList.add(ei.asExpression());
				boundsList.add(bounds);
			}
		}
	}
	
	/**
	 * Create an affine approximation based on sampling the given dynamics
	 * @param nonlinear the input dynamics 
	 * @param bounds the bounds where to take the approximation
	 * @return output (linear) dynamics which are close to the nonlinear ones
	 */
	public static LinkedHashMap<String, ExpressionInterval> affineApprox(
			LinkedHashMap<String, ExpressionInterval> nonlinear,
			HashMap<String, Interval> bounds)
	{
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();
		int numVars = nonlinear.size();
		double[][] jac = AutomatonUtil.estimateJacobian(nonlinear, bounds);
		ArrayList <String> orderedVariables = new ArrayList <String>(); // same ordering as the flow hashmap		
		orderedVariables.addAll(nonlinear.keySet());
		
		for (int derVar = 0; derVar < numVars; ++derVar)
		{
			String var = orderedVariables.get(derVar);
			Expression derivativeExp = nonlinear.get(var).asExpression();
			
			// linear estimate is: JAC[derVar][0] * var0 + JAC[derVar][1] * var1 + ...
			Expression linearized = null;
			
			for (int partialVar = 0; partialVar < numVars; ++partialVar)
			{
				if (jac[derVar][partialVar] == 0)
					continue;
				
				Operation term = new Operation(Operator.MULTIPLY, 
						new Constant(jac[derVar][partialVar]),
						new Variable(orderedVariables.get(partialVar)));
				
				if (linearized == null)
					linearized = term;
				else
					linearized = new Operation(Operator.ADD, linearized, term);
			}
			
			// if jacobian was zero for all directions
			if (linearized == null)
				linearized = new Constant(0);
			
			rv.put(var, new ExpressionInterval(linearized));
		}
		
		return rv;
	}
}
