package com.verivital.hyst.passes.complex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.geometry.SymbolicStatePoint;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.internalpasses.ConvertFromStandardForm;
import com.verivital.hyst.internalpasses.ConvertToStandardForm;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.DoubleArrayOptionHandler;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.StringOperations;

public class ContinuizationPass extends TransformationPass
{
	public ContinuizationPass()
	{
		preconditions.skip[PreconditionsFlag.NO_URGENT.ordinal()] = true;
	}

	private static class DomainValues
	{
		double startTime;
		double endTime;

		double bloat; // bloating term for this domain
		Interval range; // simulated range + bloating term
		AutomatonMode mode;

		public String toString()
		{
			return "[DomainValue mode: " + (mode == null ? "null" : mode.name) + ", times = "
					+ startTime + "-" + endTime + ", range = " + range + "]";
		}
	}

	ArrayList<DomainValues> domains = new ArrayList<DomainValues>();

	// command line parameter values below
	@Option(name = "-var", required = true, usage = "variable name being continuized", metaVar = "NAME")
	private String varName;

	@Option(name = "-timevar", usage = "time variable", metaVar = "NAME")
	private String timeVarName;

	@Option(name = "-period", required = true, usage = "period of controller", metaVar = "TIME")
	private double period;

	@Option(name = "-noerrormodes", usage = "skip creating error modes")
	boolean skipErrorModes = false;

	@Option(name = "-times", required = true, handler = DoubleArrayOptionHandler.class, usage = "time domain boundaries", metaVar = "TIME1 TIME2 ...")
	List<Double> times;

	@Option(name = "-bloats", required = true, handler = DoubleArrayOptionHandler.class, usage = "bloating terms for each time domain", metaVar = "VAL1 VAL2 ...")
	List<Double> bloats;

	public static String makeParamString(String var, String timeVar, double period,
			boolean skipError, List<Double> times, List<Double> bloats)
	{
		StringBuffer rv = new StringBuffer();
		rv.append("-var " + var);

		if (timeVar != null)
			rv.append(" -timevar " + timeVar);

		rv.append(" -period " + period);

		if (skipError == true)
			rv.append(" -noerrormodes");

		rv.append(" -times ");
		rv.append(StringOperations.join(" ", times.toArray(new Double[] {})));

		rv.append(" -bloats ");
		rv.append(StringOperations.join(" ", bloats.toArray(new Double[] {})));

		return rv.toString();
	}

	@Override
	public String getCommandLineFlag()
	{
		return "continuization";
	}

	@Override
	public String getName()
	{
		return "Continuization Pass";
	}

	@Override
	public String getLongHelp()
	{
		String header = "Currently, this pass assumes a one or two-mode automaton, where the first mode is urgent and the\n"
				+ "reset does any initialization. The second mode is the continuous approximation of the system.\n"
				+ "the 'range_adders' are combined with a simulation to determine the ranges of\n"
				+ "the variables. The ranges are initially estimated from a simulation, then this gets increased by\n"
				+ "range_adder. If you reach the error_range_ modes try increasing these values.\n\n"
				+ "If several partitions are used, the time_var_name must be provided, otherwise it can be omitted.";

		return header;
	}

	/**
	 * Is the given automaton a two-mode, one urgent initialization? This also populates the class
	 * variables related to the urgent mode
	 * 
	 * @param c
	 *            the configuration
	 * @return true iff it's a valid urgent initialization for the pass
	 */
	private boolean isUrgentInit(Configuration c)
	{
		boolean rv = false;
		BaseComponent ha = ((BaseComponent) c.root);

		if (ha.modes.size() == 2 && c.init.size() == 1 && ha.transitions.size() == 1)
		{
			AutomatonTransition at = ha.transitions.get(0);
			AutomatonMode init = ha.modes.get(c.init.keySet().iterator().next());
			AutomatonMode other = at.to;

			if (init.urgent == true && at.from == init && init != other)
			{
				rv = true;
			}
		}

		return rv;
	}

	@Override
	protected void checkPreconditons(Configuration c, String name)
	{
		super.checkPreconditons(c, name);
		BaseComponent ha = ((BaseComponent) c.root);

		// make sure it's a continuous approximation (single mode)
		if (!isUrgentInit(c) && (ha.modes.size() != 1 || ha.transitions.size() != 0))
			throw new PreconditionsFailedException("Automaton must be a continuous approximation "
					+ "(single mode or single urgent transition) for " + name + ".");
	}

	@Override
	public void runPass()
	{
		ConvertToStandardForm.run(config);

		BaseComponent ha = (BaseComponent) config.root;
		AutomatonMode approxMode = null; // mode of the continuous approximation

		AutomatonMode initMode = ConvertToStandardForm.getInitMode(ha);
		AutomatonMode errorMode = ConvertToStandardForm.getErrorMode(ha);

		for (AutomatonMode m : ha.modes.values())
		{
			if (m != initMode && m != errorMode)
			{
				approxMode = ha.modes.values().iterator().next();
				break;
			}
		}

		if (approxMode == null)
			throw new AutomatonExportException("Continuous approx mode not found");

		processParams(approxMode);

		// extract derivatives
		LinkedHashMap<String, ExpressionInterval> flows = approxMode.flowDynamics;
		ExpressionInterval ei = flows.get(varName);

		if (ei == null)
			throw new AutomatonExportException(
					"flow not found for variable " + varName + " in mode " + approxMode.name);

		// estimate ranges based on simulation, stored in domains.ranges
		estimateRanges();

		if (timeVarName != null)
			createModesWithTimeConditions(ha, approxMode);
		else
			domains.get(0).mode = approxMode; // timeVarName not provided
												// (single mode only)

		// substitute every occurrence of c_i with c_i + \omega_i
		substituteCyberVariables();

		ConvertFromStandardForm.run(config);

		// add the range conditions to each of the modes
		if (skipErrorModes == false)
			addRangeConditionsToModes(ha);

	}

	/**
	 * substitute every occurrence of c_i with c_i + \omega_i
	 */
	private void substituteCyberVariables()
	{
		String cyberVar = varName;

		for (DomainValues dv : domains)
		{
			AutomatonMode am = dv.mode;

			Interval K = dv.range;
			Interval omega = Interval.mult(K, new Interval(-period, 0));

			substituteCyberVariableInMode(am, cyberVar, omega);
		}
	}

	/**
	 * substitute every occurrence of c_i with c_i + \omega_i
	 * 
	 * @param am
	 *            the mode where to do the substitution
	 * @param cyberVar
	 *            the variable name to substitute
	 * @param omega
	 *            the omega interval to add
	 * @param range
	 *            the ranges encountered for the variables, used when intervals are multiplied by
	 *            variables
	 */
	private void substituteCyberVariableInMode(AutomatonMode am, String cyberVar, Interval omega)
	{
		// substitute in each flow
		for (Entry<String, ExpressionInterval> e : am.flowDynamics.entrySet())
		{
			ExpressionInterval newEi = substituteVariableInDerivative(e.getValue(), cyberVar,
					omega);
			e.setValue(newEi);
		}
	}

	/**
	 * Substitute each cyber variable c_i with c_i + \omega
	 * 
	 * @param ei
	 *            the expression interval we're substituting inside
	 * @param cyberVar
	 *            the variable c_i
	 * @param omega
	 *            the omega value
	 * @return the resultant expression interval
	 */
	public static ExpressionInterval substituteVariableInDerivative(ExpressionInterval ei,
			String cyberVar, Interval omega)
	{
		Operation subValue = new Operation(Operator.ADD, new Variable(cyberVar),
				new IntervalTerm(omega));

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
	 * 
	 * @param e
	 *            the expression which may contain IntervalTerms
	 * @return an ExpressionInterval representation
	 */
	public static ExpressionInterval simplifyExpressionWithIntervals(Expression e)
	{
		ExpressionInterval rv = simplifyExpressionWithIntervalsRec(e);

		if (rv == null)
			throw new AutomatonExportException(
					"Expression simplification resulted in null: " + e.toDefaultString());

		// simplify expression to get rid of 0's
		rv.setExpression(SimplifyExpressionsPass.simplifyExpression(rv.getExpression()));

		return rv;
	}

	public static ExpressionInterval simplifyExpressionWithIntervalsRec(Expression e)
	{
		ExpressionInterval rv = null;

		if (e instanceof IntervalTerm)
		{
			IntervalTerm it = (IntervalTerm) e;
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
					ExpressionInterval childRv = simplifyExpressionWithIntervalsRec(
							o.children.get(i));
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
				double expressionProduct = 1; // every non-interval will get
												// multiplied by this
				Double NOT_ALL_CONSTANTS = Double.NaN; // flag used for when op
														// is mult but
														// non-intervals are not
														// constants

				for (int i = 0; i < o.children.size(); ++i)
				{
					ExpressionInterval childRv = simplifyExpressionWithIntervalsRec(
							o.children.get(i));
					Expression childE = childRv.getExpression();
					accumulatorE.children.add(childRv.getExpression());

					if (childRv.getInterval() != null)
					{
						o.children.set(i, childE);

						if (interval == null)
							interval = childRv.getInterval();
						else
							throw new AutomatonExportException(
									"Couldn't extract interval from substituted expression "
											+ "(multiple intervals in multiplication): " + e);
					}
					else
					{
						if (expressionProduct != NOT_ALL_CONSTANTS && childE instanceof Constant)
							expressionProduct *= ((Constant) childE).getVal();
						else
							expressionProduct = NOT_ALL_CONSTANTS;
					}
				}

				if (interval != null)
				{
					if (expressionProduct == NOT_ALL_CONSTANTS)
						throw new AutomatonExportException(
								"Couldn't extract interval from substituted expression "
										+ "(non-constant multiplied by interval): " + e);
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
					ExpressionInterval childRv = simplifyExpressionWithIntervalsRec(
							o.children.get(i));

					if (childRv.getInterval() != null)
						throw new AutomatonExportException(
								"Couldn't extract interval from substituted expression "
										+ "(unsupported operation '" + o.op.name() + "'): " + e);
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

	private void addRangeConditionsToModes(BaseComponent ha)
	{
		for (DomainValues dv : domains)
		{
			AutomatonMode am = dv.mode;

			// add checks that the candidate derivatives domains are maintained
			ExpressionInterval ei = am.flowDynamics.get(varName);
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
			t1.guard = new Operation(Operator.GREATEREQUAL, maxDerivative,
					new Constant(dv.range.max));

			AutomatonMode errorModeBelow = getErrorMode(ha, "error_" + am.name + "_below");
			AutomatonTransition t2 = ha.createTransition(am, errorModeBelow);
			t2.guard = new Operation(Operator.LESSEQUAL, minDerivative, new Constant(dv.range.min));

			// adjust invariant
			/*
			 * Expression belowTop = new Operation(Operator.LESSEQUAL, maxDerivative, new
			 * Constant(dv.range.max)); Expression aboveBottom = new
			 * Operation(Operator.GREATEREQUAL, minDerivative, new Constant(dv.range.min));
			 * 
			 * am.invariant = Expression.and(am.invariant, Expression.and(belowTop, aboveBottom));
			 * 
			 * // add checks for incoming transitions ArrayList <AutomatonTransition> allTransitions
			 * = new ArrayList <AutomatonTransition>(); allTransitions.addAll(ha.transitions); //
			 * avoid concurrent modifications
			 * 
			 * for (AutomatonTransition at : allTransitions) { if (at.to == am) {
			 * AutomatonTransition t3 = ha.createTransition(at.from, getErrorMode(ha, "error_" +
			 * am.name + "_initially_above")); t3.guard = Expression.and(at.guard.copy(),
			 * t1.guard.copy());
			 * 
			 * AutomatonTransition t4 = ha.createTransition(at.from, getErrorMode(ha, "error_" +
			 * am.name + "_initially_below")); t4.guard = Expression.and(at.guard.copy(),
			 * t2.guard.copy()); } }
			 */
		}
	}

	/**
	 * Create modes for the disjoint time condition. Store in params.modes
	 * 
	 * @param params
	 *            the ParamValues instance
	 * @param mode
	 *            the automaton mode we're continuizing
	 */
	private void createModesWithTimeConditions(BaseComponent ha, AutomatonMode mode)
	{
		// make sure time variable exists
		if (!ha.variables.contains(timeVarName))
			throw new AutomatonExportException(
					"Time variable named '" + timeVarName + "' not found in automaton.");

		// make all the modes
		for (int i = 0; i < domains.size(); ++i)
		{
			DomainValues dv = domains.get(i);
			AutomatonMode newMode = mode;

			if (i != 0)
				newMode = mode.copy(ha, mode.name + "_" + (i + 1)); // copy
																	// raises
																	// error on
																	// duplicate
																	// names

			dv.mode = newMode;
		}

		// update the invariants and transitions for each mode
		for (int i = 0; i < domains.size(); ++i)
		{
			DomainValues dv = domains.get(i);
			double minTime = dv.startTime;
			double maxTime = dv.endTime;
			AutomatonMode am = dv.mode;

			Operation minTimeCond = new Operation(Operator.GREATEREQUAL, new Variable(timeVarName),
					new Constant(minTime));
			Operation maxTimeCond = new Operation(Operator.LESSEQUAL, new Variable(timeVarName),
					new Constant(maxTime + period));

			Operation timeCond = new Operation(Operator.AND, minTimeCond, maxTimeCond);

			// update invariant
			am.invariant = new Operation(Operator.AND, am.invariant, timeCond);

			// add the outgoing time transition
			if (i + 1 < domains.size())
			{
				AutomatonMode nextAm = domains.get(i + 1).mode;

				AutomatonTransition at = ha.createTransition(am, nextAm);
				at.guard = new Operation(Operator.GREATEREQUAL, new Variable(timeVarName),
						new Constant(maxTime));
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
			config.forbidden.put(badModeName, Constant.TRUE);
		}

		return rv;
	}

	/**
	 * Estimate ranges of the cyber variable derivative in each region and store in domains.ranges
	 * 
	 * @param ha
	 *            the two-mode automaton
	 * @param params
	 *            <in/out> the time divisions and where the resultant ranges are stored
	 */
	private void estimateRanges()
	{
		ConvertFromStandardForm.run(config);

		// simulate from initial state
		HyperPoint initPt = AutomatonUtil.getInitialPoint((BaseComponent) config.root, config);
		String initMode = config.init.keySet().iterator().next();
		Hyst.log("Init point from config: '" + initMode + "' with " + initPt);

		List<Interval> simTimes = new ArrayList<Interval>();

		for (DomainValues d : domains)
			simTimes.add(new Interval(d.startTime, d.endTime));

		SymbolicStatePoint start = new SymbolicStatePoint(initMode, initPt);
		List<Interval> ranges = pythonSimulateDerivativeRange(config, varName, start, simTimes);

		if (ranges.size() != domains.size())
			throw new AutomatonExportException(
					"expected single range for each domain from simulation");

		for (int index = 0; index < domains.size(); ++index)
		{
			DomainValues dv = domains.get(index);
			dv.range = ranges.get(index);
		}

		Hyst.log("Ranges from simulation were:");
		logAllRanges();

		// bloat ranges
		for (DomainValues dv : domains)
		{
			dv.range.max += dv.bloat;
			dv.range.min -= dv.bloat;
		}

		Hyst.log("Ranges after bloating were:");
		logAllRanges();

		ConvertToStandardForm.run(config);
	}

	private void logAllRanges()
	{
		for (DomainValues dv : domains)
			Hyst.log("time [" + dv.startTime + ", " + dv.endTime + "]: " + dv.range);
	}

	/**
	 * Parses the pass params
	 * 
	 * @param params
	 *            the pass param string
	 */
	private void processParams(AutomatonMode approxMode)
	{
		// make sure variables exists in automaton
		if (!config.root.variables.contains(varName))
			throw new AutomatonExportException("Varname '" + varName + "' not found in automaton.");

		// for every time domain, you should have a corresponding bloat defined
		if (times.size() != bloats.size())
			throw new AutomatonExportException("Number of bloat values (" + bloats.size()
					+ ") must match number of time domains (" + times.size() + ").");

		if (bloats.size() > 1 && timeVarName == null)
		{
			// look for a time variable in approxMode
			for (Entry<String, ExpressionInterval> e : approxMode.flowDynamics.entrySet())
			{
				if (e.getValue().equalsInterval(new Interval(1, 1)))
				{
					timeVarName = e.getKey();
					break;
				}
			}

			// time var not found, create one
			if (timeVarName == null)
				timeVarName = "_time";
		}

		if (timeVarName != null && !config.root.variables.contains(timeVarName))
		{
			// time variable doesn't exist, add it
			Hyst.log("Adding non-existant time variable: " + timeVarName);
			addTimeVar(timeVarName, approxMode);
		}

		double lastTime = 0;

		for (int i = 0; i < times.size(); ++i)
		{
			DomainValues dv = new DomainValues();
			domains.add(dv);

			dv.startTime = lastTime;
			lastTime = dv.endTime = times.get(i);

			if (dv.startTime >= dv.endTime)
				throw new AutomatonExportException(
						"Time domains must be created along with increasing times.");

			dv.bloat = bloats.get(i);
		}
	}

	/**
	 * Add a time variable to the automaton
	 * 
	 * @param timeVar
	 */
	private void addTimeVar(String timeVar, AutomatonMode approxMode)
	{
		BaseComponent ha = (BaseComponent) config.root;
		ha.variables.add(timeVar);

		AutomatonMode init = ConvertToStandardForm.getInitMode(ha);
		AutomatonMode error = ConvertToStandardForm.getErrorMode(ha);

		approxMode.flowDynamics.put(timeVar, new ExpressionInterval(1));

		if (error != null)
			error.flowDynamics.put(timeVar, new ExpressionInterval(0));

		// initial transition
		for (AutomatonTransition at : ha.transitions)
		{
			if (at.from == init)
				at.guard = Expression.and(at.guard,
						new Operation(Operator.EQUAL, new Variable(timeVar), new Constant(0)));
		}

		config.validate();
	}

	/**
	 * Simulate the automaton, getting the range of the derivative of a variable
	 * 
	 * @param automaton
	 * @param derVarName
	 *            the variable name whose derative we want the range of
	 * @param start
	 *            the start state
	 * @param timeIntervals
	 *            the times where to return the ranges
	 * @return the range of the derivative of derVarName
	 */
	public static ArrayList<Interval> pythonSimulateDerivativeRange(Configuration automaton,
			String derVarName, SymbolicStatePoint start, List<Interval> timeIntervals)
	{
		int numVars = automaton.root.variables.size();

		if (start.hp.dims.length != numVars)
			throw new AutomatonExportException(
					"start point had " + start.hp.dims.length + " dimensions; expected " + numVars);

		int derVarIndex = automaton.root.variables.indexOf(derVarName);

		if (derVarIndex == -1)
			throw new AutomatonExportException(
					"Derivative variable '" + derVarName + "' not found in automaton.");

		PythonBridge pb = PythonBridge.getInstance();
		pb.send("from pythonbridge.pysim_utils import simulate_der_range");

		StringBuilder s = new StringBuilder();
		s.append(PySimPrinter.automatonToString(automaton));

		String point = "[" + StringOperations.join(",", start.hp.dims) + "]";
		ArrayList<String> intervalStrs = new ArrayList<String>();

		for (Interval i : timeIntervals)
			intervalStrs.add("(" + i.min + "," + i.max + ")");

		String timesStr = "[" + StringOperations.join(",", intervalStrs.toArray(new String[0]))
				+ "]";

		s.append("print simulate_der_range(define_ha(), " + derVarIndex + ", '" + start.modeName
				+ "', " + point + ", " + timesStr + ")");

		String result = pb.send(s.toString());

		// result is semi-colon separated hyperrectangles
		// each hyperrectangle is a comma-separated list of size 2*N (N = number
		// of dimensions)

		ArrayList<Interval> rv = new ArrayList<Interval>();

		for (String part : result.split(";"))
		{
			String[] comma_parts = part.split(",");

			if (comma_parts.length != 2)
				throw new AutomatonExportException(
						"Result interval had " + comma_parts.length + " parts, expected 2");

			Interval i = new Interval();

			i.min = Double.parseDouble(comma_parts[0]);
			i.max = Double.parseDouble(comma_parts[1]);

			rv.add(i);
		}

		return rv;
	}

	/**
	 * An interval as part of the expression. This can be temporarily part of an expression when,
	 * for example, we substitute 'c' to 'c + [-1, 1]' in an expression. For example '5 * c + 2' ->
	 * '5 * (c + [-1, 1]) + 2'
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
