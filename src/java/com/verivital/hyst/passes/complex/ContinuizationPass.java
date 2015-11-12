package com.verivital.hyst.passes.complex;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.base.Interval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.simulation.HyperPoint;
import com.verivital.hyst.simulation.RungeKutta.StepListener;
import com.verivital.hyst.simulation.Simulator;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.PreconditionsFlag;

public class ContinuizationPass extends TransformationPass
{
	static int HELP_WIDTH = 100; // TODO: remove: leave any GUI elements out of the passes
	private static final String PASS_PARAM = "-continuize";
	
	static Options options = new Options(); 
	
	static
	{
		options.addOption(Option.builder("var").argName( "name" ).hasArg()
				.desc("variable name being continuized").required().build());
		
		options.addOption(Option.builder("period").argName( "time" ).hasArg()
				.desc("period of controller").required().build());
		
		options.addOption(Option.builder("timevar").argName( "name" ).hasArg()
				.desc("time variable").build());
		
		options.addOption(Option.builder("times").argName( "time1:time2:..." ).hasArg()
				.desc("time domain boundaries").required().build());
		
		options.addOption(Option.builder("bloats").argName( "val1:val2:..." ).hasArg()
				.desc("bloating values for each time domain").required().build());
		
		options.addOption(Option.builder("noerrorresets")
				.desc("skip creating error-indicating resets").build());
		
		options.addOption(Option.builder("noerrormodes")
				.desc("skip created error modes").build());
	}
	
	public ContinuizationPass()
	{
		preconditions.skip(PreconditionsFlag.NO_URGENT);
	}
	
	private static class DomainValues
	{
		double startTime;
		double endTime;
		
		double bloat; // bloating term for this domain
		Interval range; // simulated range + bloating term
		AutomatonMode mode;
	}
	
	private static class ParamValues
	{
		String varName;
		String timeVarName;
		double period;
		
		boolean skipErrorModes;
		boolean errorIndication;
		
		ArrayList <DomainValues> domains = new ArrayList <DomainValues>();
	}
	
	@Override
	protected void checkPreconditons(Configuration c, String name)
	{
		super.checkPreconditons(c, name);
		
		// check for single-mode automation, with single initial urgent initialization mode
		BaseComponent bc = (BaseComponent)c.root;
		
		if (bc.modes.size() != 2)
			throw new PreconditionsFailedException("Expected two modes.");
		
		if (c.init.size() != 1)
			throw new PreconditionsFailedException("Expected single initial mode.");
		
		String firstModeName = c.init.keySet().iterator().next();
		AutomatonMode firstMode = bc.modes.get(firstModeName);
		
		if (!firstMode.urgent)
			throw new PreconditionsFailedException("First mode should be urgent.");
		
		if (bc.transitions.size() != 1)
			throw new PreconditionsFailedException("Expected single transition.");
		
		AutomatonTransition at = bc.transitions.get(0);
		
		if (at.from != firstMode)
			throw new PreconditionsFailedException("Expected transition to be from initial mode.");
		
		if (at.to == firstMode)
			throw new PreconditionsFailedException("Expected transition to be to noninitial mode.");
		
		if (at.to.urgent)
			throw new PreconditionsFailedException("Noninitial mode shouldn't be urgent.");
		
		if (at.guard != Constant.TRUE)
			throw new PreconditionsFailedException("Transition from initial state shouldn't have a guard.");
	}
	
	@Override
	public String getCommandLineFlag()
	{
		return PASS_PARAM;
	}
	
	@Override
	public String getName()
	{
		return "Continuization Pass";
	}
	
	@Override
	public String getParamHelp()
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		HelpFormatter formatter = new HelpFormatter();
		formatter.printOptions(pw, HELP_WIDTH, options, 0, 0);
		
		return sw.toString();
	}
	
	@Override
	public String getLongHelp()
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		String header = "Currently, this pass assumes a two-mode automaton, where the first mode is urgent and the reset does any" +
				"initialization. The second mode is the continuous approximation of the system." +
				"the 'range_adders' are combined with a simulation to determine the ranges of" +
				"the variables. The ranges are initially estimated from a simulation, then this gets increased by" +
				"range_adder. If you reach the error_range_ modes try increasing these values.\n\n" +
				"If several partitions are used, the time_var_name must be provided, otherwise it can be omitted.";
		 
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(pw, HELP_WIDTH, PASS_PARAM, header, options, 0, 0, null, false);
		
		return sw.toString();
	}
	
	@Override
	public void runPass(String paramStr)
	{
		BaseComponent ha = (BaseComponent)config.root;
		AutomatonTransition trans = ha.transitions.get(0);
		AutomatonMode approxMode = trans.to; // mode of the continuous approximation
		
		ParamValues params = parseParams(paramStr);
		
		// extract derivatives
		LinkedHashMap <String, ExpressionInterval> flows = approxMode.flowDynamics;
		ExpressionInterval ei = flows.get(params.varName);
		
		if (ei == null)
			throw new AutomatonExportException("flow not found for variable " + params.varName + " in mode " + approxMode.name);
		
		// estimate ranges based on simulation, stored in params.ranges
		estimateRanges(ha, params);
		
		if (params.timeVarName != null)
			createModesWithTimeConditions(ha, params, approxMode);
		else
			params.domains.get(0).mode = approxMode; // timeVarName not provided (single mode only)

		// substitute every occurrence of c_i with c_i + \omega_i
		substituteCyberVariables(params);
		
		// add the range conditions to each of the modes
		if (params.skipErrorModes == false)
			addRangeConditionsToModes(ha, params);
	}

	/**
	 * substitute every occurrence of c_i with c_i + \omega_i
	 * @param params 
	 * @param params the mode parameters
	 */
	private void substituteCyberVariables(ParamValues params)
	{
		String cyberVar = params.varName;
		
		for (DomainValues dv : params.domains)
		{
			AutomatonMode am = dv.mode;
			
			Interval K = dv.range;
			Interval omega = Interval.mult(K, new Interval(-params.period, 0));
			
			substituteCyberVariableInMode(am, cyberVar, omega);
		}
	}

	/**
	 * substitute every occurrence of c_i with c_i + \omega_i
	 * @param am the mode where to do the substitution
	 * @param cyberVar the variable name to substitute
	 * @param omega the omega interval to add
	 * @param range the ranges encountered for the variables, used when intervals are multiplied by variables
	 */
	private void substituteCyberVariableInMode(AutomatonMode am, String cyberVar, Interval omega)
	{
		// substitute in each flow
		for (Entry<String, ExpressionInterval> e : am.flowDynamics.entrySet())
		{
			ExpressionInterval newEi = substituteVariableInDerivative(e.getValue(), cyberVar, omega);
			e.setValue(newEi);
		}
	}

	/**
	 * Substitute each cyber variable c_i with c_i + \omega 
	 * @param ei the expression interval we're substituting inside
	 * @param cyberVar the variable c_i
	 * @param omega the omega value
	 * @return the resultant expression interval
	 */
	public static ExpressionInterval substituteVariableInDerivative(ExpressionInterval ei, String cyberVar, 
			Interval omega)
	{
		Operation subValue = new Operation(Operator.ADD, new Variable(cyberVar), new IntervalTerm(omega));
		
		Expression e = AutomatonUtil.substituteVariable(ei.getExpression(), cyberVar, subValue);
		
		ExpressionInterval simplifiedEi = simplifyExpressionWithIntervals(e);
		
		Interval i = ei.getInterval();
		
		if (i != null)
		{
			Interval j = simplifiedEi.getInterval();
			
			if (j == null)
				simplifiedEi.setInterval(i);
			else
				simplifiedEi.setInterval(Interval.add(i, j));
		}

		return simplifiedEi;
	}

	/**
	 * Simplify an expression which may contain IntervalTerms to an ExpressionInterval. 
	 * 
	 * Possible added functionality is to substitute ranges for nonlinear multiplication.
	 * @param e the expression which may contain IntervalTerms
	 * @return an ExpressionInterval representation
	 */
	public static ExpressionInterval simplifyExpressionWithIntervals(Expression e)
	{
		ExpressionInterval rv = simplifyExpressionWithIntervalsRec(e);
		
		if (rv == null)
			throw new AutomatonExportException("Expression simplification resulted in null: " + e.toDefaultString());
		
		// simplify expression to get rid of 0's
		rv.setExpression(SimplifyExpressionsPass.simplifyExpression(rv.getExpression()));
		
		return rv;
	}
	
	public static ExpressionInterval simplifyExpressionWithIntervalsRec(Expression e)
	{
		ExpressionInterval rv = null;
		
		if (e instanceof IntervalTerm)
		{
			IntervalTerm it = (IntervalTerm)e;
			rv = new ExpressionInterval(new Constant(0), it.i);
		}
		else if (e instanceof Operation)
		{
			Operation o = e.asOperation();
			
			switch (o.op)
			{
				case NEGATIVE:
				{
					ExpressionInterval childRv = simplifyExpressionWithIntervalsRec(o.children.get(0));
					Expression childE = childRv.getExpression();
					Interval childI = childRv.getInterval();
					
					if (childI != null)
						childI = Interval.mult(childI, -1);
					
					childE = new Operation(Operator.NEGATIVE, childE);
					
					rv = new ExpressionInterval(childE, childI);
					break;
				}
				case ADD:
				{
					Interval sum = new Interval(0);
					Operation accumulatorE = new Operation(o.op);
					
					for (int i = 0; i < o.children.size(); ++i)
					{
						ExpressionInterval childRv = simplifyExpressionWithIntervalsRec(o.children.get(i));
						accumulatorE.children.add(childRv.getExpression());
						
						if (childRv.getInterval() != null)
							sum = Interval.add(sum, childRv.getInterval());
					}
					
					rv = new ExpressionInterval(accumulatorE, sum);
					break;
				}
				case SUBTRACT:
				{
					Interval sum = new Interval(0);
					Operation accumulatorE = new Operation(o.op);
					
					// first child
					ExpressionInterval childRv1 = simplifyExpressionWithIntervalsRec(o.children.get(0));
					accumulatorE.children.add(childRv1.getExpression());
					
					if (childRv1.getInterval() != null)
						sum = childRv1.getInterval();
					
					// second child
					ExpressionInterval childRv2 = simplifyExpressionWithIntervalsRec(o.children.get(1));
					accumulatorE.children.add(childRv2.getExpression());
					
					if (childRv2.getInterval() != null)
						sum = Interval.add(sum, Interval.mult(childRv2.getInterval(), -1));
					
					rv = new ExpressionInterval(accumulatorE, sum);
					break;
				}
				case MULTIPLY:
				{
					Operation accumulatorE = new Operation(o.op);
					Interval interval = null;
					double expressionProduct = 1; // every non-interval will get multiplied by this
					Double NOT_ALL_CONSTANTS = Double.NaN; // flag used for when op is mult but non-intervals are not constants
					
					for (int i = 0; i < o.children.size(); ++i)
					{
						ExpressionInterval childRv = simplifyExpressionWithIntervalsRec(o.children.get(i));
						Expression childE = childRv.getExpression();
						accumulatorE.children.add(childRv.getExpression());
						
						if (childRv.getInterval() != null)
						{
							o.children.set(i, childE);
							
							if (interval == null)
								interval = childRv.getInterval();
							else
								throw new AutomatonExportException("Couldn't extract interval from substituted expression " +
										"(multiple intervals in multiplication): " + e);
						}
						else
						{
							if (expressionProduct != NOT_ALL_CONSTANTS && childE instanceof Constant)
								expressionProduct *= ((Constant)childE).getVal();
							else
								expressionProduct = NOT_ALL_CONSTANTS;
						}
					}
					
					if (interval != null)
					{
						if (expressionProduct == NOT_ALL_CONSTANTS)
							throw new AutomatonExportException("Couldn't extract interval from substituted expression " +
									"(non-constant multiplied by interval): " + e);
						else
							interval = Interval.mult(interval, expressionProduct);
					}
					
					rv = new ExpressionInterval(accumulatorE, interval);
					break;
				}
				default:
				{
					// unsupported, make sure no kids have intervals
					for (int i = 0; i < o.children.size(); ++i)
					{
						ExpressionInterval childRv = simplifyExpressionWithIntervalsRec(o.children.get(i));
						
						if (childRv.getInterval() != null)
							throw new AutomatonExportException("Couldn't extract interval from substituted expression " +
									"(unsupported operation '" + o.op.name() + "'): " + e);
					}
					
					rv = new ExpressionInterval(e);
					break;
				}
			}
		}
		else 
			rv = new ExpressionInterval(e);
		
		if (rv != null && rv.getInterval() != null && rv.getInterval().isExactly(0))
			rv.setInterval(null);
		
		return rv;
	}

	private void addRangeConditionsToModes(BaseComponent ha, ParamValues params)
	{
		for (DomainValues dv : params.domains)
		{
			AutomatonMode am = dv.mode;
			
			// add checks that the candidate derivatives domains are maintained
			ExpressionInterval ei = am.flowDynamics.get(params.varName);
			Interval i = ei.getInterval();
			Expression e = ei.getExpression();
			Expression maxDerivative;
			Expression minDerivative;
			
			if (i != null) 
			{
				maxDerivative = new Operation(Operator.ADD, e, new Constant(i.max));
				minDerivative = new Operation(Operator.ADD, e, new Constant(i.min));
			}
			else // deterministic cyber-variable derivative
			{
				maxDerivative = e.copy();
				minDerivative = e.copy();
			}
			
			AutomatonMode errorModeAbove = getErrorMode(ha, "error_" + am.name + "_above");
			AutomatonTransition t1 = ha.createTransition(am, errorModeAbove);
			t1.guard = new Operation(Operator.GREATEREQUAL, maxDerivative, new Constant(dv.range.max));
			
			if (params.errorIndication)
				addErrorIndication(t1, true, params);
			
			AutomatonMode errorModeBelow = getErrorMode(ha, "error_" + am.name + "_below");
			AutomatonTransition t2 = ha.createTransition(am, errorModeBelow);
			t2.guard = new Operation(Operator.LESSEQUAL, minDerivative, new Constant(dv.range.min));
			
			if (params.errorIndication)
				addErrorIndication(t2, false, params);
			
			// adjust invariant
			Expression belowTop = new Operation(Operator.LESSEQUAL, maxDerivative, new Constant(dv.range.max));
			Expression aboveBottom = new Operation(Operator.GREATEREQUAL, minDerivative, new Constant(dv.range.min));
			
			am.invariant = Expression.and(am.invariant, Expression.and(belowTop, aboveBottom));
			
			// add checks for incoming transitions
			ArrayList <AutomatonTransition> allTransitions = new ArrayList <AutomatonTransition>();
			allTransitions.addAll(ha.transitions); // avoid concurrent modifications
			
			for (AutomatonTransition at : allTransitions)
			{
				if (at.to == am)
				{
					AutomatonTransition t3 = ha.createTransition(at.from, getErrorMode(ha, "error_" + am.name + "_initially_above"));
					t3.guard = Expression.and(at.guard.copy(), t1.guard.copy());
					
					if (params.errorIndication)
						addErrorIndication(t3, true, params);
					
					AutomatonTransition t4 = ha.createTransition(at.from, getErrorMode(ha, "error_" + am.name + "_initially_below"));
					t4.guard = Expression.and(at.guard.copy(), t2.guard.copy());
					
					if (params.errorIndication)
						addErrorIndication(t4, false, params);
				}
			}
		}
	}

	/**
	 * Set the transition and destination error mode to make it obvious from the plot where the variable when out of bounds. 
	 * This sets the trajectory of every non-time variable to be (from 1 down to 0) or (from -1 up to 0) depending on if we went
	 * over or under.
	 * @param at the transition to the error mode
	 * @param above did it go too high?
	 * @param params the pass parameters
	 */
	private void addErrorIndication(AutomatonTransition at, boolean above, ParamValues params)
	{
		//if (params.timeVarName != null)
		//	at.to.flowDynamics.put(params.timeVarName, new ExpressionInterval(1));
		
		for (String v : at.parent.variables)
		{
			if (!v.equals(params.timeVarName))
			{
				at.reset.put(v, new ExpressionInterval(new Constant(above ? 1 : -1)));
				at.to.flowDynamics.put(v, new ExpressionInterval(new Constant(above ? -10 : 10)));
				at.to.invariant = Expression.and(at.to.invariant, 
						FormulaParser.parseInvariant(v + (above ? ">=" : "<=") + "0"));
			}
			else
				at.to.flowDynamics.put(v, new ExpressionInterval(new Constant(1)));
		}
	}

	/**
	 * Create modes for the disjoint time condition. Store in params.modes
	 * @param params the ParamValues instance
	 * @param mode the automaton mode we're continuizing
	 */
	private void createModesWithTimeConditions(BaseComponent ha, ParamValues params, AutomatonMode mode)
	{
		// make sure time variable exists
		if (!ha.variables.contains(params.timeVarName))
			throw new AutomatonExportException("Time variable named '" + params.timeVarName + "' not found in automaton.");
			
		// make all the modes
		for (int i = 0; i < params.domains.size(); ++i)
		{
			DomainValues dv = params.domains.get(i);
			AutomatonMode newMode = mode;
			
			if (i != 0)
				newMode = mode.copy(ha, mode.name + "_" + (i + 1)); // copy raises error on duplicate names
			
			dv.mode = newMode;
		}
		
		// update the invariants and transitions for each mode
		for (int i = 0; i < params.domains.size(); ++i)
		{
			DomainValues dv = params.domains.get(i);
			double minTime = dv.startTime;
			double maxTime = dv.endTime;
			AutomatonMode am = dv.mode;
			
			Operation minTimeCond = new Operation(Operator.GREATEREQUAL, new Variable(params.timeVarName), new Constant(minTime));
			Operation maxTimeCond = new Operation(Operator.LESSEQUAL, new Variable(params.timeVarName), 
					new Constant(maxTime + params.period));
			
			Operation timeCond = new Operation(Operator.AND, minTimeCond, maxTimeCond);
			
			// update invariant
			am.invariant = new Operation(Operator.AND, am.invariant, timeCond);
			
			// add the outgoing time transition
			if (i + 1 < params.domains.size())
			{
				AutomatonMode nextAm = params.domains.get(i + 1).mode;
				
				AutomatonTransition at = ha.createTransition(am, nextAm);
				at.guard = new Operation(Operator.GREATEREQUAL, new Variable(params.timeVarName), new Constant(maxTime));
			}
		}
	}

	private AutomatonMode getErrorMode(BaseComponent ha, String badModeName)
	{
		AutomatonMode rv = null;
		
		if (ha.modes.containsKey(badModeName))
			rv = ha.modes.get(badModeName);
		else
		{
			rv = ha.createMode(badModeName);
			
			rv.invariant = Constant.TRUE;
			
			for (String v : ha.variables)
				rv.flowDynamics.put(v, new ExpressionInterval(new Constant(0)));
			
			// update forbidden states
			config.forbidden.put(badModeName, null);
		}
		
		return rv;
	}

	/**
	 * Estimate ranges of the cyber variable derivative in each region and store in params
	 * @param ha the two-mode automaton
	 * @param params <in/out> the time divisions and where the resultant ranges are stored
	 */
	private void estimateRanges(final BaseComponent ha, final ParamValues params)
	{
		AutomatonTransition t = ha.transitions.get(0);
		final AutomatonMode approxMode = t.to;
		
		int SIM_STEPS = 1000; // maybe make this a parameter
		
		// minimum range is used to determine step size
		double minRange = Double.MAX_VALUE;
		
		// initialize return data structure and compute minimum range
		for (DomainValues dv : params.domains)
		{
			double range = dv.endTime - dv.startTime;
			
			if (range < minRange)
				minRange = range;
		}
		
		final double stepTime = minRange / SIM_STEPS;
		
		Hyst.log("minimum simulation range is " + minRange + ", simulation step = " + stepTime);
		
		// simulate from initial state
		double[] initPt = AutomatonUtil.getInitialPoint(ha, config);
		Hyst.log("Init point from config: " + Arrays.toString(initPt));
		
		initPt = Simulator.processReset(initPt, ha.variables, t.reset);
		Hyst.log("Init point after urgent transition: " + Arrays.toString(initPt));
		
		double simTime = params.domains.get(params.domains.size() - 1).endTime;
		int numSteps = (int)(Math.ceil(simTime / stepTime));
		
		Simulator.simulateFor(simTime, initPt, numSteps, approxMode.flowDynamics, ha.variables, new StepListener()
		{
			@Override
			public void step(int numStepsCompleted, HyperPoint hp)
			{
				double curTime = numStepsCompleted * stepTime; 
				
				for (DomainValues dv : params.domains)
				{
					if (curTime < dv.startTime || curTime > dv.endTime)
						continue;
					
					ExpressionInterval ei = approxMode.flowDynamics.get(params.varName);
					Interval valInterval = ei.evaluate(hp, ha.variables);
					
					if (dv.range == null)
						dv.range = valInterval;
					else 
						dv.range = Interval.union(dv.range, valInterval);
				}
			}
		});
		
		Hyst.log("Ranges from simulation were:");
		logAllRanges(params);
		
		// bloat ranges
		for (DomainValues dv : params.domains)
		{
			dv.range.max += dv.bloat;
			dv.range.min -= dv.bloat;
		}
		
		Hyst.log("Ranges after bloating were:");
		logAllRanges(params);
	}

	private void logAllRanges(ParamValues params)
	{
		for (DomainValues dv : params.domains)
			Hyst.log("time [" + dv.startTime + ", " + dv.endTime + "]: " + dv.range);
	}

	private ParamValues parseParams(String params)
	{
		ParamValues rv = new ParamValues();
		
		String[] args = AutomatonUtil.extractArgs(params);
		CommandLineParser parser = new DefaultParser();
		
		try
		{
			CommandLine line = parser.parse(options, args);
			
			rv.varName = line.getOptionValue("var");
			rv.period = parsePositiveDouble(line.getOptionValue("period"), "period");
			
			String[] times = line.getOptionValue("times").split(":");
			String[] bloats = line.getOptionValue("bloats").split(":");
			
			String timeVar = line.getOptionValue("timevar"); // may be null
			rv.errorIndication = !line.hasOption("noerrorresets");
			rv.skipErrorModes = line.hasOption("noerrormodes");
		
			if (timeVar == null && times.length > 1)
				throw new ParseException("Multiple times given but timevar was not defined.");
			else
				rv.timeVarName = timeVar;
			
			// make sure variables exists in automaton
			if (!config.root.variables.contains(rv.varName))
				throw new ParseException("Varname '" + rv.varName + "' not found in automaton.");
			
			if (rv.timeVarName != null && !config.root.variables.contains(rv.timeVarName))
				throw new ParseException("Time varname '" + rv.timeVarName + "' not found in automaton.");
			
			// for every time domain, you should have a corresponding bloat defined
			if (times.length != bloats.length)
				throw new ParseException("Number of bloat values (" + bloats.length + ") must match number of time domains (" 
						+ times.length + ").");
			
			double lastTime = 0;
			
			for (int i = 0; i < times.length; ++i)
			{	
				DomainValues dv = new DomainValues();
				rv.domains.add(dv);
				
				dv.startTime = lastTime;
				lastTime = dv.endTime = parsePositiveDouble(times[i], "time domain boundary");
				
				if (dv.startTime >= dv.endTime)
					throw new ParseException("Time domains must be created along with increasing times.");
			
				dv.bloat = parsePositiveDouble(bloats[i], "bloating term for time=[" + dv.startTime + ", " + dv.endTime + "]");
			}
		} 
		catch (ParseException e)
		{
			throw new AutomatonExportException("Error parsing Continuization Pass parameters: " + e.getLocalizedMessage(), e);
		}
		
		return rv;
	}

	private static double parsePositiveDouble(String val, String desc) throws ParseException
	{
		double rv = 0;
		
		try
		{
			rv = Double.parseDouble(val);
		}
		catch (NumberFormatException e)
		{
			throw new ParseException("Error parsing " + desc + " as double: " + val + ". " + e.getLocalizedMessage());
		}
		
		return rv;
	}
	
	/**
	 * An interval as part of the expression. This can be temporarily part of an expression when, for example, we substitute
	 * 'c' to 'c + [-1, 1]' in an expression. For example '5 * c + 2' -> '5 * (c + [-1, 1]) + 2'
	 * 
	 * Eventually this would get simplified to Expression('5 * c + 2') + Interval(-5, 5)
	 */
	public static class IntervalTerm extends Expression
	{
		Interval i;
		
		public IntervalTerm(Interval i)
		{
			this.i = new Interval(i);
		}

		@Override
		public Expression copy()
		{
			return new IntervalTerm(i);
		}
		
		@Override
		public String toString()
		{
			return "[" + i.min + ", " + i.max + "]";
		}
	}
}
