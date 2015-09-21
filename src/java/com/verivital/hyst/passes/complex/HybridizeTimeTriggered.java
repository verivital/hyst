package com.verivital.hyst.passes.complex;

import java.util.Arrays;

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
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.simulation.RungeKutta.StepListener;
import com.verivital.hyst.simulation.Simulator;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

public class HybridizeTimeTriggered extends TransformationPass 
{
	private final static String USAGE = "[time_step, time max, epsilon]";
	double timeStep;
	double timeMax;
	double epsilon;
	String timeVariable;
	int timeVarIndex; // the index of timeVarible in ha.variableName
    private BaseComponent ha;

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
		AutomatonMode am = ha.modes.values().iterator().next();
        
		// simulate from initial state
		double[] initPt = AutomatonUtil.getInitialPoint(ha, config);
		Hyst.log("Init point from config: " + Arrays.toString(initPt));
		
		final double SIM_TIME_STEP = timeStep / 20.0; // 20 steps per simulation time... seems reasonable
		int numSteps = (int)Math.round(timeMax / SIM_TIME_STEP);
		
        Simulator.simulateFor(timeMax, initPt, numSteps, am.flowDynamics, ha.variables, new StepListener()
		{
        	private int modeCount = 0;
        	private HyperPoint prevPoint = null;
        	private AutomatonMode prevMode = null;
        	
			@Override
			public void step(int numStepsCompleted, HyperPoint hp)
			{
				double curTime = numStepsCompleted * SIM_TIME_STEP;
				double prevTime = (numStepsCompleted - 1) * SIM_TIME_STEP;
				
				if (prevTime / timeStep != curTime / timeStep)
				{
					System.out.println(". creating new mode at time " + curTime);

					if (prevPoint != null)
					{
						final String MODE_PREFIX = "_m_";
						AutomatonMode am = ha.createMode(MODE_PREFIX + modeCount++);
						
				        // bloat bounding box between prevPoint and hp
						HyperRectangle hr = boundingBox(prevPoint, hp);
						hr.bloatAdditive(epsilon);
						
						// set invariant based on the box
						setModeInvariant(am, hr);
						
				        // add transitions to error mode on all sides of box
						addModeErrorTransitions(am, hr);
						
						if (prevMode != null)
						{
							// add to previous mode's invariant the time-triggered value
							
							// add transitions at time trigger from previous mode
						}
						
						prevMode = am;
					}
					
					// record the time-triggered point for the construction of the next box
					prevPoint = hp;
				}
			}
		});
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
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.max));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.min));
			Expression constraint = Expression.and(le, ge);
			
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
		// look for a variable with derivative 1
		AutomatonMode am = ha.modes.values().iterator().next();
		
		for (String v : ha.variables)
		{
			ExpressionInterval ei = am.flowDynamics.get(v);
			Expression e = ei.getExpression();
			
			if (ei.getInterval() == null && (e instanceof Constant) && ((Constant)e).getVal() == 1)
			{
				timeVariable = v;
				break;
			}
		}
		
		// not found, add a new one
		if (timeVariable == null)
		{
			final String TIME_VAR_NAME = "_trigger";
			
			timeVariable = TIME_VAR_NAME;
			am.flowDynamics.put(TIME_VAR_NAME, new ExpressionInterval(new Constant(1)));
			ha.variables.add(TIME_VAR_NAME);
		}
		
		timeVarIndex = ha.variables.indexOf(timeVariable);
	}

	private void makeErrorMode()
	{
		String name = ha.modes.keySet().iterator().next() + "_error"; 
		errorMode = ha.createMode(name);
		errorMode.invariant = Constant.TRUE;
	}

	@Override
    protected void runPass(String params)
    {
        Expression.expressionPrinter = DefaultExpressionPrinter.instance;

        this.ha = (BaseComponent)config.root;
        parseParams(params);
        extractTimeVariable(); // sets timeVariable
        makeErrorMode(); // sets errorMode
        ha.validate();
        
        simulateAndConstruct();

        config.settings.spaceExConfig.timeTriggered = true;
    }
}