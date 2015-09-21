package com.verivital.hyst.simulation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.simulation.RungeKutta.StepListener;

/**
 * A simple simulator for a flat hybrid automata. Currently this only simulates a single mode using a 
 * fixed-step Runge-Kutta 4th order scheme
 * @author Stanley Bak
 *
 */
public class Simulator
{
	public static double[] simulateFor(double time, double[] init, int numSteps, 
			Map <String, ExpressionInterval> dynamics, List <String> variableNames, StepListener sl)
	{
		HyperPoint hp = new HyperPoint(init.length);
		
		for (int i = 0; i < hp.dims.length; ++i)
			hp.dims[i] = init[i];
		
		double stepTime = time / numSteps;
		RungeKutta.stepsRk(centerDynamics(dynamics), variableNames, hp, stepTime, numSteps, sl);

		return hp.dims;
	}
	
	public static Map<String, Expression> centerDynamics(Map<String, ExpressionInterval> flowDynamics)
	{
		LinkedHashMap<String, Expression> rv = new LinkedHashMap<String, Expression>();
		
		for (Entry<String, ExpressionInterval> e : flowDynamics.entrySet())
		{
			ExpressionInterval ei = e.getValue();
			double mid = 0;
			Interval i = ei.getInterval();
			
			if (i != null)
				mid = i.middle();
			
			Expression eMid = new Operation(Operator.ADD, ei.getExpression(), new Constant(mid)); 
			rv.put(e.getKey(), eMid);
		}
		
		return rv;
	}

	/** Process a reset on a point, and return the new point
	 * 
	 * @param pt the incoming point
	 * @param variableNames the list of variables, in order
	 * @param reset the reset map
	 * @return the outgoing point
	 */
	public static double[] processReset(double[] p, ArrayList <String> variableNames, 
			LinkedHashMap<String, ExpressionInterval> reset)
	{
		double[] rv = new double[p.length];
		
		for (int i = 0; i < variableNames.size(); ++i)
		{
			String v = variableNames.get(i);
			ExpressionInterval resetAssignment = reset.get(v);
			
			if (resetAssignment == null)
				rv[i] = p[i];
			else
				rv[i] = assignFromExpression(v, p, variableNames, resetAssignment);
		}
		
		return rv;
	}

	private static double assignFromExpression(String v, double[] p, ArrayList<String> variableNames, 
			ExpressionInterval resetAssignment)
	{
		HyperPoint hp = new HyperPoint(p);
		
		double d = RungeKutta.evaluateExpression(resetAssignment.getExpression(), hp, variableNames);
		
		Interval i = resetAssignment.getInterval();
		
		if (i != null)
			d += i.middle();
		
		return d;
	}
}
