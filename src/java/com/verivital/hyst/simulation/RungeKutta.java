package com.verivital.hyst.simulation;


import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.util.ValueSubstituter;


/**
 * Solver using the 4th order Runge-Kutta method
 * @author Stanley Bak (sbak2@illinois.edu)
 *
 */
public class RungeKutta 
{
	private static Expression[] dynamics = null;
	private static List <String> varNames = null;
	
	private static void assignDynamics(Map <String, Expression> flowDynamics, List <String> varNames)
	{
		assert(flowDynamics.size() == varNames.size());
		
		RungeKutta.varNames = varNames;
		dynamics = new Expression[flowDynamics.size()];
		
		for (int i = 0; i < varNames.size(); ++i)
		{
			String name = varNames.get(i);
			dynamics[i] = flowDynamics.get(name);
		}
	}
	
	public static abstract class StepListener
	{
		public abstract void step(int numStepsCompleted, HyperPoint hp);
	}
	
	public static void stepsRk(Map <String, Expression> flowDynamics, List <String> varNames, HyperPoint hp, double h, 
			int numSteps, StepListener sl)
	{
		assignDynamics(flowDynamics, varNames);
		
		for (int s = 0; s < numSteps; ++s)
		{
			if (sl != null)
				sl.step(s, hp);
				
			stepRK(hp, h);
		}
		
		if (sl != null)
			sl.step(numSteps, hp);
	}
	
	/**
	 * Do some steps of the Runge-Kutta method
	 * @param dy the dynamics to use
	 * @param hp the point where to start, this is modified during the function call to be the result
	 * @param h the time step to use
	 * @param numSteps the number of steps 
	 */
	private static void stepRK(HyperPoint hp, double h)
	{
		HyperPoint k_1 = new HyperPoint(hp.dims.length);
		HyperPoint k_2 = new HyperPoint(hp.dims.length);
		HyperPoint k_3 = new HyperPoint(hp.dims.length);
		HyperPoint k_4 = new HyperPoint(hp.dims.length);
		HyperPoint rv = new HyperPoint(hp.dims.length);
		
		k_1 = multiplyDerivative(h, hp);
		k_2 = multiplyDerivative(h, addVector(hp, divVector(k_1, 2)));
		k_3 = multiplyDerivative(h, addVector(hp, divVector(k_2, 2)));
		k_4 = multiplyDerivative(h, addVector(hp,k_3));
		
		// rv = y + 1/6 * (k_1 + 2*k_2 + 2*k_3 + k_4) 
		rv = addVector(addVector(k_1, multVector(k_2, 2)),addVector(multVector(k_3, 2), k_4));
		rv = divVector(rv, 6);
		rv = addVector(rv, hp);
		
		/*
		System.out.println("Doing Step RK");
		System.out.println("start = " + hp);
		System.out.println("k_1 = " + k_1);
		System.out.println("k_2 = " + k_2);
		System.out.println("k_3 = " + k_3);
		System.out.println("k_4 = " + k_4);
		System.out.println("rv(end) = " + rv);
		*/
		
		// return result in the passed-in point
		
		for (int d = 0; d < hp.dims.length; ++d)
			hp.dims[d] = rv.dims[d];
	}
	
	/**
	 * Divide a vector by a constant
	 * @param vec the vector to divide
	 * @param divisor the constant to divide by
	 * @return the resultant vector
	 */
	private static HyperPoint divVector(HyperPoint vec, double divisor)
	{
		HyperPoint rv = new HyperPoint(vec.dims.length);
		
		for (int d = 0; d < vec.dims.length; ++d)
			rv.dims[d] = vec.dims[d] / divisor;
		
		return rv;
	}
	
	/**
	 * Multiply a vector by a constant
	 * @param vec the vector to multiply
	 * @param m the constant to multiply by
	 * @return the resultant vector
	 */
	private static HyperPoint multVector(HyperPoint vec, double m)
	{
		HyperPoint rv = new HyperPoint(vec.dims.length);
		
		for (int d = 0; d < vec.dims.length; ++d)
			rv.dims[d] = vec.dims[d] * m;
		
		return rv;
	}
	
	/**
	 * Add a vector to a hyperpoint
	 * @param pt the point to sum
	 * @param vec the vector to sum
	 * @return the resultant point
	 */
	private static HyperPoint addVector(HyperPoint pt, HyperPoint vec)
	{
		HyperPoint rv = new HyperPoint(pt.dims.length);
		
		for (int d = 0; d < pt.dims.length; ++d)
			rv.dims[d] = pt.dims[d] + vec.dims[d]; 
		
		return rv;
	}
	
	/**
	 * Multiply a constant times a derivative at a point
	 * @param t the constant to multiplyBy
	 * @param derivativePoint the point where to take the derivative
	 * @return the point representing the resultant vector
	 */
	private static HyperPoint multiplyDerivative(double t, HyperPoint derivativePoint)
	{
		HyperPoint rv = new HyperPoint(derivativePoint.dims.length);
		
		for (int dim = 0; dim < derivativePoint.dims.length; ++dim) 
		{
			double d = getDerivative(dim, derivativePoint);
					
			rv.dims[dim] = t * d;
		}
		
		return rv;
	}

	private static double getDerivative(int dim, HyperPoint pt)
	{
		Expression e = dynamics[dim];
		
		return evaluateExpression(e, pt, varNames);
	}
	
	public static double evaluateExpression(Expression e, HyperPoint pt, List <String> variableNames)
	{
		// create value map
		TreeMap<String, Expression> valMap = new TreeMap<String, Expression>();
		
		for (int i = 0; i < pt.dims.length; ++i)
			valMap.put(variableNames.get(i), new Constant(pt.dims[i]));
		
		ValueSubstituter vs = new ValueSubstituter(valMap);
		
		Expression substituted = vs.substitute(e);
		
		Expression result = SimplifyExpressionsPass.simplifyExpression(substituted);
		
		double rv = 0;
		
		if (result instanceof Constant)
			rv = ((Constant)result).getVal();
		else
			throw new AutomatonExportException("Could not simplify flow expression completely while simulating: " + e);
		
		return rv;
	}
	
	public static double[] getGradientAtPoint(Map <String, Expression> flowDynamics, List <String> varNames, HyperPoint pt)
	{
		assignDynamics(flowDynamics, varNames);
				
		double[] rv = new double[pt.dims.length];
		
		for (int dim = 0; dim < pt.dims.length; ++dim)
		{
			double d = getDerivative(dim, pt);
		
			rv[dim] = d;
		}
		
		return rv;
	}
	
}
