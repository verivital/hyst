package com.verivital.hyst.passes.complex.hybridize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.HyperRectangleCornerEnumerator;
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
import com.verivital.hyst.simulation.RungeKutta;
import com.verivital.hyst.simulation.RungeKutta.StepListener;
import com.verivital.hyst.simulation.Simulator;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;

public class HybridizeTimeTriggeredPass extends TransformationPass 
{
	private final static String USAGE = "step=<val>,maxtime=<val>,epsilon=<val>(,simtype={center|star|corners})" +
									    ",(addforbidden={true|false})";
	
	double timeStep;
	double timeMax;
	double epsilon;
	boolean addForbidden = true;
	SimulationType simType = SimulationType.CENTER;
	
	enum SimulationType
	{
		CENTER, // simulate from the center
		STAR, // simulate from 2*n+1 points (center point as well as limits in each dimension),
		CORNERS // simulate from n^2+1 points (center point as well as corners),
	}
	
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
    	//"step=<val>,maxtime=<val>,epsilon=<val>(,simtype={center|star|corners}),(addforbidden={true|false})";

    	// defaults
		timeStep = -1;
		timeMax = -1;
		epsilon = -1;
		addForbidden = true;
		simType = SimulationType.CENTER;
		
		String[] parts = params.split(",");
		
		for (String part : parts)
		{
			String[] assignment = part.split("=");
			
			if (assignment.length != 2)
				throw new AutomatonExportException("Pass parameter expected single '=' sign: " + part);
	
			String name = assignment[0];
			String value = assignment[1];
			
			try
			{
				if (name.equals("step"))
					timeStep=Double.parseDouble(value);
				else if (name.equals("maxtime"))
					timeMax=Double.parseDouble(value);
				else if (name.equals("epsilon"))
					epsilon=Double.parseDouble(value);
				else if (name.equals("simtype"))
				{
					if (value.equals("center"))
						simType = SimulationType.CENTER;
					else if (value.equals("star"))
						simType = SimulationType.STAR;
					else if (value.equals("corners"))
						simType = SimulationType.CORNERS;
					else
						throw new AutomatonExportException("Unknown simulation type parameter: " + part+ "; usage: " + USAGE);
				}
				else if (name.equals("addforbidden"))
				{
					if (value.equals("true"))
						addForbidden = true;
					else if (value.equals("false"))
						addForbidden = false;
					else
						throw new AutomatonExportException("Unknown addforbidden parameter: " + part + "; usage: " + USAGE);
				}
				else
					throw new AutomatonExportException("Unknown pass parameter: '" + name + "'; usage: " + USAGE);
			}
			catch (NumberFormatException e)
			{
				throw new AutomatonExportException("Error parsing pass parameter: " + USAGE, e);
			}
		}
		
		// errors if not set
		if (timeStep <= 0)
			throw new AutomatonExportException("Positive time must be set as pass parameter: " + USAGE);
		
		if (timeMax <= 0)
			throw new AutomatonExportException("Positive max time must be set as pass parameter: " + USAGE);
		
		if (epsilon < 0)
			throw new AutomatonExportException("Nonnegative epsilon must be set as pass parameter: " + USAGE);
	}
    
    /**
     * Get the initial set of states as a HyperRectangle
     * @return the initial set of states
     * @throws AutomatonExportException if the initial set of states is not a box
     */
    public HyperRectangle getInitialBox()
    {
    	int numDims = ha.variables.size();
    	HyperRectangle rv = new HyperRectangle(numDims);
    	
    	// start in the middle of the initial state set
		TreeMap <String, Interval> ranges = new TreeMap <String, Interval>();
		
		try
		{
			RangeExtractor.getVariableRanges(config.init.values().iterator().next(), ranges);
		} 
		catch (EmptyRangeException e)
		{
			throw new AutomatonExportException("Could not determine ranges for inital values (not rectangluar initial states).", e);
		}
		catch (ConstantMismatchException e)
		{
			throw new AutomatonExportException("Constant mismatch in initial values.", e);
		}
		
		int numVars = ha.variables.size();
		
		for (int i = 0; i < numVars; ++i)
		{
			String var = ha.variables.get(i);
			Interval dimRange = ranges.get(var);
			
			if (dimRange == null)
				throw new AutomatonExportException("Range for '" + var + "' was not set (not rectangluar initial states).");
			else
				rv.dims[i] = dimRange;
		}
		
		return rv;
    }
    
    /**
     * Gets the start of the simulation, depending on the simType parameter
     * @param initBox the initial box of states (from getInitialBox())
     * @return a set of points
     */
    private ArrayList <HyperPoint> getSimulationStart(HyperRectangle initBox)
    {
    	HyperPoint center = initBox.center();
		
		final ArrayList <HyperPoint> rv = new ArrayList <HyperPoint>();
		rv.add(center); // all sim types include center
		
		if (simType == SimulationType.CENTER)
			; // center was already included
		else if (simType == SimulationType.STAR)
		{
			for (int d = 0; d < initBox.dims.length; ++d)
			{
				Interval range = initBox.dims[d];
				HyperPoint left = new HyperPoint(center);
				HyperPoint right = new HyperPoint(center);
				
				left.dims[d] = range.min;
				right.dims[d] = range.max;
				
				rv.add(left);
				rv.add(right);
			}
		}
		else if (simType == SimulationType.CORNERS)
		{
			initBox.enumerateCornersUnique(new HyperRectangleCornerEnumerator()
			{
				@Override
				public void enumerate(HyperPoint p)
				{
					rv.add(p);
				}
			});
		}
		else
			throw new AutomatonExportException("Unimplemented simType: " + simType);
		
		return rv;
    }

    /**
     * Construct the time-triggered modes and transitions
     */
	private void simulateAndConstruct()
	{
		// simulate from initial state
    	HyperRectangle initBox = getInitialBox();
    	HyperPoint centerPoint = initBox.center();
		ArrayList <HyperPoint> simPoints = getSimulationStart(initBox);
				
		Hyst.log("Initial simulation points: " + simPoints);

		double simTimeStep = timeStep / 20.0; // 20 steps per simulation time... seems reasonable
		int numSteps = (int)Math.round(timeMax / simTimeStep);
		
		MyStepListener sl = new MyStepListener(simTimeStep, simPoints, initBox, originalMode);
		
		long simStartMs = System.currentTimeMillis();
		Simulator.simulateFor(timeMax, centerPoint, numSteps, originalMode.flowDynamics, ha.variables, sl);
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
    	private ArrayList <HyperPoint> prevBoxPoints = null;
    	private double prevBoxTime = -1;
    	private AutomatonMode prevMode = null;
    	private double simTimeStep;
    	
    	public ArrayList <AutomatonMode> modes = new ArrayList <AutomatonMode>();
    	public ArrayList <HyperRectangle> rects = new ArrayList <HyperRectangle>();
    	
    	private ArrayList<HyperPoint> simPoints = null;
    	private HyperRectangle initBox = null;
    	private Map <String, Expression> flowDynamics = null; 
    	private List <String> varNames = null;
    	
    	public MyStepListener(double simTimeStep, ArrayList<HyperPoint> simPoints, HyperRectangle initBox, AutomatonMode am)
    	{
    		this.simTimeStep = simTimeStep;
    		this.simPoints = simPoints;
    		this.initBox = initBox;
    		
    		if (isNondeterministicDynamics(am.flowDynamics))
    			throw new AutomatonExportException("Nondeterministic Dynamics are not implemented in simulation.");
    		
    		this.flowDynamics = Simulator.centerDynamics(am.flowDynamics);
    		this.varNames = am.automaton.variables;
    	}

    	private boolean isNondeterministicDynamics(	LinkedHashMap<String, ExpressionInterval> dy)
		{
			boolean rv = false;
			
			for (ExpressionInterval ei : dy.values())
			{
				if (ei.getInterval() != null)
				{
					rv = true;
					break;
				}
			}
			
			return rv;
		}

		/**
    	 * Advance a simulation point
    	 * @param p [inout] the point to advance (in place)
    	 */
    	private void step(HyperPoint p)
    	{
    		RungeKutta.singleStepRk(flowDynamics, varNames, p, simTimeStep);
    	}
    	
		@Override
		public void step(int numStepsCompleted, HyperPoint hp)
		{
			double curTime = numStepsCompleted * simTimeStep;
			double prevTime = (numStepsCompleted - 1) * simTimeStep;
			
			// if it's not the first (initial) call
			if (numStepsCompleted > 0)
			{
				// simulate every point in simPoints by the time step
				for (HyperPoint sp : simPoints)
					step(sp);
			}
			
			if (Math.floor(prevTime / timeStep) != Math.floor(curTime / timeStep))
			{
				// A time-triggered transition should occur here
				
				if (prevBoxPoints != null)
				{
					final String MODE_PREFIX = "_m_";
					AutomatonMode am = ha.createMode(MODE_PREFIX + modeCount++);
					am.flowDynamics = originalMode.flowDynamics;
					am.invariant = originalMode.invariant;
					
			        // bloat bounding box between prevBoxPoints and simPoints
					HyperRectangle hr = boundingBox(prevBoxPoints, simPoints);
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
				prevBoxPoints = deepCopy(simPoints);
				prevBoxTime = curTime;
			}
		}

		private ArrayList<HyperPoint> deepCopy(ArrayList<HyperPoint> pts)
		{
			ArrayList<HyperPoint> rv = new ArrayList<HyperPoint>(pts.size());
			
			for (HyperPoint hp : pts)
			{
				HyperPoint newHp = new HyperPoint(hp);
				rv.add(newHp);
			}
			
			return rv;
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
		if (modes.size() == 0)
			throw new AutomatonExportException("hybridizeFlows was called 0 modes");
		
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

	/**
	 * Get the bounding box of two sets of points
	 * @param setA one of the sets
	 * @param setB the other set
	 * @return a hyperrectngle which tightly includes all the points
	 */
	private static HyperRectangle boundingBox(ArrayList<HyperPoint> setA,	ArrayList<HyperPoint> setB)
	{
		HyperPoint firstPoint = setA.get(0);
		int numDims = firstPoint.dims.length;
		HyperRectangle rv = firstPoint.toHyperRectangle();
		
		for (int d = 0; d < numDims; ++d)
		{
			for (HyperPoint hp : setA)
				rv.dims[d].expand(hp.dims[d]);
			
			for (HyperPoint hp : setB)
				rv.dims[d].expand(hp.dims[d]);
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
		
		if (addForbidden)
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