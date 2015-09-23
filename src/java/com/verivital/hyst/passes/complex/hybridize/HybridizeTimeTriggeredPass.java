package com.verivital.hyst.passes.complex.hybridize;

import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
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
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.simulation.RungeKutta.StepListener;
import com.verivital.hyst.simulation.Simulator;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.RangeExtractor;

public class HybridizeTimeTriggeredPass extends TransformationPass 
{
	private final static String USAGE = "[time_step, time max, epsilon]";
	double timeStep;
	double timeMax;
	double epsilon;
	String timeVariable;
	int timeVarIndex; // the index of timeVarible in ha.variableName
    private BaseComponent ha;
    private AutomatonMode originalMode;

    AutomatonMode errorMode = null;
    
    @Override
    public String getName()
    {
        return "Time-Tiggered Hybridization Pass";
    }

    @Override
    public String getCommandLineFlag()
    {
        return "-hybridizett";
    }

    @Override
    public String getParamHelp()
    {
        return USAGE;
    }
    
    @Override
    protected void checkPreconditons(Configuration c, String name)
	{
		super.checkPreconditons(c, name);
		ha = (BaseComponent)c.root;
		
		// single mode
		if (((BaseComponent)c.root).modes.size() != 1)
			throw new PreconditionsFailedException(name + " requires a single mode.");
	}
    
    private void parseParams(String params)
	{
		// time step, time max, epsilon
		String[] parts = params.split(",");
		
		if (parts.length != 3)
			throw new AutomatonExportException("Pass expected three params: " + USAGE);
		
		try
		{
			timeStep = Double.parseDouble(parts[0]);
			timeMax = Double.parseDouble(parts[1]);
			epsilon = Double.parseDouble(parts[2]);
		}
		catch (NumberFormatException e)
		{
			throw new AutomatonExportException("Error parsing pass parameter: " + USAGE, e);
		}
	}

    /**
     * Construct the time-triggered modes and transitions
     */
	private void simulateAndConstruct()
	{
		// simulate from initial state
		double[] initPt = AutomatonUtil.getInitialPoint(ha, config);
		Hyst.log("Init point from config: " + Arrays.toString(initPt));

		final double SIM_TIME_STEP = timeStep / 20.0; // 20 steps per simulation time... seems reasonable
		int numSteps = (int)Math.round(timeMax / SIM_TIME_STEP);
		final PythonBridge pb = new PythonBridge();
		pb.open();
		long startMs = System.currentTimeMillis();
		AffineOptimize.numOptimizations = 0;
		
        Simulator.simulateFor(timeMax, initPt, numSteps, originalMode.flowDynamics, ha.variables, new StepListener()
		{
        	private int modeCount = 0;
        	private HyperPoint prevBoxPoint = null;
        	private double prevBoxTime = -1;
        	private AutomatonMode prevMode = null;
        	
			@Override
			public void step(int numStepsCompleted, HyperPoint hp)
			{
				double curTime = numStepsCompleted * SIM_TIME_STEP;
				double prevTime = (numStepsCompleted - 1) * SIM_TIME_STEP;
				
				if (Math.floor(prevTime / timeStep) != Math.floor(curTime / timeStep))
				{
					if (prevBoxPoint != null)
					{
						final String MODE_PREFIX = "_m_";
						AutomatonMode am = ha.createMode(MODE_PREFIX + modeCount++);
						am.flowDynamics = originalMode.flowDynamics;
						am.invariant = originalMode.invariant;
						
				        // bloat bounding box between prevPoint and hp
						HyperRectangle hr = boundingBox(prevBoxPoint, hp);
						hr.bloatAdditive(epsilon);
						
						// set the flows based on the box
						hybridizeFlow(am, hr, pb);
						
						// set invariant based on the box
						setModeInvariant(am, hr);
						
				        // add transitions to error mode on all sides of box
						addModeErrorTransitions(am, hr);
						
						if (prevMode != null)
						{
							// add to previous mode's invariant the time-triggered value
							Expression timeConstraint = new Operation(Operator.LESSEQUAL, 
									new Variable(timeVariable), new Constant(prevBoxTime));
							prevMode.invariant = Expression.and(prevMode.invariant, timeConstraint);
							
							// add transitions at time trigger from previous mode
							Expression timeGuard = new Operation(Operator.EQUAL, 
									new Variable(timeVariable), new Constant(prevBoxTime));
							ha.createTransition(prevMode, am).guard = timeGuard;
							
							// add transitions at the time trigger to the error mode (negation of invariant)
							addErrorTransitionsAtTimeTrigger(prevMode, prevBoxTime, hr);
						}
						
						prevMode = am;
					}
					
					// record the time-triggered point for the construction of the next box
					prevBoxPoint = hp;
					prevBoxTime = curTime;
				}
			}
		});
        
        // report stats
		long difMs = System.currentTimeMillis() - startMs;
		pb.close();
		
		Hyst.log("Completed " + AffineOptimize.numOptimizations + " optimizations in " + difMs + " milliseconds. " +
				(1000.0 * AffineOptimize.numOptimizations / difMs) + " per second.");
	}
	
	/**
	 * Change the (nonlinear) flow in the given mode to a hybridized one with affine dynamics
	 * @param am the mode to change 
	 * @param hr the constraint set, in the order of ha.variablenames
	 */
	private void hybridizeFlow(AutomatonMode am, HyperRectangle hr, PythonBridge pb)
	{
		HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		
		for (int dim = 0; dim < ha.variables.size(); ++dim)
		{
			String name = ha.variables.get(dim);
			bounds.put(name, hr.dims[dim]);
		}
		
		am.flowDynamics = AffineOptimize.createAffineDynamics(pb, am.flowDynamics, bounds);
	}
	
	/**
	 * Add transitions to the error mode on each side of the box for a given mode
	 * @param am the mode
	 * @param hr the invariant box of the mode
	 */ 
	private void addModeErrorTransitions(AutomatonMode am, HyperRectangle hr)
	{
		for (int d = 0; d < hr.dims.length; ++d)
		{
			if (d == timeVarIndex)
				continue;
			
			Interval i = hr.dims[d];
			
			Variable v = new Variable(ha.variables.get(d));
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.min));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.max));
			
			ha.createTransition(am, errorMode).guard = le;
			ha.createTransition(am, errorMode).guard = ge;
		}
	}
	
	/**
	 * Add transitions at the time trigger to the error mode (negation of invariant)
	 * @param am the mode to add to
	 * @param tt the time-trigger time
	 * @param hr the rectangle bounds
	 */
	private void addErrorTransitionsAtTimeTrigger(
			AutomatonMode am, double curTime, HyperRectangle hr)
	{
		for (int d = 0; d < hr.dims.length; ++d)
		{
			if (d == timeVarIndex)
				continue;
			
			Interval i = hr.dims[d];
			
			Variable v = new Variable(ha.variables.get(d));
			Expression atTT = new Operation(Operator.EQUAL, timeVariable, curTime);
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.min));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.max));
			
			ha.createTransition(am, errorMode).guard = Expression.and(atTT, le);
			ha.createTransition(am, errorMode).guard = Expression.and(atTT, ge);
		}
	}

	/**
	 * Set the invariant for a newly-constructed mode
	 * @param am the mode
	 * @param hr the rectangle invariant
	 */
	private void setModeInvariant(AutomatonMode am, HyperRectangle hr)
	{
		for (int d = 0; d < hr.dims.length; ++d)
		{
			if (d == timeVarIndex)
				continue;
			
			Interval i = hr.dims[d];
			Variable v = new Variable(ha.variables.get(d));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.min));
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.max));
			Expression constraint = Expression.and(ge, le);
			
			am.invariant = Expression.and(am.invariant, constraint);
		}
	}

	private static HyperRectangle boundingBox(HyperPoint pt1,	HyperPoint pt2)
	{
		HyperRectangle rv = new HyperRectangle(pt1.dims.length);
		
		for (int d = 0; d < pt2.dims.length; ++d)
		{
			rv.dims[d] = new Interval(pt1.dims[d]);
			rv.dims[d].expand(pt2.dims[d]);
		}
		
		return rv;
	}
	
	/**
	 * Sets the value of timeVariable, adding it if necessary
	 */
	private void extractTimeVariable()
	{
		// look for a variable with derivative 1 and initial state 0
		AutomatonMode am = ha.modes.values().iterator().next();
		Expression init = config.init.values().iterator().next();
		TreeMap<String, Interval> ranges = RangeExtractor.getVariableRanges(init, "initial states");
		
		for (String v : ha.variables)
		{
			ExpressionInterval ei = am.flowDynamics.get(v);
			Expression e = ei.getExpression();
			
			if (ei.getInterval() == null && (e instanceof Constant) && ((Constant)e).getVal() == 1 && ranges.get(v).isExactly(0))
			{
				timeVariable = v;
				break;
			}
		}
		
		// not found, add a new one
		if (timeVariable == null)
		{
			final String TIME_VAR_NAME = "_time_trigger";
			
			timeVariable = TIME_VAR_NAME;
			am.flowDynamics.put(TIME_VAR_NAME, new ExpressionInterval(new Constant(1)));
			ha.variables.add(TIME_VAR_NAME);
			
			Expression newInit = Expression.and(init, new Operation(Operator.EQUAL, timeVariable, 0));
			config.init.put(am.name, newInit);
		}
		
		timeVarIndex = ha.variables.indexOf(timeVariable);
	}

	private void makeErrorMode()
	{
		String name = ha.modes.keySet().iterator().next() + "_error"; 
		errorMode = ha.createMode(name);
		errorMode.invariant = Constant.TRUE;
		
		for (String v : ha.variables)
			errorMode.flowDynamics.put(v, new ExpressionInterval("0"));
	}
	
	private void assignInitialStates()
	{
		Expression init = config.init.values().iterator().next();
		TreeMap<String, Interval> initRanges = RangeExtractor.getVariableRanges(init, "initial states");
		config.init.clear();
		
		final String FIRST_MODE_NAME = "_m_0";
		AutomatonMode am = ha.modes.get(FIRST_MODE_NAME);
		TreeMap<String, Interval> modeRanges = RangeExtractor.getVariableRanges(am.invariant, "mode _m_0 invariant");
		
		for (int i = 0; i < ha.variables.size(); ++i)
		{
			String var = ha.variables.get(i);
			
			if (var.equals(timeVariable))
				continue;
			
			// make sure mode range is contained entirely in initRanges
			Interval modeInterval = modeRanges.get(var);
			Interval initInterval = initRanges.get(var);
			
			if (initInterval.min < modeInterval.min || initInterval.max > modeInterval.max)
			{
				throw new AutomatonExportException("Hybridized first mode's invariant does not entirely contain the " +
						"initial set of states. " + var + " is " + initInterval + " in initial states, and " + modeInterval 
						+ " in the first mode. Consider increasing the bloating term (epsilon).");
			}
		}
		
		config.init.put(FIRST_MODE_NAME, init);
	}

	@Override
    protected void runPass(String params)
    {
        Expression.expressionPrinter = DefaultExpressionPrinter.instance;

        this.ha = (BaseComponent)config.root;
        this.originalMode = ha.modes.values().iterator().next();
        parseParams(params);
        extractTimeVariable(); // sets timeVariable
        makeErrorMode(); // sets errorMode
        ha.validate();

        ha.modes.remove(originalMode.name);
        simulateAndConstruct();
        assignInitialStates();

        config.settings.spaceExConfig.timeTriggered = true;
    }
}