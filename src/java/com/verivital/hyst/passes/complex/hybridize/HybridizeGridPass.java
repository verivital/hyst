package com.verivital.hyst.passes.complex.hybridize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize.OptimizationParams;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.python.PythonUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.RangeExtractor;


public class HybridizeGridPass extends TransformationPass
{
	/**
	 *
	 * This class takes in a hybrid automaton with one discrete location and converts it to a one with multiple hyperboxes.
	 * @param numVars stores the number of dimensions
	 * @param intervalMinValue[] stores the lower range of the particular dimension
	 * @param intervalMaxValue[] stores the upper range of the particular dimension
	 * @param gridSize[] contains the number of partitions in each dimension
	 * @param sizeOfBox[] contains the width of a hyperbox in each dimension, calculated as
	 *		sizeOfBox[i] = (intervalMaxValue[i] - intervalMinValue[i]) / gridSize[i]
	 * @param optimizationType defines that the system should have piecewise constant or dynamics
	 * @param modeIndexLists stores the names that the new modes should have
	 *
	 *                     Suppose the interval along the x-axis is [0,2] and along the y-axis it's [5,8]
	 *                     and we want two partitions along the x-axis and 3 along the y-axis
	 *                     Then the intervals along the x-axis are [0,1] and [1,2] and along y-axis are [5,6], [6,7] and [7,8]
	 *
	 *                     So if we have to find the mode name for the rectangle given by 0 <= x <= 1 and 6 <= y <= 7, then
	 *                     we need to observe that [0,1] is the 0th interval along the x-axis and [6,7] is the 1st interval
	 *                     along the y-axis. So based on this, this mode gets the name m_0_1 (based on the position of interval
	 *                     along the number line).
	 *
	 *                     Similarly the interval 1 <= x <= 2 and 7 <= y <= 8 gets the name m_1_2
	 *
	 *                     This generalizes to the n-dimensional space.
	 *                     This nomenclature helps in finding out the neighboring hyperboxes and providing transitions to them.
	 * @param variableMaps maps some common named variable(x) to the variables in the automaton
	 * @param ha is the HybridAutomaton
	 *
	 */

	private BaseComponent ha;
	private double[] hybridizationStart;
	private int[] numPartitions;
	private double[] width;

	private enum Optimization
	{
		AFFINE,
		LINEAR
	}
	
	private Optimization optimizationType;	
	private ArrayList<ArrayList<Integer>> modeIndexLists; //  for example, [0,0,0], [0,0,1], [0,1,0], [0,1,1], ...

	// the original dynamics / invariant we're hybridizing
	private LinkedHashMap<String, ExpressionInterval> originalDynamics;
	private Expression originalInvariant;
	
	private boolean printGuards = false;
	
	@Override
	public String getName()
	{
		return "Hybridize Grid Pass";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "-hybridizegrid";
	}
	
	@Override
	protected void runPass()
	{
		String params = "TEMP: CONVERT THIS TO ARGS4J!";
		
		PythonBridge.getInstance().setTimeout(-1);
		
		Expression.expressionPrinter = DefaultExpressionPrinter.instance;
		ha = (BaseComponent)config.root;
		AutomatonMode mode = ha.modes.values().iterator().next();
		originalDynamics = mode.flowDynamics;
		originalInvariant = mode.invariant;

		parseParams(params);
		createHybridAutomaton();
		doOptimization();
	}

	/*@Override
	public String getParamHelp()
	{
		return "[<variable for dim_1>,..,<variable for dim_n>,<min of dim1>,<max of dim1>,..<min of dim_n>,<max of dim_n>,<#partitions in dim1>,..,<#partitions in dim_n>,<For affine -> a/linear -> l>]";
	}*/
	
	public String getLongHelp()
	{
		return "Perform hybridization on a fixed grid.";
	}
	
	@Override
	protected void checkPreconditons(Configuration c, String name)
	{
		super.checkPreconditons(c, name);
		
		// check for single-mode automation, with single initial urgent initialization mode
		BaseComponent bc = (BaseComponent)c.root;
		
		if (bc.modes.size() != 1)
			throw new PreconditionsFailedException("Expected a single mode.");
		
		if (c.init.size() != 1)
			throw new PreconditionsFailedException("Expected a single initial mode.");
		
		if (!PythonBridge.hasPython())
			throw new PreconditionsFailedException("Python (and required libraries) needed to run Hybridize Grid pass.");
	}
	
	private void parseParams(String params)
	{
		// process the pass params
		if(params.trim().length() == 0)
			throw new AutomatonExportException("No parameters specified!");
		
		String[] parts = params.split(",");

		if(parts.length != 1 + 4*ha.variables.size() && parts.length != 2 + 4*ha.variables.size())
			throw new AutomatonExportException("Invalid number of parameters specified");

		// change the order of the variables in the automaton to match the order they were specified
		int numVars = ha.variables.size();
		ArrayList<String> allVars = new ArrayList<String>(ha.variables);
		ha.variables.clear();

		//changing the order of variables
		for(int i = 0; i < numVars; ++i)
		{
			if(!allVars.contains(parts[i]))
				throw new AutomatonExportException(parts[i] + " is not in the original automata");
			
			ha.variables.add(parts[i]);
		}
		
		// parse the hybridization start / end points and the number of partitions
		double[] hybridizationEnd = new double[numVars];
		hybridizationStart = new double[numVars];
		width = new double[numVars];
		numPartitions = new int[numVars];

		try
		{
			for(int i=numVars; i<3*numVars; i+=2)
			{
				hybridizationStart[i/2-1] = Double.parseDouble(parts[i]);
				hybridizationEnd[i/2-1] = Double.parseDouble(parts[i+1]);
			}

			for(int i=3*numVars; i<4*numVars; ++i)
				numPartitions[i-3*numVars] = Integer.parseInt(parts[i]);
		}
		catch(NumberFormatException e)
		{
			throw new AutomatonExportException("Non-numeric values specified for interval bounds", e);
		}

		// get the sizes of each box
		for(int i=0;i<numVars;++i)
			width[i] = (hybridizationEnd[i] - hybridizationStart[i])/numPartitions[i];

		// optimization type
		if(parts[4*ha.variables.size()].equals("a"))
			optimizationType = Optimization.AFFINE;
		else if(parts[4*ha.variables.size()].equals("l"))
			optimizationType = Optimization.LINEAR;
		else
			throw new AutomatonExportException("Invalid optimization argument!");
		
		// also add guards
		if(parts.length == 2 + 4*ha.variables.size())
		{
			if(parts[1 + 4*ha.variables.size()].equals("g"))
				printGuards = true;
			else 
				throw new AutomatonExportException("Guard flag incorrect!");
		}
	}

	/**
	 * Create the hybridized modes and transitions
	 */
	private void createHybridAutomaton()
	{
		createModeIndexLists();
		createModes();
		createTransitions();
		addInitialCondition();
		addForbiddenCondition();
	}

	private void doOptimization()
	{
		if (optimizationType == Optimization.AFFINE)
			affineOptimize();
		else
			linearOptimize();
	}
	
	public void affineOptimize()
	{
		List<OptimizationParams> params = new ArrayList<OptimizationParams>();

		for (ArrayList <Integer> indexList : modeIndexLists)
		{
			HashMap<String, Interval> bounds = getIntervalBounds(indexList);
			OptimizationParams op = new OptimizationParams();
			
			op.bounds = bounds;
			op.original = originalDynamics;
			params.add(op);
		}
		
		long start = System.currentTimeMillis();

		AffineOptimize.createAffineDynamics(params);
		
		long dif = System.currentTimeMillis() - start;
		
		// store result
		for (int i = 0; i < modeIndexLists.size(); ++i)
		{
			ArrayList <Integer> indexList = modeIndexLists.get(i);
			AutomatonMode am = ha.modes.get(indexListToModeName(indexList));
			
			am.flowDynamics = params.get(i).result;
		}
		
		int numOpt = modeIndexLists.size() * (ha.variables.size());
		Hyst.log("Completed " + numOpt + " optimizations in " + dif + " milliseconds. " +
				(1000.0 * numOpt / dif) + " per second");
	}

	private void linearOptimize()
	{
		linearOptimizeScipy();
	}

	private void linearOptimizeScipy()
	{
		int optCount = 0;
		long start = System.currentTimeMillis();
		
		for (ArrayList <Integer> indexList : modeIndexLists)
		{
			AutomatonMode am = ha.modes.get(indexListToModeName(indexList));
			
			HashMap<String, Interval> bounds = getIntervalBounds(indexList);

			for(Entry<String, ExpressionInterval> e : am.flowDynamics.entrySet())
			{
				Interval i = PythonUtil.scipyOptimize(e.getValue().asExpression(), bounds);
				
				e.setValue(new ExpressionInterval(new Constant(0), i));
				optCount++;
			}
		}
		
		long dif = System.currentTimeMillis() - start;
		
		Hyst.log("Completed " + optCount + " optimizations in " + dif + " milliseconds. " +
				(1000.0 * optCount / dif) + " per second");
	}

	/**
	 * Convert an index list to a bounds on the variables
	 * @param indexList
	 * @return
	 */
	private HashMap<String, Interval> getIntervalBounds(ArrayList<Integer> indexList)
	{
		HashMap<String, Interval> rv = new HashMap<String, Interval>();
		
		for (int i = 0; i < ha.variables.size(); ++i)
		{
			String var = ha.variables.get(i);
			
			double min = hybridizationStart[i] + width[i]*indexList.get(i);
			double max = hybridizationStart[i] + width[i]*(1+indexList.get(i));
			
			rv.put(var, new Interval(min, max));
		}
		
		return rv;
	}

	/**
	 *	useful to convert the mode names to string
	 *	mode name starts with _m_
 	 *	and the following characters have been described at
	 *	the start of this class
	 */
	private String indexListToModeName(ArrayList<Integer> v)
	{
		String s = "_m";
		
		for(int i=0;i<v.size();++i)
			s += "_" + v.get(i);
		
		return s;
	}

	/**
	 * create the names of the different modes in the hybridized automaton
	 */
	private void createModeIndexLists()
	{
		modeIndexLists = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> tmp1 = new ArrayList<Integer>();
		
		// add a mode for the first dimension
		for(int i=0;i<numPartitions[0];++i){
			tmp1.add(i);
			modeIndexLists.add(new ArrayList<Integer>(tmp1));
			tmp1.clear();
		}

		// for every remaining dimension, add it to the list of modes
		ArrayList<ArrayList<Integer>> tmp = new ArrayList<ArrayList<Integer>>();
			
		for(int i=1;i<ha.variables.size();++i){
			tmp.clear();
			for(int k=0;k<modeIndexLists.size();++k)
			{
				for(int j=0;j<numPartitions[i];++j)
				{
					ArrayList<Integer> w = new ArrayList<Integer>(modeIndexLists.get(k));
					w.add(j);
					tmp.add(new ArrayList<Integer>(w));
				}
			}
			modeIndexLists = new ArrayList<ArrayList<Integer>>(tmp);
		}
	}

	/**
	 *  creates a mode invariant from an index list
	 */
	private Expression createInvariant(ArrayList<Integer> indexList)
	{
		HashMap<String, Interval> bounds = getIntervalBounds(indexList);

		return boundsToExpression(bounds);
	}

	/**
	 * create modes in the hybrid automaton, as well as their invariants
	 */
	private void createModes()
	{
        ha.modes.clear();

		for(int j=0;j <modeIndexLists.size(); ++j) 
		{
			ArrayList<Integer> indexList = modeIndexLists.get(j);
			
			String name = indexListToModeName(indexList);
			AutomatonMode am = ha.createMode(name);
		
			am.invariant = Expression.and(originalInvariant, createInvariant(indexList));
			am.flowDynamics = new LinkedHashMap<String, ExpressionInterval>(originalDynamics);
		}
	}

	/**
	 *	Add transitions between the different modes.
	 */
	private void createTransitions()
	{
		for(int j=0;j<modeIndexLists.size();++j)
		{
			ArrayList<Integer> indexList = modeIndexLists.get(j);
			HashMap<String, Interval> bounds = getIntervalBounds(indexList);
			String name = indexListToModeName(indexList);
			AutomatonMode am = ha.modes.get(name);
			
			// for every dimension, add transitions to the prev and next neighbor
			for (int dim = 0; dim < indexList.size(); ++dim)
			{
				String variableName = ha.variables.get(dim);
				Interval invariantInterval = bounds.get(variableName);
				int index = indexList.get(dim);
				
				// if there is a previous neighbor
				if (index > 0)
				{
					ArrayList<Integer> prevNeb = new ArrayList<Integer>(indexList);
					prevNeb.set(dim, index - 1);
					
					AutomatonMode prevAm = ha.modes.get(indexListToModeName(prevNeb));
					AutomatonTransition at = ha.createTransition(am, prevAm);
					
					at.guard = new Operation(Operator.LESSEQUAL, new Variable(variableName), new Constant(invariantInterval.min));
					
					// add condition that derivative is less than zero
					if (printGuards)
						at.guard = Expression.and(at.guard, 
								new Operation(Operator.LESSEQUAL, dynamicsAsExpression(dim), new Constant(0)));
				}
				
				// if there is a next neighbor
				if (index < numPartitions[dim] - 1)
				{
					ArrayList<Integer> nextNeb = new ArrayList<Integer>(indexList);
					nextNeb.set(dim, index + 1);
					
					AutomatonMode nextAm = ha.modes.get(indexListToModeName(nextNeb));
					AutomatonTransition at = ha.createTransition(am, nextAm);
					
					at.guard = new Operation(Operator.GREATEREQUAL, new Variable(variableName), new Constant(invariantInterval.max));
					
					// add condition that derivative is greater than zero
					if (printGuards)
						at.guard = Expression.and(at.guard, 
								new Operation(Operator.GREATEREQUAL, dynamicsAsExpression(dim), new Constant(0)));
				}
			}
		}
	}

	/**
	 * Get the dynamics as an expression for a given dimension
	 * @param dim the dimension index
	 * @return
	 */
	private Expression dynamicsAsExpression(int dim)
	{
		String variable = ha.variables.get(dim);
		ExpressionInterval ei = originalDynamics.get(variable);

		return ei.asExpression();
	}

	/**
	 *	copy paste the forbidden conditions to the initial mode
	 *	to the newly created modes
	 */
	private void addForbiddenCondition(){
		ArrayList<Expression> l = new ArrayList<Expression>(config.forbidden.values());
		
		config.forbidden.clear();
		
		for(String s : ha.modes.keySet())
		{
			for(int i=0;i<l.size();++i)
				config.forbidden.put(s, l.get(i));
		}
	}

	/**
	 * Set the initial states
	*/
	private void addInitialCondition()
	{
		Expression init = config.init.values().iterator().next();
		config.init = expressionInvariantsIntersection(ha, init, "initial states");
	}
	
	/**
	 * Get the modes in a hybrid automaton which intersect an expression
	 * @param ha the original autommaton (with modes created and invariants assigned)
	 * @param e the expression
	 * @param desc a description for errors if e is not a rectangle
	 * @return a map from mode_name to expression where the value is the intersection of e an the mode's invariant
	 */
	public static LinkedHashMap<String, Expression> expressionInvariantsIntersection(BaseComponent ha, Expression e, String desc)
	{
		LinkedHashMap<String, Expression> rv = new LinkedHashMap<String, Expression>();
		
		TreeMap<String, Interval> rangesExp = RangeExtractor.getVariableRanges(e, desc); 
		
		for(AutomatonMode am : ha.modes.values())
		{
			TreeMap<String, Interval> rangesMode = RangeExtractor.getVariableRanges(am.invariant, "invariant for mode " + am.name);
			
			// if there is a point in both rangesMode and rangesInit, add as initial state
			TreeMap<String, Interval> intersection = getIntersection(rangesExp, rangesMode);
			
			if (intersection != null)
				rv.put(am.name, boundsToExpression(intersection));
		}
		
		return rv;
	}
	
	/**
	 * Convert from a set of bounds (variable -> interval) to an expression
	 * @param bounds the bounds
	 * @return the generated expression
	 */
	private static Expression boundsToExpression(Map<String, Interval> bounds)
	{
		Expression rv = Constant.TRUE;
		
		for (Entry<String, Interval> e : bounds.entrySet())
		{
			Variable v = new Variable(e.getKey());
			Interval i = e.getValue();
			
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.min));
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.max));
			Expression range = Expression.and(ge, le);

			if (i.isMinOpen())
				rv = Expression.and(rv, le);
			else if (i.isMaxOpen())
				rv = Expression.and(rv, ge);
			else
				rv = Expression.and(rv, range);
		}

		return rv;
	}

	/**
	 * Get the intersection of two sets of constraints
	 * @param a the first range
	 * @param b the second range
	 * @return the intersection
	 */
	private static TreeMap<String, Interval> getIntersection(Map<String, Interval> a, Map<String, Interval> b)
	{
		TreeMap<String, Interval> rv = new TreeMap<String, Interval>();
		
		for (Entry<String, Interval> entryA : a.entrySet())
		{
			String variable = entryA.getKey();
			Interval intervalA = entryA.getValue();
			Interval intervalB = b.get(variable);
			
			if (intervalB == null) // unconstrained in B, any value in intervalA is valid
				rv.put(variable, intervalA);
			else
			{
				Interval i = Interval.intersection(intervalA, intervalB);
				
				if (i == null)
				{
					rv = null;
					break;
				}
				else
					rv.put(variable, i);
			}
		}
		
		return rv;
	}
}