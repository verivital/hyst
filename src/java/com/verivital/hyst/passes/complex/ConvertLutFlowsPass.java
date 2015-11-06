package com.verivital.hyst.passes.complex;


import java.util.Arrays;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.HyperRectangleCornerEnumerator;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.LutExpression;
import com.verivital.hyst.grammar.formula.MatrixExpression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.util.PreconditionsFlag;


/**
 * A model transformation pass which re-scales time
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class ConvertLutFlowsPass extends TransformationPass
{
	public ConvertLutFlowsPass()
	{
		// this pass is doing the conversion, if we don't skip we'd have an infinite loop
		preconditions.skip[PreconditionsFlag.CONVERT_BASIC_OPERATORS.ordinal()] = true;
		
		// network automata are supported
		preconditions.skip[PreconditionsFlag.CONVERT_TO_FLAT_AUTOMATON.ordinal()] = true;
		
		// urgent modes are supported
		preconditions.skip[PreconditionsFlag.NO_URGENT.ordinal()] = true;
	}

	@Override
	protected void runPass(String params)
	{
		if (!(config.root instanceof BaseComponent))
			throw new AutomatonExportException("Only BaseComponents are supported until the IR is updated to support" + 
					"checking if a mode is initial (github issue #10).");
		
		convertLuts(config.root);
	}
	
	private void convertLuts(Component c)
	{
		if (c instanceof BaseComponent)
		{
			// base case
			convertLutsInBaseComponent((BaseComponent)c);
		}
		else
		{
			NetworkComponent nc = (NetworkComponent)c;
			
			for (ComponentInstance ci : nc.children.values())
			{
				convertLuts(ci.child);
			}
		}
	}

	private void convertLutsInBaseComponent(BaseComponent ha)
	{
		// The automaton is modified in place, so we separate the process of iterating the modes and finding lut dynamics
		// and the process of conversion (which modifies ha)
		int MAX_CONVERSIONS = 1000;
		int numConversions = 0;
		
		while (true)
		{
			// find a mode which contains a LUT in its dynamics
			AutomatonMode foundMode = null;
			String foundVar = null;
			LutExpression foundLut = null;

			for (AutomatonMode am : ha.modes.values())
			{
				if (am.flowDynamics == null) // urgent
					continue;
				
				for (Entry<String, ExpressionInterval> entry : am.flowDynamics.entrySet())
				{
					String var = entry.getKey();
					Expression exp = entry.getValue().getExpression();
					
					// look for a lut in the flow
					foundLut = getLutSubexpression(exp);
					
					if (foundLut != null)
					{
						foundMode = am;
						foundVar = var;
						break;
					}
				}
				
				if (foundLut != null)
					break;
			}
			
			// no luts in dynamics, break
			if (foundLut == null)
				break;
			
			// convert the found lut
			convertFlowInMode(foundMode, foundVar, foundLut);
			
			if (++numConversions > MAX_CONVERSIONS)
				throw new AutomatonExportException("Reached limit of LUT conversions per hybrid automaton: " + MAX_CONVERSIONS);
		};
	}

	/**
	 * Look for a lut subexpression in a given expression. If it exists, return it. Else return null.
	 * @return the lut subexpression or null
	 */
	private LutExpression getLutSubexpression(Expression e)
	{
		LutExpression rv = null;
		
		if (e instanceof LutExpression)
			rv = (LutExpression)e;
		else if (e instanceof Operation)
		{
			Operation o = e.asOperation();
			
			for (Expression c : o.children)
			{
				rv = getLutSubexpression(c);
				
				if (rv != null)
					break;
			}
		}
		
		return rv;
	}

	/**
	 * Convert a lut expression which is part of a flow. This modifies am's parent base-component.
	 * @param am
	 * @param variable
	 * @param lut
	 */
	public void convertFlowInMode(AutomatonMode am, String variable, LutExpression lut)
	{
		checkNotInitialForbidden(am);
		
		// TODO implement
		throw new AutomatonExportException("unimplemented");
	}
	
	/**
	 * Perform n-linear interpolation. This is a generalization of the tri-linear scheme given in
	 * http://paulbourke.net/miscellaneous/interpolation/
	 * @param variableList the names of the variables, in order
	 * @param table the look-up-table of values
	 * @param indexList the index values for each dimension we want to interpolate, the interpolation is done between
	 *                  the elements i and i + 1
	 * @param rangeList the ranges being interpolated (from the two relevant breakpoints in each dimension) 
	 * @return an expression which is the n-linear interpolation
	 */
	public static Expression nLinearInterpolation(String[] variableList, MatrixExpression table, int[] indexList, Interval[] rangeList)
	{
		int numDims = variableList.length;
		Expression[] vars = new Expression[numDims];
		Expression[] oneMinusVars = new Expression[numDims];
		
		if (numDims != table.getNumDims())
			throw new AutomatonExportException("passed-in variables.length must match dimensions of table values");
		
		if (numDims != indexList.length)
			throw new AutomatonExportException("passed-in variables.length must match size of indexList");
		
		if (numDims != rangeList.length)
			throw new AutomatonExportException("passed-in variables.length must match number of ranges");
		
		for (int d = 0; d < numDims; ++d)
		{
			Variable v = new Variable(variableList[d]);
			Expression minVal = new Constant(rangeList[d].min);
			Constant scale = new Constant(1.0 / rangeList[d].width());			
			
			vars[d] = new Operation(Operator.SUBTRACT, 
					new Operation(Operator.MULTIPLY, scale, v),
					new Operation(Operator.MULTIPLY, minVal, scale));
			oneMinusVars[d] = new Operation(Operator.SUBTRACT, new Constant(1), vars[d].copy());
		}
		
		InterpolateCornerEnumerator interpolateEnumerator = new InterpolateCornerEnumerator(vars, oneMinusVars, table, indexList);
		HyperRectangle hr = new HyperRectangle(numDims);
		
		for (int d = 0; d < numDims ;++d)
			hr.dims[d] = new Interval(0, 1);
		
		hr.enumerateCorners(interpolateEnumerator);
		Expression e = interpolateEnumerator.accumulator;
		
		Hyst.logDebug("nLinearInterpolation result expression for " + Arrays.toString(indexList) + ": " + e.toDefaultString());
		e = SimplifyExpressionsPass.simplifyExpression(e);
		Hyst.logDebug("after simplification: " + e.toDefaultString());
		
		return e;
	}
	
	// used to accumulate the expression when performing the interpolation
	private static class InterpolateCornerEnumerator extends HyperRectangleCornerEnumerator
	{
		public Expression accumulator = null;
		
		private Expression[] vars;
		private Expression[] oneMinusVars;
		private MatrixExpression table;
		private int[] indexList;
		
		public InterpolateCornerEnumerator(Expression[] vars, Expression[] oneMinusVars, 
				MatrixExpression table, int[] indexList)
		{
			this.vars = vars;
			this.oneMinusVars = oneMinusVars;
			this.table = table;
			this.indexList = indexList;
		}
		
		@Override
		public void enumerateWithCoord(HyperPoint p, boolean[] isMin)
		{
			int[] index = new int[isMin.length];
			
			for (int i = 0; i < indexList.length; ++i)
				index[i] = isMin[i] ? indexList[i] : indexList[i] + 1;
			
			Expression term = table.get(index);
			
			for (int d = 0; d < vars.length; ++d)
			{
				Expression e = null;
				
				if (isMin[d])
					e = oneMinusVars[d];
				else
					e = vars[d];
					
				term = new Operation(Operator.MULTIPLY, term, e);
			}
			
			// add to accumulator
			if (accumulator == null)
				accumulator = term;
			else
				accumulator = new Operation(Operator.ADD, term, accumulator);
		}
	};

	private void checkNotInitialForbidden(AutomatonMode am)
	{
		String realModeName = am.name;

		// TODO, update this to work for network components
		
		if (config.init.containsKey(realModeName))
			throw new AutomatonExportException("Initial mode '" + realModeName + "' contains LUT, which is not supported.");
		
		if (config.forbidden.containsKey(realModeName))
			throw new AutomatonExportException("Forbidden mode '" + realModeName + "' contains LUT, which is not supported.");
	}
}
