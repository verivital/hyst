package com.verivital.hyst.passes.complex.hybridize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.HyperPoint;
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
import com.verivital.hyst.util.KodiakUtil;
import com.verivital.hyst.util.StringOperations;

public class AffineOptimize
{
	public static class OptimizationParams
	{
		// set these two as input (newdynamics, descriptions of modes)
		public LinkedHashMap<String, ExpressionInterval> newDynamics = new LinkedHashMap<String, ExpressionInterval>();
		public ArrayList<OptimizationModeParams> origModes = new ArrayList<OptimizationModeParams>();

		// this is set as output
		public LinkedHashMap<String, ExpressionInterval> result = new LinkedHashMap<String, ExpressionInterval>();

		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("[OptimizationParams\n");
			sb.append("newDynamics = " + newDynamics + "\n");
			sb.append("origModes = " + origModes + "\n");
			sb.append("result = " + result + "\n");
			sb.append("]");

			return sb.toString();
		}
	}

	public static class OptimizationModeParams
	{
		// mode original dynamics and interval bounds for each variable
		public LinkedHashMap<String, ExpressionInterval> origDynamics = new LinkedHashMap<String, ExpressionInterval>();
		public HashMap<String, Interval> bounds = new HashMap<String, Interval>();

		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("[OptimizationModeParams\n");
			sb.append("origDynamics = " + origDynamics + "\n");
			sb.append("bounds = " + bounds + "\n");
			sb.append("]");

			return sb.toString();
		}
	}

	/**
	 * Perform an optimization in order to find out the differences in dynamics, for example,
	 * between a nonlinear derivative and its linear approximation.
	 * 
	 * @param optimizationType
	 *            one of {"basinhopping", "kodiak", "interval", "intervalXXX" where XXX is a real
	 *            number describing the maximum overapproximation error
	 * @param params
	 *            [in/out] the list of OptimizationParams to optimize. Result is stored here
	 */
	public static void optimizeDynamics(String optimizationType, List<OptimizationParams> params)
	{
		if (params.size() == 0)
			throw new AutomatonExportException(
					"createAffineDynamics was called with params list of length 0");

		PythonBridge.getInstance(PythonBridge.NO_TIMEOUT); // turn off timeout

		ArrayList<Expression> expList = new ArrayList<Expression>();
		ArrayList<HashMap<String, Interval>> boundsList = new ArrayList<HashMap<String, Interval>>();

		createOptimizationParams(params, expList, boundsList);

		Hyst.logDebug("Created optimization params; expList="
				+ StringOperations.makeDefaultExpressionListString(expList) + "; boundsList = "
				+ boundsList);

		List<Interval> optimizationResult;

		if (optimizationType.equals("basinhopping"))
			optimizationResult = PythonUtil.scipyOptimize(expList, boundsList);
		else if (optimizationType.equals("kodiak"))
			optimizationResult = KodiakUtil.kodiakOptimize(expList, boundsList);
		else if (optimizationType.equals("interval"))
			optimizationResult = PythonUtil.intervalOptimize(expList, boundsList);
		else if (optimizationType.startsWith("interval"))
		{
			String num = optimizationType.substring("interval".length());

			try
			{
				double accuracy = Double.parseDouble(num);

				if (accuracy <= 0)
					throw new AutomatonExportException(
							"malformed interval optimization param: " + accuracy);

				optimizationResult = PythonUtil.intervalOptimizeBounded(expList, boundsList,
						accuracy);
			}
			catch (NumberFormatException e)
			{
				throw new AutomatonExportException("invalid interval optimization param", e);
			}
		}
		else
			throw new AutomatonExportException(
					"Unsupported Optimization Method: " + optimizationType);

		// output stored in params.result
		createOptimizationResult(params, optimizationResult);
	}

	/**
	 * Populate the result data structures after the optimization was performed
	 * 
	 * @param params
	 *            the param list where the results will be stored
	 * @param result
	 *            the optimization result intervals
	 */
	private static void createOptimizationResult(List<OptimizationParams> params,
			List<Interval> result)
	{
		ArrayList<String> orderedVariables = new ArrayList<String>(); // same
																		// ordering
																		// as
																		// the
																		// flow
																		// hashmap
		orderedVariables.addAll(params.get(0).newDynamics.keySet());

		int resultIndex = 0;

		for (OptimizationParams op : params)
		{
			for (OptimizationModeParams modeParams : op.origModes)
			{
				for (Entry<String, ExpressionInterval> entry : modeParams.origDynamics.entrySet())
				{
					Interval inter = result.get(resultIndex++);
					String var = entry.getKey();
					Expression newDynamics = op.newDynamics.get(var).asExpression();
					ExpressionInterval curEi = op.result.get(var);

					if (curEi == null)
						op.result.put(var, new ExpressionInterval(newDynamics, inter));
					else
						curEi.setInterval(Interval.union(inter, curEi.getInterval()));
				}
			}

			for (String v : orderedVariables)
			{
				double TOL = 1e-9;
				ExpressionInterval ei = op.result.get(v);
				Interval i = ei.getInterval();

				if (i.min > -TOL && i.max < TOL)
				{
					// clear interval
					ei.setInterval(null);
				}
				else if (i.width() < TOL)
				{
					// shift it and clear interval
					double subVal = i.min;

					Expression e = ei.getExpression();
					Expression newE = null;

					if (subVal > 0)
						newE = new Operation(Operator.ADD, e, new Constant(subVal));
					else
						newE = new Operation(Operator.SUBTRACT, e, new Constant(-subVal));

					ei.setExpression(newE);
					ei.setInterval(null);
				}
				else if (!ei.getInterval().contains(0))
				{
					// shift it
					double subVal = i.min;

					i.min -= subVal;
					i.max -= subVal;

					Expression e = ei.getExpression();
					Expression newE = null;

					if (subVal > 0)
						newE = new Operation(Operator.ADD, e, new Constant(subVal));
					else
						newE = new Operation(Operator.SUBTRACT, e, new Constant(-subVal));

					ei.setExpression(newE);
				}
			}

		}
	}

	/**
	 * Create the parameters for optimization
	 * 
	 * @param params
	 *            the passed-in parameters
	 * @param expList
	 *            [out] the expressions to optimize
	 * @param boundsList
	 *            [out] the bounds where to optimize
	 */
	private static void createOptimizationParams(List<OptimizationParams> params,
			ArrayList<Expression> expList, ArrayList<HashMap<String, Interval>> boundsList)
	{
		for (OptimizationParams op : params)
		{
			for (OptimizationModeParams modeParams : op.origModes)
			{
				HashMap<String, Interval> bounds = modeParams.bounds;

				for (Entry<String, ExpressionInterval> entry : modeParams.origDynamics.entrySet())
				{
					String var = entry.getKey();
					Expression oldE = entry.getValue().asExpression();
					Expression newE = op.newDynamics.get(var).asExpression();

					// toOptimize is origDynamics - newDynamics
					Expression toOptimize = new Operation(Operator.SUBTRACT, oldE, newE);

					expList.add(toOptimize);
					boundsList.add(bounds);
				}
			}
		}
	}

	/**
	 * Create an affine approximation based on sampling the given dynamics
	 * 
	 * @param nonlinear
	 *            the input dynamics
	 * @param bounds
	 *            the bounds where to take the approximation
	 * @return output (linear) dynamics which are close to the nonlinear ones
	 */
	public static LinkedHashMap<String, ExpressionInterval> affineApprox(
			LinkedHashMap<String, ExpressionInterval> nonlinear, HashMap<String, Interval> bounds)
	{
		double TOL = 1e-9;
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();
		int numVars = nonlinear.size();
		double[][] jac = AutomatonUtil.estimateJacobian(nonlinear, bounds);
		ArrayList<String> orderedVariables = new ArrayList<String>(); // same
																		// ordering
																		// as
																		// the
																		// flow
																		// hashmap
		orderedVariables.addAll(nonlinear.keySet());

		for (int derVar = 0; derVar < numVars; ++derVar)
		{
			String var = orderedVariables.get(derVar);

			// linear estimate is: JAC[derVar][0] * var0 + JAC[derVar][1] * var1
			// + ...
			Expression nonlinearE = nonlinear.get(var).asExpression();
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

			// the offset constant is computed by computing f_lin(center)
			// and adding a constant to make it equal to f_nonlin(center)
			HyperPoint center = AutomatonUtil.boundsCenter(bounds, orderedVariables);
			double linVal = AutomatonUtil.evaluateExpression(linearized, center, orderedVariables);
			double nonlinVal = AutomatonUtil.evaluateExpression(nonlinearE, center,
					orderedVariables);

			double offset = nonlinVal - linVal;

			if (offset < -TOL)
				linearized = new Operation(Operator.SUBTRACT, linearized, new Constant(-offset));
			else if (offset > TOL)
				linearized = new Operation(Operator.ADD, linearized, new Constant(offset));

			rv.put(var, new ExpressionInterval(linearized));
		}

		return rv;
	}
}
