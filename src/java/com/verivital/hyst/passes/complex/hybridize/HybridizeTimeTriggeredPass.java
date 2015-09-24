package com.verivital.hyst.passes.complex.hybridize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize.OptimizationParams;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.simulation.RungeKutta.StepListener;
import com.verivital.hyst.simulation.Simulator;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.RangeExtractor;

public class HybridizeTimeTriggeredPass extends TransformationPass 
{
	private final static String USAGE = "[time_step, time max, epsilon, (noforbidden)]";
	
	double timeStep;
	double timeMax;
	double epsilon;
	boolean noForbidden = false;
	
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
		
		if (parts.length != 3 && parts.length != 4)
			throw new AutomatonExportException("Pass expected three or four params: " + USAGE);
		
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
		
		if (parts.length == 4)
		{
			if (parts[3].equals("noforbidden"))
				noForbidden = true;
			else
				throw new AutomatonExportException("Unknown last param: '" + parts[3] + "'. " + USAGE);
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

		double simTimeStep = timeStep / 20.0; // 20 steps per simulation time... seems reasonable
		int numSteps = (int)Math.round(timeMax / simTimeStep);
		
		MyStepListener sl = new MyStepListener(simTimeStep);
		
		long simStartMs = System.currentTimeMillis();
		Simulator.simulateFor(timeMax, initPt, numSteps, originalMode.flowDynamics, ha.variables, sl);
		long simEndMs = System.currentTimeMillis();
		
		final PythonBridge pb = new PythonBridge();
		pb.open();

		long optStartMs = System.currentTimeMillis();
		
		// hybridize all the flows
		hybridizeFlows(sl.modes, sl.rects, pb);
		
        // report stats
		long simTime = simEndMs - simStartMs;
		long optTime = System.currentTimeMillis() - optStartMs;
		pb.close();
		
		int numOpt = sl.modes.size() * (sl.modes.get(0).flowDynamics.size() - 1);
		
		Hyst.log("Simulation time to construct " + sl.modes.size() + " modes: " + simTime + " milliseconds.");
		
		Hyst.log("Completed " + numOpt + " optimizations in " + optTime + " milliseconds. " +
				(1000.0 * numOpt / optTime) + " per second.");
		
		printAverageBoxWidths(sl);
	}
	
	private void printAverageBoxWidths(MyStepListener sl)
	{
		Hyst.log("Average box widths (after bloating):");
		
		ArrayList <Double> totalWidth = new ArrayList <Double>();
		
		for (int i = 0; i < ha.variables.size(); ++i)
			totalWidth.add(0.0);
		
		for (HyperRectangle hr : sl.rects)
		{
			for (int d = 0; d < hr.dims.length; ++d)
				totalWidth.set(d, totalWidth.get(d) + hr.dims[d].width());
		}
		
		for (int i = 0; i < ha.variables.size(); ++i)
		{
			if (i == timeVarIndex)
				continue;
			
			double avg = totalWidth.get(i) / sl.rects.size();
			Hyst.log(ha.variables.get(i) + ": " + avg);
		}
	}

	private class MyStepListener extends StepListener
	{
    	private int modeCount = 0;
    	private HyperPoint prevBoxPoint = null;
    	private double prevBoxTime = -1;
    	private AutomatonMode prevMode = null;
    	private double simTimeStep;
    	
    	public ArrayList <AutomatonMode> modes = new ArrayList <AutomatonMode>();
    	public ArrayList <HyperRectangle> rects = new ArrayList <HyperRectangle>();
    	
    	public MyStepListener(double simTimeStep)
    	{
    		this.simTimeStep = simTimeStep;
    	}
    	
		@Override
		public void step(int numStepsCompleted, HyperPoint hp)
		{
			double curTime = numStepsCompleted * simTimeStep;
			double prevTime = (numStepsCompleted - 1) * simTimeStep;
			
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
					modes.add(am);
					rects.add(hr);
					
					// set invariant based on the box
					setModeInvariant(am, hr);
					
			        // add transitions to error mode on all sides of box
					addModeErrorTransitions(am, hr);
					
					// add to mode's invariant the time-triggered value
					Expression timeConstraint = new Operation(Operator.LESSEQUAL, timeVariable, curTime);
					am.invariant = Expression.and(am.invariant, timeConstraint);
					
					if (prevMode != null)
					{
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
	}
	
	/**
	 * Change the (nonlinear) flow in each mode to a hybridized one with affine dynamics
	 * @param am the mode to change 
	 * @param hr the constraint set, in the order of ha.variablenames
	 */
	private void hybridizeFlows(ArrayList<AutomatonMode> modes,
			ArrayList<HyperRectangle> rects, PythonBridge pb)
	{
		List<OptimizationParams> params = new ArrayList<OptimizationParams>();
		
		for (int i = 0; i < modes.size(); ++i)
		{
			AutomatonMode am = modes.get(i);
			HyperRectangle hr = rects.get(i);
			
			OptimizationParams op = new OptimizationParams();
			op.original = am.flowDynamics;
			
			for (int dim = 0; dim < ha.variables.size(); ++dim)
			{
				String name = ha.variables.get(dim);
				op.bounds.put(name, hr.dims[dim]);
			}
			
			params.add(op);
		}
		
		 AffineOptimize.createAffineDynamics(pb, params);
		
		 for (int i = 0; i < modes.size(); ++i)
		 {
			AutomatonMode am = modes.get(i);
			OptimizationParams op = params.get(i);
			
			am.flowDynamics = op.result;
		 }
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
		String name = originalMode.name + "_error"; 
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
	
	private void assignForbiddenStates()
	{
		if (config.forbidden.size() > 0)
		{
			Expression e = config.forbidden.values().iterator().next();
			config.forbidden = HybridizeGridPass.expressionInvariantsIntersection(ha, e, "forbidden states");
		}
		
		if (noForbidden == false)
		{
			// add error mode to the forbidden states
			String name = originalMode.name + "_error";
			config.forbidden.put(name, Constant.TRUE);
		}
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
        assignForbiddenStates();

        config.settings.spaceExConfig.timeTriggered = true;
    }
}